package com.etw4s.twitchchatlink.client;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.event.TwitchChatEvent.TwitchChatListener;
import com.etw4s.twitchchatlink.model.ChatFragment;
import com.etw4s.twitchchatlink.model.ChatFragment.ChatFragmentType;
import com.etw4s.twitchchatlink.model.TwitchChat;
import com.etw4s.twitchchatlink.model.TwitchEmote;
import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
import com.etw4s.twitchchatlink.model.TwitchStaticEmote;
import com.etw4s.twitchchatlink.twitch.GetEmoteSetResult.Status;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmoteManager implements TwitchChatListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);
  private static final EmoteManager instance = new EmoteManager();
  private final Set<String> loadedEmote = Collections.synchronizedSet(new HashSet<>());
  private final Map<String, TwitchEmote> emotes = Collections.synchronizedMap(new HashMap<>());

  public static EmoteManager getInstance() {
    return instance;
  }

  public boolean isLoaded(String name) {
    return loadedEmote.contains(name);
  }

  public TwitchEmote getEmote(String name) {
    return emotes.get(name);
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
        if (input == null) return;
        NativeImage image = NativeImage.read(input);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        TwitchStaticEmote emote = new TwitchStaticEmote(info.id(), info.name());
        MinecraftClient.getInstance().getTextureManager().registerTexture(emote.getIdentifier(), texture);
        loadedEmote.add(info.name());
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
      URL url = URI.create(info.getUrl("static", null, null)).toURL();
      try (InputStream input = url.openStream()) {
        if (input == null) return;
        NativeImage image = NativeImage.read(input);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        TwitchStaticEmote emote = new TwitchStaticEmote(info.id(), info.name());
        MinecraftClient.getInstance().getTextureManager().registerTexture(emote.getIdentifier(), texture);
        loadedEmote.add(info.name());
        emotes.put(emote.getName(), emote);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    loadedEmote.add(info.name());
  }
}
