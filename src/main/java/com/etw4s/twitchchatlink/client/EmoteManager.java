package com.etw4s.twitchchatlink.client;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.event.TwitchChatEvent.TwitchChatListener;
import com.etw4s.twitchchatlink.model.AnimatedEmoji;
import com.etw4s.twitchchatlink.model.ChatFragment;
import com.etw4s.twitchchatlink.model.ChatFragment.ChatFragmentType;
import com.etw4s.twitchchatlink.model.TwitchChat;
import com.etw4s.twitchchatlink.model.BaseEmoji;
import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
import com.etw4s.twitchchatlink.model.StaticEmoji;
import com.etw4s.twitchchatlink.twitch.GetEmoteSetResult.Status;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmoteManager implements TwitchChatListener, StartWorldTick {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);
  private static final EmoteManager instance = new EmoteManager();
  private static final int MIN_UNICODE = 0xE000;
  private static final int MAX_UNICODE = 0xF8FF;
  private int offset = 9;
  private final Map<String, String> unicodeMap = Collections.synchronizedMap(new HashMap<>());
  private final Map<String, BaseEmoji> emotes = Collections.synchronizedMap(new HashMap<>());
  private long last = 0;

  public static EmoteManager getInstance() {
    return instance;
  }

  public boolean isLoaded(String name) {
    return emotes.containsKey(name);
  }

  public String getNameByUnicode(String unicode) {
    return unicodeMap.get(unicode);
  }

  public BaseEmoji getEmoteByUnicode(String unicode) {
    var name = unicodeMap.get(unicode);
    if (name == null) {
      return null;
    }
    return emotes.get(name);
  }

  public BaseEmoji getEmote(String name) {
    return emotes.get(name);
  }

  private String getNextUnicode() {
    String unicode = new String(Character.toChars(MIN_UNICODE+offset++));
    if (MIN_UNICODE + offset > MAX_UNICODE) {
      offset = 0;
    }
    return unicode;
  }

  public synchronized String getOrMappingUnicode(String name) {
    if (unicodeMap.containsValue(name)) {
      var entry = unicodeMap.entrySet().stream().filter(e -> e.getValue().equals(name)).findFirst();
      if (entry.isPresent()) {
        return entry.get().getKey();
      }
    }
    var unicode = getNextUnicode();
    unicodeMap.put(unicode, name);
    return unicode;
  }

  @Override
  public void onReceive(TwitchChat chat) {
    var emotes = chat.getFragments().stream()
        .filter(f -> f.getType() == ChatFragmentType.Emote)
        .collect(Collectors.toSet());
    var emoteSetIds = emotes.stream().map(ChatFragment::getEmoteSetId).collect(Collectors.toSet());

    emoteSetIds.forEach(emoteSetId ->
        TwitchApi.getEmoteSet(emoteSetId)
            .thenAccept(result -> {
              if (result.getStatus() == Status.Success) {
                result.getEmoteInfos().stream()
                    .filter(info -> emotes.stream().anyMatch(e -> info.id().equals(e.getEmoteId())))
                    .forEach(this::loadEmote);
              }
            }));
  }

  private void loadEmote(TwitchEmoteInfo info) {
    if (isLoaded(info.name())) return;
    if (Arrays.asList(info.format()).contains("animated")) {
      loadAnimatedEmote(info);
    } else {
      loadStaticEmote(info);
    }
  }

  private void loadStaticEmote(TwitchEmoteInfo info) {
    try {
      URL url = URI.create(info.getUrl("static", null, null)).toURL();
      try (InputStream input = url.openStream()) {
        if (input == null) {
          return;
        }
        NativeImage image = NativeImage.read(input);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        StaticEmoji emote = new StaticEmoji(info.id(), info.name());
        MinecraftClient.getInstance().getTextureManager()
            .registerTexture(emote.getIdentifier(), texture);
        emotes.put(emote.getName(), emote);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  private void loadAnimatedEmote(TwitchEmoteInfo info) {
    try {
      URL url = URI.create(info.getUrl("animated", null, null)).toURL();
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
        for (int i = 0; i < totalFrames; i++) {
          BufferedImage frame = reader.read(i);
          IIOMetadata metadata = reader.getImageMetadata(i);
          int delay = getFrameDelay(metadata);
          totalDelay += delay;
          try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(frame, "png", output);
            NativeImage image = NativeImage.read(output.toByteArray());
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            MinecraftClient.getInstance().getTextureManager()
                .registerTexture(emote.getFrameIdentifier(i), texture);
          }
        }
        emote.setTotalDelay(totalDelay);
        emotes.put(emote.getName(), emote);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
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

  @Override
  public void onStartTick(ClientWorld clientWorld) {
    var now = System.currentTimeMillis();
    if (last == 0) last = now;
    emotes.values().forEach(e -> e.animate(now - last));
    last = now;
  }
}
