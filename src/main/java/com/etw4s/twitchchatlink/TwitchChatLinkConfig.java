package com.etw4s.twitchchatlink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public class TwitchChatLinkConfig {

  private final Path path = FabricLoader.getInstance().getConfigDir()
      .resolve("twitchchatlink.properties");
  private final Properties properties = new Properties();

  public TwitchChatLinkConfig() {
    loadConfig();
  }

  public void loadConfig() {
    if (!Files.exists(path)) {
      createDefaultConfig();
      return;
    }

    try (InputStream input = Files.newInputStream(path)) {
      properties.load(input);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void saveConfig() {
    if (!Files.exists(path.getParent())) {
      try {
        Files.createDirectories(path.getParent());
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
    }

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      properties.store(outputStream, "Twitch Chat Link Config");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void createDefaultConfig() {
    properties.setProperty("token", "");
    properties.setProperty("userId", "");
    properties.setProperty("defaultLogin", "");
    saveConfig();
  }

  public void setToken(String token) {
    properties.setProperty("token", token);
  }

  public void setUserId(String userId) {
    properties.setProperty("userId", userId);
  }

  public void setDefaultLogin(String login) {
    properties.setProperty("defaultLogin", login);
  }

  public String getToken() {
    return properties.getProperty("token");
  }

  public String getUserId() {
    return properties.getProperty("userId");
  }

  public String getDefaultLogin() {
    return properties.getProperty("defaultLogin");
  }
}
