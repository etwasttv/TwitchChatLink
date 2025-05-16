package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.TwitchChatLinkContracts;
import com.etw4s.twitchchatlink.model.TwitchChannel;
import com.etw4s.twitchchatlink.model.TwitchChannel.LiveStatus;
import com.etw4s.twitchchatlink.twitch.response.CreateEventSubSubscriptionResponse;
import com.etw4s.twitchchatlink.twitch.response.GetEmoteSetResponse;
import com.etw4s.twitchchatlink.twitch.response.GetUsersResponse;
import com.etw4s.twitchchatlink.twitch.response.SearchChannelsResponse;
import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;

public class TwitchApi {

  private static final HttpClient client = HttpClient.newBuilder().build();
  private static final Gson gson = TwitchChatLinkGson.getGson();

  private static final Logger LOGGER = Logger.getLogger(TwitchApi.class.getName());

  public static GetUsersResult getUsersByLogin(String[] logins) {
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(
        "https://api.twitch.tv/helix/users?" + String.join("&",
            Arrays.stream(logins).map(login -> "login=" + login).toList())))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json").GET().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() == HttpStatus.SC_OK) {
      var body = gson.fromJson(response.body(), GetUsersResponse.class);
      var channels = Arrays.stream(body.data)
          .map(u -> new TwitchChannel(u.id, u.login, u.displayName, LiveStatus.Unknown)).toList();
      return new GetUsersResult(channels);
    } else if (response.statusCode() == HttpStatus.SC_BAD_REQUEST) {
      throw new TwitchApiException("Bad Request");
    } else if (response.statusCode() == HttpStatus.SC_UNAUTHORIZED) {
      throw new TwitchApiException("Unauthorized");
    } else {
      throw new IllegalStateException("Unexpected value: " + response.statusCode());
    }
  }

  public static CreateEventSubSubscriptionResult createChannelChatMessageSubscription(
      String sessionId, TwitchChannel broadcaster) {
    LOGGER.info("Creating EventSub subscription for channel: " + broadcaster.login());
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
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

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() == HttpStatus.SC_ACCEPTED) {
      var body = gson.fromJson(response.body(), CreateEventSubSubscriptionResponse.class);
      LOGGER.info("EventSub subscription created: " + body.data[0].id);
      return new CreateEventSubSubscriptionResult(body.data[0].id, body.data[0].type);
    } else if (response.statusCode() == HttpStatus.SC_BAD_REQUEST) {
      throw new TwitchApiException("Bad Request", HttpStatus.SC_BAD_REQUEST);
    } else if (response.statusCode() == HttpStatus.SC_UNAUTHORIZED) {
      throw new TwitchApiException("Unauthorized", HttpStatus.SC_UNAUTHORIZED);
    } else if (response.statusCode() == HttpStatus.SC_FORBIDDEN) {
      throw new TwitchApiException("Forbidden", HttpStatus.SC_FORBIDDEN);
    } else if (response.statusCode() == HttpStatus.SC_TOO_MANY_REQUESTS) {
      throw new TwitchApiException("Too Many Requests", HttpStatus.SC_TOO_MANY_REQUESTS);
    } else {
      throw new IllegalStateException("Unexpected value: " + response.statusCode());
    }
  }

  public static DeleteEventSubSubscriptionResult deleteEventSubSubscription(
      String subscriptionId) {
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/eventsub/subscriptions?id=" + subscriptionId))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID).DELETE().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() == HttpStatus.SC_NO_CONTENT) {
      return new DeleteEventSubSubscriptionResult();
    } else if (response.statusCode() == HttpStatus.SC_BAD_REQUEST) {
      throw new TwitchApiException("Bad Request", HttpStatus.SC_BAD_REQUEST);
    } else if (response.statusCode() == HttpStatus.SC_UNAUTHORIZED) {
      throw new TwitchApiException("Unauthorized", HttpStatus.SC_UNAUTHORIZED);
    } else if (response.statusCode() == HttpStatus.SC_NOT_FOUND) {
      throw new TwitchApiException("Not Found", HttpStatus.SC_NOT_FOUND);
    } else {
      throw new IllegalStateException("Unexpected value: " + response.statusCode());
    }
  }

  public static GetEmoteSetResult getEmoteSet(String emoteSetId) {
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.twitch.tv/helix/chat/emotes/set?emote_set_id=" + emoteSetId))
        .header("Authorization", "Bearer " + config.getToken())
        .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID)
        .header("Content-Type", "application/json").GET().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() == HttpStatus.SC_OK) {
      var body = gson.fromJson(response.body(), GetEmoteSetResponse.class);
      var emotes = Arrays.stream(body.data).map(d -> new TwitchEmoteInfo(d.id, d.name, d.format,
          d.scale, d.theme_mode, body.template)).toList();
      return GetEmoteSetResult.success(emoteSetId, emotes);
    } else if (response.statusCode() == HttpStatus.SC_BAD_REQUEST) {
      return GetEmoteSetResult.badRequest(emoteSetId);
    } else if (response.statusCode() == HttpStatus.SC_UNAUTHORIZED) {
      return GetEmoteSetResult.unauthorized(emoteSetId);
    } else {
      throw new IllegalStateException("Unexpected value: " + response.statusCode());
    }
  }

  public static SearchChannelsResult searchChannels(String query,
      boolean liveOnly, int count, String after) throws TwitchApiException {
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
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

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() == HttpStatus.SC_OK) {
      var body = gson.fromJson(response.body(), SearchChannelsResponse.class);
      var channels = Arrays.stream(body.data).map(data -> new TwitchChannel(data.id(),
          data.broadcasterLogin(), data.displayName(),
          data.isLive() ? LiveStatus.Online : LiveStatus.Offline)).toList();
      var cursor = body.pagination != null ? body.pagination.cursor() : null;
      return new SearchChannelsResult(channels, cursor);
    } else if (response.statusCode() == HttpStatus.SC_BAD_REQUEST) {
      throw new TwitchApiException("Bad Request", HttpStatus.SC_BAD_REQUEST);
    } else if (response.statusCode() == HttpStatus.SC_UNAUTHORIZED) {
      throw new TwitchApiException("Unauthorized", HttpStatus.SC_UNAUTHORIZED);
    } else {
      throw new IllegalStateException("Unexpected value: " + response.statusCode());
    }
  }
}
