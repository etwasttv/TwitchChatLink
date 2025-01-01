package com.etw4s.twitchchatlink.twitch.eventsub;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.event.TwitchChatEvent;
import com.etw4s.twitchchatlink.model.ChatFragment;
import com.etw4s.twitchchatlink.model.TwitchChat;
import com.etw4s.twitchchatlink.model.TwitchUser;
import com.etw4s.twitchchatlink.twitch.CreateEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.DeleteEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSubClient implements Listener {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);
  private static final EventSubClient instance = new EventSubClient();
  private volatile WebSocket webSocket;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private volatile String sessionId;
  private final Gson gson = TwitchChatLinkGson.getGson();
  private final Map<String, TwitchUser> subscribes = Collections.synchronizedMap(new HashMap<>());

  private EventSubClient() {
  }

  public static EventSubClient getInstance() {
    return instance;
  }

  public CompletableFuture<CreateEventSubSubscriptionResult> subscribe(TwitchUser broadcaster) {
    return connect().thenCompose((_v) -> {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return TwitchApi.createChannelChatMessageSubscription(sessionId, broadcaster)
          .thenApply(result -> {
            subscribes.put(result.subscriptionId(), broadcaster);
            return result;
          });
    });
  }

  public CompletableFuture<DeleteEventSubSubscriptionResult> unsubscribe(String login) {
    var target = subscribes.entrySet().stream().filter(s -> s.getValue().login().equals(login))
        .findFirst();
    if (target.isEmpty()) {
      return CompletableFuture.completedFuture(
          new DeleteEventSubSubscriptionResult());
    }
    return TwitchApi.deleteEventSubSubscription(target.get().getKey())
        .thenApply(result -> {
          subscribes.remove(target.get().getKey());
          return result;
        });
  }

  public List<TwitchUser> getSubscribeList() {
    return new ArrayList<>(subscribes.values());
  }

  private CompletableFuture<Void> connect() {
    synchronized (this) {
      if (webSocket != null) {
        return CompletableFuture.completedFuture(null);
      }
      return httpClient.newWebSocketBuilder()
          .buildAsync(URI.create("wss://eventsub.wss.twitch.tv/ws"), this).thenAccept(ws -> {
            webSocket = ws;
            LOGGER.info("WebSocket is created");
          });
    }
  }

  public CompletableFuture<Void> disconnect() {
    synchronized (this) {
      if (webSocket == null) {
        LOGGER.info("WebSocket is already closed");
        return CompletableFuture.completedFuture(null);
      }
      return webSocket.sendClose(1000, "Close").thenAccept(ws -> {
        clear();
        LOGGER.info("WebSocket is closed");
      });
    }
  }

  private void clear() {
    webSocket = null;
    sessionId = null;
    subscribes.clear();
  }

  @Override
  public void onOpen(WebSocket webSocket) {
    LOGGER.info("WebSocket is opened");
    webSocket.request(10);
  }

  StringBuffer buffer = new StringBuffer();

  @Override
  public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    webSocket.request(1);
    buffer.append(data);
    if (last) {
      try {
        var message = gson.fromJson(buffer.toString(), WebSocketMessage.class);
        buffer.delete(0, buffer.length());
        LOGGER.info("Message Type is {}", message.metadata.messageType);
        switch (message.metadata.messageType) {
          case "session_welcome" -> sessionId = message.payload.session.id;
          case "notification" -> handleNotification(message);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return Listener.super.onText(webSocket, data, last);
  }

  private void handleNotification(WebSocketMessage message) {
    if (message.metadata.subscriptionType.equals("channel.chat.message")) {
      var event = message.payload.event;
      var broadcaster = new TwitchUser(
          event.broadcasterUserId,
          event.broadcasterUserLogin,
          event.broadcasterUserName);
      var chatter = new TwitchUser(
          event.chatterUserId,
          event.chatterUserLogin,
          event.chatterUserName);
      var fragments = Arrays.stream(event.message.fragments).map(f -> {
        if (f.type.equals("emote")) {
          return new ChatFragment(f.text, f.emote.id, f.emote.emoteSetId);
        }
        return new ChatFragment(f.text);
      }).toList();
      var twitchChat = new TwitchChat(broadcaster, chatter, event.message.text, fragments,
          event.color);
      TwitchChatEvent.EVENTS.invoker().onReceive(twitchChat);
    }
  }

  @Override
  public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
    LOGGER.info("WebSocket is closing, {}: {}", statusCode, reason);
    clear();
    return Listener.super.onClose(webSocket, statusCode, reason);
  }

  @Override
  public void onError(WebSocket webSocket, Throwable error) {
    LOGGER.info("WebSocket on error, {}", error.getMessage());
    clear();
    Listener.super.onError(webSocket, error);
  }
}
