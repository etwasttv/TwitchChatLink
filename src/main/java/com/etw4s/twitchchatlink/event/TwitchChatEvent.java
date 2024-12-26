package com.etw4s.twitchchatlink.event;

import com.etw4s.twitchchatlink.model.TwitchChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@Environment(EnvType.CLIENT)
public class TwitchChatEvent {

  public static final Event<TwitchChatListener> EVENTS = EventFactory.createArrayBacked(
      TwitchChatListener.class,
      listeners -> event -> {
        for (TwitchChatListener listener : listeners) {
          listener.onReceive(event);
        }
      });

  @FunctionalInterface
  @Environment(EnvType.CLIENT)
  public interface TwitchChatListener {
    void onReceive(TwitchChat chat);
  }
}
