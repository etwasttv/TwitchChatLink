package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.TwitchChatLinkContracts;
import com.etw4s.twitchchatlink.twitch.auth.TokenValidator.ValidationResult;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AuthManager {

  private static final AuthManager instance = new AuthManager();
  private final TokenValidator tokenValidator = new TokenValidator();
  private final AuthRedirectServer authRedirectServer;

  public static AuthManager getInstance() {
    return instance;
  }

  private AuthManager() {
    this.authRedirectServer = AuthRedirectServer.getInstance();
  }

  public void startAuth() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null || client.world == null) {
      return;
    }
    try {
      this.authRedirectServer.startRedirectServer();
      sendAuthUrl(client.player);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopAuth() {
    this.authRedirectServer.stopRedirectServer();
  }

  private void sendAuthUrl(ClientPlayerEntity player) {
    MutableText link = Text.literal("ここ")
        .setStyle(Style.EMPTY
            .withBold(true)
            .withUnderline(true)
            .withItalic(true)
            .withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("ブラウザで開く")))
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, getAuthUrl()))
            .withColor(Formatting.GOLD));

    MutableText message = Text.literal("をクリックしてTwitchChatLinkを認証してください")
        .setStyle(Style.EMPTY
            .withColor(Formatting.GOLD));

    MutableText full = Text.empty().append(link).append(message);

    player.sendMessage(full);
  }

  public boolean saveToken(String token) {
    ValidationResult result = tokenValidator.validate(token);
    if (!result.isValidated())
      return false;

    var config = new TwitchChatLinkConfig();
    config.setToken(token);
    config.setUserId(result.userId());
    config.saveConfig();

    ClientPlayerEntity player = MinecraftClient.getInstance().player;
    if (player != null) {
      MutableText text = Text.literal("認証に成功しました!");
      player.sendMessage(text);
    }
    return true;
  }

  private String getAuthUrl() {
    return "https://id.twitch.tv/oauth2/authorize?client_id="
        + TwitchChatLinkContracts.TWITCH_CLIENT_ID
        + "&redirect_uri=" + this.authRedirectServer.getRedirectUrl()
        + "&scope=" + URLEncoder.encode("user:read:chat", StandardCharsets.UTF_8)
        + "&response_type=token";
  }
}
