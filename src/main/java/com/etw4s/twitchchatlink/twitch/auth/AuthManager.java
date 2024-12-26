package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.TwitchChatLinkContracts;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
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

  public static AuthManager getInstance() {
    return instance;
  }

  private AuthManager() {
  }

  public void startAuth() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null || client.world == null) {
      return;
    }
    try {
      AuthRedirectServer.getInstance().startRedirectServer();
      sendAuthUrl(client.player);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendAuthUrl(ClientPlayerEntity player) {
    MutableText link = Text.literal("ここ")
        .setStyle(Style.EMPTY
            .withBold(true)
            .withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("ブラウザで開く")))
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, getAuthUrl()))
            .withColor(Formatting.GOLD));

    MutableText message = Text.literal("をクリックしてTwitchChatLinkを認証してください")
        .setStyle(Style.EMPTY
            .withColor(Formatting.WHITE));

    MutableText full = Text.empty().append(link).append(message);

    player.sendMessage(full);
  }

  public CompletableFuture<Boolean> saveToken(String token) {
    return TokenValidator.validate(token)
        .thenApply((result -> {
          if (result.isValidated()) {
            var config = TwitchChatLinkConfig.load();
            config.setToken(token);
            config.setUserId(result.userId());
            config.save();

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
              MutableText text = Text.literal("認証に成功しました!");
              player.sendMessage(text);
            }
            return true;
          }
          return false;
        }));
  }

  private String getAuthUrl() {
    return "https://id.twitch.tv/oauth2/authorize?client_id="
        + TwitchChatLinkContracts.TWITCH_CLIENT_ID
        + "&redirect_uri=" + AuthRedirectServer.getInstance().getRedirectUrl()
        + "&scope=" + URLEncoder.encode("user:read:chat", StandardCharsets.UTF_8)
        + "&response_type=token";
  }
}
