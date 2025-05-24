package com.etw4s.twitchchatlink.emote;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.event.TwitchChatEvent.TwitchChatListener;
import com.etw4s.twitchchatlink.model.AnimatedEmoji;
import com.etw4s.twitchchatlink.model.BaseEmoji;
import com.etw4s.twitchchatlink.model.ChatFragment;
import com.etw4s.twitchchatlink.model.StaticEmoji;
import com.etw4s.twitchchatlink.model.TwitchChat;
import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
import com.etw4s.twitchchatlink.twitch.api.TwitchApi;
import com.etw4s.twitchchatlink.twitch.result.GetEmoteSetResult.Status;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartWorldTick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmoteManager implements TwitchChatListener, StartWorldTick {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);
  private static final EmoteManager instance = new EmoteManager();
  // Emoji.Name -> BaseEmoji
  private final Map<String, BaseEmoji> emojis = Collections.synchronizedMap(new HashMap<>());
  // Unicode -> Emoji.Name
  private final Map<String, String> unicodeMap = Collections.synchronizedMap(new HashMap<>());
  private long last = 0;
  private static final ExecutorService executor = Executors.newFixedThreadPool(2);
  private final UnicodeProvider unicodeProvider = new UnicodeProvider();

  public static EmoteManager getInstance() {
    return instance;
  }

  public boolean isLoaded(String name) {
    return emojis.get(name) != null;
  }

  public BaseEmoji getEmojiByUnicode(String unicode) {
    String name = getNameByUnicode(unicode);
    if (name == null) {
      return null;
    }
    return emojis.get(name);
  }

  public void applyUsingUnicode(Set<String> unicode) {
    LOGGER.debug("{} Unicode are used", unicode.size());
    var unusedUnicodes = unicodeMap.entrySet().stream()
        .filter(entry -> !unicode.contains(entry.getKey())).collect(Collectors.toSet());
    LOGGER.debug("{} Unicode are not used", unusedUnicodes.size());
    unusedUnicodes.forEach(unused -> {
      var emoji = emojis.get(unused.getValue());
      if (emoji == null) {
        return;
      }
      for (Identifier identifier : emoji.getAllIdentifiers()) {
        MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
      }
      unicodeMap.remove(unused.getKey());
      emojis.remove(emoji.getName());
    });
  }

  public String getNameByUnicode(String unicode) {
    return unicodeMap.get(unicode);
  }

  public boolean IsUsedUnicode(String unicode) {
    return unicodeMap.get(unicode) != null;
  }

  @Override
  public void onReceive(TwitchChat chat) {
    var emotes = chat.getEmotes();
    emotes.stream()
        .map(ChatFragment::getEmoteSetId)
        .collect(Collectors.toSet())
        .forEach(emoteSetId -> {
          var result = TwitchApi.getEmoteSet(emoteSetId);
          if (result.getStatus() == Status.Success) {
            result.getEmoteInfos().stream()
                .filter(info -> emotes.stream().anyMatch(e -> info.id().equals(e.getEmoteId())))
                .forEach(info -> executor.submit(new EmoteLoader(info)));
          }
        });
  }

  public synchronized String getOrMappingUnicode(String name) {
    var unicodeOptional = unicodeMap.entrySet().stream()
        .filter(entry -> entry.getValue().equals(name)).findFirst();

    if (unicodeOptional.isPresent()) {
      return unicodeOptional.get().getKey();
    }

    var unicode = unicodeProvider.getNextUnicode();
    unicodeMap.put(unicode, name);
    return unicode;
  }

  static class EmoteLoader implements Runnable {

    private final TwitchEmoteInfo info;
    private final EmoteManager manager = EmoteManager.getInstance();

    EmoteLoader(TwitchEmoteInfo info) {
      this.info = info;
    }

    @Override
    public void run() {
      if (manager.isLoaded(info.id())) {
        return;
      }
      BaseEmoji emoji;
      if (info.isAnimated()) {
        emoji = loadAnimatedEmote(info);
      } else {
        emoji = loadStaticEmote(info);
      }

      if (emoji == null) {
        LOGGER.info("Cant load emoji id: {}, name: {}", info.id(), info.name());
        return;
      }

      manager.emojis.put(info.name(), emoji);
    }

    private StaticEmoji loadStaticEmote(TwitchEmoteInfo info) {
      try {
        URL url = info.getUrl("static", null, null);
        try (InputStream input = url.openStream()) {
          NativeImageBackedTexture texture = new NativeImageBackedTexture(NativeImage.read(input));
          StaticEmoji emote = new StaticEmoji(info.id(), info.name());
          MinecraftClient.getInstance().getTextureManager()
              .registerTexture(emote.getIdentifier(), texture);
          return emote;
        } catch (IOException e) {
          e.printStackTrace();
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
      return null;
    }

    private AnimatedEmoji loadAnimatedEmote(TwitchEmoteInfo info) {
      try {
        URL url = info.getUrl("animated", null, null);
        try (ImageInputStream input = ImageIO.createImageInputStream(url.openStream())) {
          Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
          if (!readers.hasNext()) {
            throw new IOException("GIF format not supported");
          }
          ImageReader reader = readers.next();
          reader.setInput(input);
          int totalFrames = reader.getNumImages(true);
          AnimatedEmoji emote = new AnimatedEmoji(info.id(), info.name(), totalFrames);
          int totalDelay = 0;
          BufferedImage previousFrame = null;
          BufferedImage canvas = null;
          for (int i = 0; i < totalFrames; i++) {
            BufferedImage currentFrame = reader.read(i);
            if (canvas == null) {
              canvas = new BufferedImage(currentFrame.getWidth(), currentFrame.getHeight(),
                  BufferedImage.TYPE_INT_ARGB);
            }
            IIOMetadata metadata = reader.getImageMetadata(i);
            var disposalMethod = getDisposalMethod(metadata);
            LOGGER.info("frame {}: {}x{} {}", i, currentFrame.getWidth(), currentFrame.getHeight(),
                disposalMethod.name());
            int delay = getFrameDelay(metadata);
            int[] pos = getFramePosition(metadata);
            totalDelay += delay;

            var g = canvas.createGraphics();
            g.drawImage(currentFrame, pos[0], pos[1], null);

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
              ImageIO.write(canvas, "png", output);
              NativeImage image = NativeImage.read(output.toByteArray());
              NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
              MinecraftClient.getInstance().getTextureManager()
                  .registerTexture(emote.getFrameIdentifier(i), texture);
            }
            switch (disposalMethod) {
              case DoNotDispose -> {
              }
              case Nothing, RestoreToBackground -> {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                g.setComposite(AlphaComposite.SrcOver);
              }
              case RestoreToPrevious -> {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                g.setComposite(AlphaComposite.SrcOver);
                if (previousFrame != null) {
                  g.drawImage(previousFrame, 0, 0, null);
                }
              }
            }
            previousFrame = new BufferedImage(canvas.getWidth(), canvas.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
            var pg = previousFrame.getGraphics();
            pg.drawImage(canvas, 0, 0, null);
            pg.dispose();
            g.dispose();
          }
          emote.setTotalDelay(totalDelay);
          return emote;
        } catch (IOException e) {
          e.printStackTrace();
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }

      return null;
    }

    private static int getFrameDelay(IIOMetadata metadata) {
      String formatName = "javax_imageio_gif_image_1.0";
      if (metadata.isStandardMetadataFormatSupported()) {
        try {
          String[] metadataNames = metadata.getMetadataFormatNames();
          for (String name : metadataNames) {
            if (name.equals(formatName)) {
              var tree = metadata.getAsTree(formatName);
              return parseDelay(tree);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return 0;
    }

    private static int parseDelay(org.w3c.dom.Node root) {
      var child = root.getFirstChild();
      while (child != null) {
        if ("GraphicControlExtension".equals(child.getNodeName())) {
          var attributes = child.getAttributes();
          var delay = attributes.getNamedItem("delayTime");
          if (delay != null) {
            return Integer.parseInt(delay.getNodeValue()) * 10; // GIFは100分の1秒単位
          }
        }
        child = child.getNextSibling();
      }
      return 0;
    }

    private static DisposableMethod getDisposalMethod(IIOMetadata metadata) {
      String nativeFormat = metadata.getNativeMetadataFormatName();
      javax.imageio.metadata.IIOMetadataNode root = (javax.imageio.metadata.IIOMetadataNode) metadata.getAsTree(
          nativeFormat);

      // "GraphicControlExtension" ノードを探す
      javax.imageio.metadata.IIOMetadataNode graphicControlExtension = (javax.imageio.metadata.IIOMetadataNode) root
          .getElementsByTagName(
              "GraphicControlExtension")
          .item(0);

      if (graphicControlExtension == null) {
        return DisposableMethod.Nothing;
      }

      // "disposalMethod" 属性を取得
      String disposalMethod = graphicControlExtension.getAttribute("disposalMethod");
      switch (disposalMethod) {
        case "doNotDispose":
          return DisposableMethod.DoNotDispose;
        case "restoreToBackgroundColor":
          return DisposableMethod.RestoreToBackground;
        case "restoreToPrevious":
          return DisposableMethod.RestoreToPrevious;
        case "none":
        default:
          return DisposableMethod.Nothing;
      }
    }

    private enum DisposableMethod {
      DoNotDispose, RestoreToBackground, RestoreToPrevious, Nothing,
    }

    private static int[] getFramePosition(IIOMetadata metadata) {
      String nativeFormat = metadata.getNativeMetadataFormatName();
      javax.imageio.metadata.IIOMetadataNode root = (javax.imageio.metadata.IIOMetadataNode) metadata.getAsTree(
          nativeFormat);

      // "ImageDescriptor" ノードを探す
      javax.imageio.metadata.IIOMetadataNode imageDescriptor = (javax.imageio.metadata.IIOMetadataNode) root
          .getElementsByTagName("ImageDescriptor")
          .item(0);

      if (imageDescriptor == null) {
        return new int[] { 0, 0 };
      }

      // 座標情報を取得
      int leftPosition = Integer.parseInt(imageDescriptor.getAttribute("imageLeftPosition"));
      int topPosition = Integer.parseInt(imageDescriptor.getAttribute("imageTopPosition"));

      return new int[] { leftPosition, topPosition };
    }
  }

  @Override
  public void onStartTick(ClientWorld clientWorld) {
    var now = System.currentTimeMillis();
    if (last == 0) {
      last = now;
    }
    emojis.values().forEach(e -> e.animate(now - last));
    last = now;
  }
}
