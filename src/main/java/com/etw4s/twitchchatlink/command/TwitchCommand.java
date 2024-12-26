package com.etw4s.twitchchatlink.command;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.twitch.GetUsersResult.Status;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import com.etw4s.twitchchatlink.twitch.auth.AuthManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitchCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);

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
    String login = StringArgumentType.getString(context, "login");
    var future = TwitchApi.getUsersByLogin(new String[]{login});
    future.whenComplete(((getUsersResult, throwable) -> {
      if (getUsersResult.status() == Status.Unauthorized) {
        context.getSource().sendFeedback(Text.literal("認証に失敗しました"));
        AuthManager.getInstance().startAuth();
      } else if (getUsersResult.status() == Status.Ok) {
        var users = getUsersResult.users();
        if (users.isEmpty()) {
          context.getSource().sendFeedback(Text.literal(login + "は見つかりませんでした"));
        } else {
          context.getSource()
              .sendFeedback(Text.literal(users.getFirst().displayName() + "が見つかりました"));
        }
      } else {
        context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
      }
    }));
    return 1;
  }

  private static int authHandler(CommandContext<FabricClientCommandSource> context) {
    AuthManager.getInstance().startAuth();
    return 1;
  }
}
