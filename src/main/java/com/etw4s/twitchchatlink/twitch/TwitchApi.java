package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.TwitchChatLinkContracts;
import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
import com.etw4s.twitchchatlink.model.TwitchUser;
import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitchApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);

  private static final HttpClient client = HttpClient.newBuilder().build();
  private static final Gson gson = TwitchChatLinkGson.getGson();

  public static CompletableFuture<GetUsersResult> getUsersByLogin(String[] logins) {

    TwitchChatLinkConfig config = TwitchChatLinkConfig.load();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(
            "https://api.twitch.tv/helix/users?" + String.join("&",
                Arrays.stream(logins).map(login -> "login=" + login).toList())))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json").GET().build();

    return client.sendAsync(request, BodyHandlers.ofString()).thenApply(response ->
        switch (response.statusCode()) {
          case HttpStatus.SC_OK -> {
            var body = gson.fromJson(response.body(), GetUsersResponse.class);
            yield GetUsersResult.success(Arrays.stream(body.data)
                .map(user -> new TwitchUser(user.id, user.login, user.displayName)).toList());
          }
          case HttpStatus.SC_BAD_REQUEST -> GetUsersResult.badRequest();
          case HttpStatus.SC_UNAUTHORIZED -> GetUsersResult.unauthorized();
          default -> throw new IllegalStateException("Unexpected value: " + response.statusCode());
        });
  }

  public static CompletableFuture<CreateEventSubSubscriptionResult> createChannelChatMessageSubscription(
      String sessionId, TwitchUser broadcaster) {
    TwitchChatLinkConfig config = TwitchChatLinkConfig.load();
    var requestBody = new CreateEventSubSubscriptionRequest();
    requestBody.condition = new ChannelChatMessageCondition(broadcaster.id(), config.getUserId());
    requestBody.version = "1";
    requestBody.type = "channel.chat.message";
    requestBody.transport = new Transport("websocket", sessionId);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/eventsub/subscriptions"))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(gson.toJson(requestBody))).build();

    return client.sendAsync(request, BodyHandlers.ofString()).thenApply(response ->
        switch (response.statusCode()) {
          case HttpStatus.SC_ACCEPTED -> {
            var body = gson.fromJson(response.body(), CreateEventSubSubscriptionResponse.class);
            yield CreateEventSubSubscriptionResult.success(body.data[0].id, body.data[0].type);
          }
          case HttpStatus.SC_BAD_REQUEST -> CreateEventSubSubscriptionResult.badRequest();
          case HttpStatus.SC_UNAUTHORIZED -> CreateEventSubSubscriptionResult.unauthorized();
          case HttpStatus.SC_FORBIDDEN -> CreateEventSubSubscriptionResult.forbidden();
          case HttpStatus.SC_TOO_MANY_REQUESTS ->
              CreateEventSubSubscriptionResult.tooManyRequests();
          default -> throw new IllegalStateException("Unexpected value: " + response.statusCode());
        });
  }

  public static CompletableFuture<DeleteEventSubSubscriptionResult> deleteEventSubSubscription(
      String subscriptionId) {
    TwitchChatLinkConfig config = TwitchChatLinkConfig.load();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/eventsub/subscriptions?id=" + subscriptionId))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID).DELETE().build();

    return client.sendAsync(request, BodyHandlers.ofString())
        .thenApply(response -> switch (response.statusCode()) {
          case HttpStatus.SC_NO_CONTENT -> DeleteEventSubSubscriptionResult.success();
          case HttpStatus.SC_BAD_REQUEST -> DeleteEventSubSubscriptionResult.badRequest();
          case HttpStatus.SC_UNAUTHORIZED -> DeleteEventSubSubscriptionResult.unauthorized();
          case HttpStatus.SC_NOT_FOUND -> DeleteEventSubSubscriptionResult.notFound();
          default -> throw new IllegalStateException("Unexpected value: " + response.statusCode());
        });
  }

  public static CompletableFuture<GetEmoteSetResult> getEmoteSet(String emoteSetId) {
    TwitchChatLinkConfig config = TwitchChatLinkConfig.load();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/chat/emotes/set?emote_set_id=" + emoteSetId))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json").GET().build();

    return client.sendAsync(request, BodyHandlers.ofString())
        .thenApply(response -> switch (response.statusCode()) {
          case HttpStatus.SC_OK -> {
            var body = gson.fromJson(response.body(), GetEmoteSetResponse.class);
            yield GetEmoteSetResult.success(emoteSetId, Arrays.stream(body.data).map(
                d -> new TwitchEmoteInfo(d.id, d.name, d.format, d.scale, d.theme_mode,
                    body.template)).toList());
          }
          case HttpStatus.SC_BAD_REQUEST -> GetEmoteSetResult.badRequest(emoteSetId);
          case HttpStatus.SC_UNAUTHORIZED -> GetEmoteSetResult.unauthorized(emoteSetId);
          default -> throw new IllegalStateException("Unexpected value: " + response.statusCode());
        });
  }
}
