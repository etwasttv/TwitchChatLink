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
import java.net.http.HttpResponse;
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
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/users?"
            + String.join("&", Arrays.stream(logins).map(login -> "login=" + login).toList())))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json")
        .GET()
        .build();

    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() == HttpStatus.SC_UNAUTHORIZED) {
            return GetUsersResult.Unauthorized();
          } else if (response.statusCode() != HttpStatus.SC_OK) {
            return GetUsersResult.ErrorResult();
          }
          var getUsersResponse = gson.fromJson(response.body(), GetUsersResponse.class);
          return GetUsersResult.OkResult(Arrays.stream(getUsersResponse.data)
              .map(d -> new TwitchUser(d.id, d.login, d.displayName)).toList());
        });
  }

  public static CompletableFuture<CreateEventSubSubscriptionResult> createChannelChatMessageSubscription(
      String sessionId,
      TwitchUser broadcaster) {
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
        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
        .build();

    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() != HttpStatus.SC_ACCEPTED) {
            return CreateEventSubSubscriptionResult.fail();
          }
          var responseBody = gson.fromJson(response.body(),
              CreateEventSubSubscriptionResponse.class);
          if (responseBody.data.length == 0) {
            return CreateEventSubSubscriptionResult.fail();
          }
          return CreateEventSubSubscriptionResult.success(
              responseBody.data[0].id,
              "channel.chat.message");
        });
  }

  public static CompletableFuture<GetEmoteSetResult> getEmoteSet(String emoteSetId) {
    TwitchChatLinkConfig config = TwitchChatLinkConfig.load();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/chat/emotes/set?emote_set_id=" + emoteSetId))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json")
        .GET()
        .build();

    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() == HttpStatus.SC_OK) {
            var body = gson.fromJson(response.body(), GetEmoteSetResponse.class);

            return GetEmoteSetResult.success(
                emoteSetId,
                Arrays.stream(body.data)
                    .map(d -> new TwitchEmoteInfo(d.id, d.name, d.format, d.scale, d.theme_mode, body.template))
                    .toList());
          }
          return GetEmoteSetResult.fail(emoteSetId);
        });
  }
}
