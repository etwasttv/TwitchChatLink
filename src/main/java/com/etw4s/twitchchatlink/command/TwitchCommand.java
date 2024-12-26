package com.etw4s.twitchchatlink.command;

import com.etw4s.twitchchatlink.twitch.auth.AuthManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

public class TwitchCommand {

  public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
      CommandRegistryAccess access) {
    dispatcher.register(ClientCommandManager.literal("twitch")
        .then(ClientCommandManager.literal("auth")
            .executes(TwitchCommand::authHandler))
        .then(ClientCommandManager.literal("connect")
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .executes(TwitchCommand::connectHandler))));
  }

  private static int connectHandler(CommandContext<FabricClientCommandSource> context) {
    context.getSource().sendFeedback(Text.literal("success!!"));
    return 1;
  }

  private static int authHandler(CommandContext<FabricClientCommandSource> context) {
    AuthManager.getInstance().startAuth();
    return 1;
  }
}
