package com.etw4s.twitchchatlink.client;

import com.etw4s.twitchchatlink.command.TwitchCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class TwitchChatLinkClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    ClientCommandRegistrationCallback.EVENT.register(TwitchCommand::register);
  }
}
