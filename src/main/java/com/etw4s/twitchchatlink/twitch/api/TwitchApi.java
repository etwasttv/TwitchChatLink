package com.etw4s.twitchchatlink.twitch.api;

import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.model.TwitchChannel;
import com.etw4s.twitchchatlink.model.TwitchChannel.LiveStatus;
import com.etw4s.twitchchatlink.twitch.request.ChannelChatMessageCondition;
import com.etw4s.twitchchatlink.twitch.request.CreateEventSubSubscriptionRequest;
import com.etw4s.twitchchatlink.twitch.request.Transport;
import com.etw4s.twitchchatlink.twitch.response.CreateEventSubSubscriptionResponse;
import com.etw4s.twitchchatlink.twitch.response.GetEmoteSetResponse;
import com.etw4s.twitchchatlink.twitch.response.GetUsersResponse;
import com.etw4s.twitchchatlink.twitch.response.SearchChannelsResponse;
import com.etw4s.twitchchatlink.twitch.result.CreateEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.result.DeleteEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.result.GetEmoteSetResult;
import com.etw4s.twitchchatlink.twitch.result.GetUsersResult;
import com.etw4s.twitchchatlink.twitch.result.SearchChannelsResult;
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

  private static final String ENDPOINT = "https://api.twitch.tv/helix";

  public static GetUsersResult getUsersByLogin(String[] logins) {
    HttpRequest request = TwitchApiRequestBuilder.newBuilder()
        .uri(URI.create(ENDPOINT + "/users?" + String.join("&",
            Arrays.stream(logins).map(login -> "login=" + login).toList())))
        .header("Content-Type", "application/json").GET().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() != HttpStatus.SC_OK) {
      LOGGER.warning("Failed to get users: " + response.body());
      throw new TwitchApiException("Failed to get users", response.statusCode());
    }

    var body = gson.fromJson(response.body(), GetUsersResponse.class);
    var channels = Arrays.stream(body.data)
        .map(u -> new TwitchChannel(u.id, u.login, u.displayName, LiveStatus.Unknown)).toList();
    return new GetUsersResult(channels);
  }

  public static CreateEventSubSubscriptionResult createChannelChatMessageSubscription(
      String sessionId, TwitchChannel broadcaster) {
    var requestBody = new CreateEventSubSubscriptionRequest();
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
    requestBody.condition = new ChannelChatMessageCondition(broadcaster.id(), config.getUserId());
    requestBody.version = "1";
    requestBody.type = "channel.chat.message";
    requestBody.transport = new Transport("websocket", sessionId);
    HttpRequest request = TwitchApiRequestBuilder.newBuilder()
        .uri(URI.create(ENDPOINT + "/eventsub/subscriptions"))
        .header("Content-Type", "application/json")
        .POST(BodyPublishers.ofString(gson.toJson(requestBody))).build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() != HttpStatus.SC_ACCEPTED) {
      LOGGER.warning("Failed to create EventSub subscription: " + response.body());
      throw new TwitchApiException("Failed to create subscription", response.statusCode());
    }

    var body = gson.fromJson(response.body(), CreateEventSubSubscriptionResponse.class);
    LOGGER.info("EventSub subscription created: " + body.data[0].id);
    return new CreateEventSubSubscriptionResult(body.data[0].id, body.data[0].type);
  }

  public static DeleteEventSubSubscriptionResult deleteEventSubSubscription(
      String subscriptionId) {
    var request = TwitchApiRequestBuilder.newBuilder()
        .uri(URI.create(ENDPOINT + "/eventsub/subscriptions?id=" + subscriptionId))
        .DELETE().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() != HttpStatus.SC_NO_CONTENT) {
      throw new TwitchApiException("Failed to delete subscription", response.statusCode());
    }

    return new DeleteEventSubSubscriptionResult();
  }

  public static GetEmoteSetResult getEmoteSet(String emoteSetId) {
    var request = TwitchApiRequestBuilder.newBuilder()
        .uri(URI.create(ENDPOINT + "/chat/emotes/set?emote_set_id=" + emoteSetId))
        .header("Content-Type", "application/json").GET().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() != HttpStatus.SC_OK) {
      throw new TwitchApiException("Failed to get emote set", response.statusCode());
    }

    var body = gson.fromJson(response.body(), GetEmoteSetResponse.class);
    var emotes = Arrays.stream(body.data).map(d -> new TwitchEmoteInfo(d.id, d.name, d.format,
        d.scale, d.theme_mode, body.template)).toList();
    return GetEmoteSetResult.success(emoteSetId, emotes);
  }

  public static SearchChannelsResult searchChannels(String query,
      boolean liveOnly, int count, String after) throws TwitchApiException {
    StringBuilder url = new StringBuilder(ENDPOINT + "/search/channels");
    url.append("?query=").append(query);
    url.append("&live_only=").append(liveOnly);
    url.append("&first=").append(count);
    if (after != null && !after.isEmpty()) {
      url.append("&after=").append(after);
    }
    var request = TwitchApiRequestBuilder.newBuilder()
        .uri(URI.create(url.toString()))
        .GET().build();

    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new TwitchApiException("Failed to send request");
    }

    if (response.statusCode() != HttpStatus.SC_OK) {
      LOGGER.warning("Failed to search channels: " + response.body());
      throw new TwitchApiException("Failed to search channels", response.statusCode());
    }

    var body = gson.fromJson(response.body(), SearchChannelsResponse.class);
    var channels = Arrays.stream(body.data).map(data -> new TwitchChannel(data.id(),
        data.broadcasterLogin(), data.displayName(),
        data.isLive() ? LiveStatus.Online : LiveStatus.Offline)).toList();
    var cursor = body.pagination != null ? body.pagination.cursor() : null;
    return new SearchChannelsResult(channels, cursor);
  }
}
