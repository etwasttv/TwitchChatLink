package com.etw4s.twitchchatlink.client;

import com.etw4s.twitchchatlink.command.TwitchCommand;
import com.etw4s.twitchchatlink.event.TwitchChatEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TwitchChatLinkClient implements ClientModInitializer {

  private final TwitchCommand command;

  public TwitchChatLinkClient() {
    this.command = new TwitchCommand();
  }

  @Override
  public void onInitializeClient() {
    registerCommands();
    ClientTickEvents.START_WORLD_TICK.register(EmoteManager.getInstance());
    TwitchChatEvent.EVENTS.register(new TwitchChatEventListener());
    TwitchChatEvent.EVENTS.register(EmoteManager.getInstance());
  }

  private void registerCommands() {
    ClientCommandRegistrationCallback.EVENT
        .register((dispatcher, registryAcce) -> this.command.register(dispatcher, registryAcce));
  }
}
