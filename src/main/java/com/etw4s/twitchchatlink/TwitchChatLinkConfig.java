package com.etw4s.twitchchatlink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class TwitchChatLinkConfig {

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Path path = FabricLoader.getInstance().getConfigDir()
      .resolve("twitch-chat-link.json");

  private String token;
  private String userId;

  private TwitchChatLinkConfig() {
  }

  public static TwitchChatLinkConfig load() {
    if (Files.exists(path)) {
      try {
        String json = Files.readString(path);
        return gson.fromJson(json, TwitchChatLinkConfig.class);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return new TwitchChatLinkConfig();
  }

  public void save() {
    try {
      String json = gson.toJson(this);
      Files.createDirectories(path.getParent());
      Files.writeString(path, json);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public String getUserId() {
    return userId;
  }
}
