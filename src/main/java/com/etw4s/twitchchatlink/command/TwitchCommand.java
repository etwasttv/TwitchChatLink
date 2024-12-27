package com.etw4s.twitchchatlink.command;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.twitch.CreateEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.DeleteEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.GetUsersResult.Status;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import com.etw4s.twitchchatlink.twitch.auth.AuthManager;
import com.etw4s.twitchchatlink.twitch.eventsub.EventSubClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
                .executes(TwitchCommand::connectHandler)))
        .then(ClientCommandManager.literal("list")
            .executes(TwitchCommand::listHandler))
        .then(ClientCommandManager.literal("disconnect")
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .executes(TwitchCommand::disconnectHandler))));
  }

  private static int disconnectHandler(CommandContext<FabricClientCommandSource> context) {
    String login = StringArgumentType.getString(context, "login");
    EventSubClient.getInstance().unsubscribe(login)
        .thenAccept(result -> {
          if (result.status() == DeleteEventSubSubscriptionResult.Status.Success) {
            context.getSource().sendFeedback(Text.literal(login + " から切断しました"));
          } else {
            context.getSource().sendFeedback(Text.literal(login + " から切断できませんでした\nすでに切断しているか、存在しないチャンネルの可能性があります"));
          }
        });
    context.getSource().sendFeedback(Text.literal(login + " から切断します"));

    return 1;
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
          EventSubClient.getInstance().subscribe(users.getFirst()).thenAccept(result -> {
                if (result.status() == CreateEventSubSubscriptionResult.Status.Success) {
                  context.getSource()
                      .sendFeedback(
                          Text.literal(users.getFirst().displayName() + "のチャットが表示されます"));
                } else {
                  context.getSource()
                      .sendFeedback(Text.literal(
                          users.getFirst().displayName() + "のチャットに接続できませんでした"));
                }
              }
          );
        }
      } else {
        context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
      }
    }));
    context.getSource().sendFeedback(Text.literal(login + " に接続します"));
    return 1;
  }

  private static int authHandler(CommandContext<FabricClientCommandSource> context) {
    AuthManager.getInstance().startAuth();
    return 1;
  }

  private static int listHandler(CommandContext<FabricClientCommandSource> context) {
    var subscribes = EventSubClient.getInstance().getSubscribeList();
    if (subscribes.isEmpty()) {
      var text = Text.literal("現在、接続しているチャンネルはありません");
      context.getSource().sendFeedback(text);
    } else {
      var text = Text.literal("現在、以下のチャンネルに接続しています\n");
      for (var broadcaster : subscribes) {
        text.append(
            Text.literal(broadcaster.displayName() + "(" + broadcaster.login() + ")").setStyle(
                Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal(broadcaster.getChannelUrl())))
                    .withClickEvent(
                        new ClickEvent(ClickEvent.Action.OPEN_URL, broadcaster.getChannelUrl()))
                    .withColor(Formatting.LIGHT_PURPLE)));
        text.append(" ");
      }
      context.getSource().sendFeedback(text);
    }

    return 1;
  }
}
