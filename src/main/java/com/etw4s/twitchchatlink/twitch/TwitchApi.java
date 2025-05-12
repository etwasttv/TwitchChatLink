package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.TwitchChatLinkContracts;
import com.etw4s.twitchchatlink.model.TwitchChannel;
import com.etw4s.twitchchatlink.model.TwitchChannel.LiveStatus;
import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
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
            var channels = Arrays.stream(body.data)
                .map(u -> new TwitchChannel(u.id, u.login, u.displayName, LiveStatus.Unknown)).toList();
            yield new GetUsersResult(channels);
          }
          case HttpStatus.SC_BAD_REQUEST ->
              throw new TwitchApiException("Bad Request", HttpStatus.SC_BAD_REQUEST);
          case HttpStatus.SC_UNAUTHORIZED ->
              throw new TwitchApiException("Unauthorized", HttpStatus.SC_UNAUTHORIZED);
          default -> throw new IllegalStateException("Unexpected value: " + response.statusCode());
        });
  }

  public static CompletableFuture<CreateEventSubSubscriptionResult> createChannelChatMessageSubscription(
      String sessionId, TwitchChannel broadcaster) {
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
            yield new CreateEventSubSubscriptionResult(body.data[0].id, body.data[0].type);
          }
          case HttpStatus.SC_BAD_REQUEST,
               HttpStatus.SC_UNAUTHORIZED,
               HttpStatus.SC_FORBIDDEN,
               HttpStatus.SC_TOO_MANY_REQUESTS ->
              throw new TwitchApiException(response.statusCode());
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
          case HttpStatus.SC_NO_CONTENT -> new DeleteEventSubSubscriptionResult();
          case HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_UNAUTHORIZED,
               HttpStatus.SC_NOT_FOUND -> throw new TwitchApiException(response.statusCode());
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

  public static CompletableFuture<SearchChannelsResult> searchChannels(String query,
      boolean liveOnly, int count, String after) throws TwitchApiException {
    TwitchChatLinkConfig config = TwitchChatLinkConfig.load();
    StringBuilder url = new StringBuilder("https://api.twitch.tv/helix/search/channels");
    url.append("?query=").append(query);
    url.append("&live_only=").append(liveOnly);
    url.append("&first=").append(count);
    if (after != null && !after.isEmpty()) {
      url.append("&after=").append(after);
    }
    var request = HttpRequest.newBuilder()
        .uri(URI.create(url.toString()))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .GET().build();
    return client.sendAsync(request, BodyHandlers.ofString())
        .thenApply(response ->
            switch (response.statusCode()) {
              case HttpStatus.SC_OK -> {
                var body = gson.fromJson(response.body(), SearchChannelsResponse.class);
                var channels = Arrays.stream(body.data).map(data -> new TwitchChannel(data.id(),
                    data.broadcasterLogin(), data.displayName(),
                    data.isLive() ? LiveStatus.Online : LiveStatus.Offline)).toList();
                var cursor = body.pagination != null ? body.pagination.cursor() : null;
                yield new SearchChannelsResult(channels, cursor);
              }
              case HttpStatus.SC_BAD_REQUEST ->
                  throw new TwitchApiException("BadRequest", HttpStatus.SC_BAD_REQUEST);
              case HttpStatus.SC_UNAUTHORIZED ->
                  throw new TwitchApiException("Unauthorized", HttpStatus.SC_UNAUTHORIZED);
              default ->
                  throw new IllegalStateException("Unexpected value: " + response.statusCode());
            });
  }
}
