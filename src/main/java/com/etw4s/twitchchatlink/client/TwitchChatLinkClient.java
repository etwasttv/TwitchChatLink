package com.etw4s.twitchchatlink.client;

import com.etw4s.twitchchatlink.command.TwitchCommand;
import com.etw4s.twitchchatlink.event.TwitchChatEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TwitchChatLinkClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    ClientCommandRegistrationCallback.EVENT.register(TwitchCommand::register);
    ClientTickEvents.START_WORLD_TICK.register(EmoteManager.getInstance());
    TwitchChatEvent.EVENTS.register(new TwitchChatEventListener());
    TwitchChatEvent.EVENTS.register(EmoteManager.getInstance());
  }
}
