package com.etw4s.twitchchatlink.command;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.model.TwitchUser;
import com.etw4s.twitchchatlink.twitch.CreateEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.DeleteEventSubSubscriptionResult;
import com.etw4s.twitchchatlink.twitch.GetUsersResult.Status;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import com.etw4s.twitchchatlink.twitch.auth.AuthManager;
import com.etw4s.twitchchatlink.twitch.eventsub.EventSubClient;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
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
        .then(ClientCommandManager.literal("auth").executes(TwitchCommand::authHandler))
        .then(ClientCommandManager.literal("search")
            .then(ClientCommandManager.argument("query", StringArgumentType.greedyString())
                .executes(TwitchCommand::searchHandler)
                .then(ClientCommandManager.argument("cursor", StringArgumentType.string())
                    .executes(TwitchCommand::searchHandler))))
        .then(ClientCommandManager.literal("connect")
            .executes(TwitchCommand::connectToDefault)
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .executes(TwitchCommand::connectHandler)))
        .then(ClientCommandManager.literal("list").executes(TwitchCommand::listHandler))
        .then(ClientCommandManager.literal("disconnect")
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .suggests(getSubscribesSuggestion())
                .executes(TwitchCommand::disconnectHandler)))
        .then(ClientCommandManager.literal("set-default")
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .executes(TwitchCommand::setDefaultHandler))));
  }

  private static int searchHandler(CommandContext<FabricClientCommandSource> context) {
    String query = StringArgumentType.getString(context, "query");
    String cursor = null;
    try {
      cursor = StringArgumentType.getString(context, "cursor");
    } catch (IllegalArgumentException ignored) {
    }
    var future = TwitchApi.searchChannels(query, false, 8, cursor);
    future.whenComplete(((result, throwable) -> {
      if (throwable != null) {
        LOGGER.info("Search Command Exception: {}", throwable.getMessage());
        context.getSource().sendFeedback(
            Text.literal("検索できませんでした。").setStyle(Style.EMPTY.withColor(Formatting.RED)));
        return;
      }

      var response = Text.empty();
      response.append(
          Text.literal(query + "の検索結果 ==========\n")
              .setStyle(Style.EMPTY
                  .withBold(true)
                  .withColor(Formatting.GOLD)));
      var itr = result.channels().iterator();
      while (itr.hasNext()) {
        var data = itr.next();
        var connect = Text.empty();
        connect.append(Text.literal("["));
        connect.append(Text.literal("接続").setStyle(Style.EMPTY
            .withColor(Formatting.GOLD)));
        connect.append(Text.literal("]"));
        connect.setStyle(Style.EMPTY
            .withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.literal("クリックして" + data.displayName() + "のチャットに接続する")))
            .withClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/twitch connect " + data.login())));
        response.append(connect);
        response.append(" ");
        var channelInfo = Text.empty();
        channelInfo.append(
            Text.literal(data.displayName()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));
        channelInfo.append(Text.literal("(").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        channelInfo.append(Text.literal(data.login())
            .setStyle(Style.EMPTY
                .withColor(Formatting.DARK_AQUA)
                .withItalic(true)));
        channelInfo.append(Text.literal(")").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        channelInfo.setStyle(Style.EMPTY
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Text.empty()
                    .append(Text.literal("クリックで"))
                    .append(
                        Text.literal(data.getUrl())
                            .setStyle(Style.EMPTY
                                .withColor(Formatting.BLUE)))
                    .append(Text.literal("を開く"))))
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, data.getUrl())));
        response.append(channelInfo);

        if (itr.hasNext()) {
          response.append("\n");
        }
      }
      if (result.cursor() != null) {
        response.append("\n");
        response.append("     [");
        response.append(Text.literal("次のページ").setStyle(
            Style.EMPTY
                .withClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/twitch search " + query + " " + StringArgumentType.escapeIfRequired(
                            result.cursor())))
                .withUnderline(true)
                .withBold(true)
                .withItalic(true)
                .withColor(Formatting.DARK_GRAY)));
        response.append("]");
      }
      context.getSource().sendFeedback(response);
    }));
    return 1;
  }

  private static int connectToDefault(CommandContext<FabricClientCommandSource> context) {
    var config = TwitchChatLinkConfig.load();
    String login = config.getDefaultLogin();
    if (login == null || login.isEmpty()) {
      context.getSource().sendFeedback(Text.literal("デフォルトの接続先が設定されていません"));
      return 0;
    }
    var future = TwitchApi.getUsersByLogin(new String[]{login});

    future.whenComplete(((getUsersResult, throwable) -> {
      if (throwable != null) {
        context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
        return;
      }
      switch (getUsersResult.status()) {
        case Status.Unauthorized -> {
          context.getSource().sendFeedback(Text.literal("認証に失敗しました"));
          AuthManager.getInstance().startAuth();
        }
        case Status.Success -> {
          var users = getUsersResult.users();
          if (users.isEmpty()) {
            context.getSource().sendFeedback(Text.literal(login + "は見つかりませんでした"));
            config.setDefaultLogin("");
            config.save();
            return;
          }
          var target = users.getFirst();
          EventSubClient.getInstance().subscribe(target).thenAccept(result -> {
            if (result.status() == CreateEventSubSubscriptionResult.Status.Success) {
              context.getSource().sendFeedback(
                  //  Todo: create method to get string: Display(login) of TwitchUser
                  Text.literal(target.getDisplayNameAndLogin() + "のチャットが表示されます"));
            } else {
              context.getSource().sendFeedback(Text.literal(
                  users.getFirst().displayName() + "のチャットに接続できませんでした"));
            }
          });
        }
      }
    }));
    return 1;
  }

  private static int setDefaultHandler(CommandContext<FabricClientCommandSource> context) {
    var config = TwitchChatLinkConfig.load();
    String login = StringArgumentType.getString(context, "login");
    if (login.isEmpty()) {
      config.setDefaultLogin("");
      config.save();
      context.getSource().sendFeedback(Text.literal("デフォルトの接続設定を削除しました"));
      return 1;
    }
    var future = TwitchApi.getUsersByLogin(new String[]{login});
    future.whenComplete(((getUsersResult, throwable) -> {
      if (throwable != null) {
        context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
        return;
      }
      switch (getUsersResult.status()) {
        case Status.Unauthorized -> {
          context.getSource().sendFeedback(Text.literal("認証に失敗しました"));
          AuthManager.getInstance().startAuth();
        }
        case Status.Success -> {
          var users = getUsersResult.users();
          if (users.isEmpty()) {
            context.getSource().sendFeedback(Text.literal(login + "は見つかりませんでした"));
            return;
          }
          var target = users.getFirst();
          config.setDefaultLogin(target.login());
          config.save();
          context.getSource().sendFeedback(Text.literal(
              target.getDisplayNameAndLogin() + "をデフォルトの接続先に設定しました"));
        }
      }
    }));
    return 1;
  }

  private static int disconnectHandler(CommandContext<FabricClientCommandSource> context) {
    String login = StringArgumentType.getString(context, "login");
    EventSubClient.getInstance().unsubscribe(login).thenAccept(result -> {
      switch (result.status()) {
        case DeleteEventSubSubscriptionResult.Status.Success ->
            context.getSource().sendFeedback(Text.literal(login + " から切断しました"));
        case DeleteEventSubSubscriptionResult.Status.BadRequest,
             DeleteEventSubSubscriptionResult.Status.NotFound ->
            context.getSource().sendFeedback(Text.literal(login
                + " から切断できませんでした\nすでに切断しているか、存在しないチャンネルの可能性があります"));
        case DeleteEventSubSubscriptionResult.Status.Unauthorized -> {
          context.getSource().sendFeedback(Text.literal("認証に失敗しました"));
          AuthManager.getInstance().startAuth();
        }
      }
    });
    context.getSource().sendFeedback(Text.literal(login + " から切断します"));

    return 1;
  }

  private static int connectHandler(CommandContext<FabricClientCommandSource> context) {
    String login = StringArgumentType.getString(context, "login");
    var future = TwitchApi.getUsersByLogin(new String[]{login});
    future.whenComplete(((getUsersResult, throwable) -> {
      if (throwable != null) {
        context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
        return;
      }
      switch (getUsersResult.status()) {
        case Status.Unauthorized -> {
          context.getSource().sendFeedback(Text.literal("認証に失敗しました"));
          AuthManager.getInstance().startAuth();
        }
        case Status.Success -> {
          var users = getUsersResult.users();
          if (users.isEmpty()) {
            context.getSource().sendFeedback(Text.literal(login + "は見つかりませんでした"));
            return;
          }
          var target = users.getFirst();
          EventSubClient.getInstance().subscribe(target).thenAccept(result -> {
            if (result.status() == CreateEventSubSubscriptionResult.Status.Success) {
              context.getSource().sendFeedback(
                  Text.literal(target.getDisplayNameAndLogin() + "のチャットが表示されます"));
            } else {
              context.getSource().sendFeedback(Text.literal(
                  users.getFirst().displayName() + "のチャットに接続できませんでした"));
            }
          });
        }
        default -> context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
      }
    }));
    context.getSource().sendFeedback(Text.literal(login + " に接続します"));
    return 1;
  }

  private static int authHandler(CommandContext<FabricClientCommandSource> context) {
    AuthManager.getInstance().startAuth();
    return 1;
  }

  private static SuggestionProvider<FabricClientCommandSource> getSubscribesSuggestion() {
    return (context, builder) -> CommandSource.suggestMatching(
        EventSubClient.getInstance().getSubscribeList().stream().map(TwitchUser::login), builder);
  }

  private static int listHandler(CommandContext<FabricClientCommandSource> context) {
    var subscribes = EventSubClient.getInstance().getSubscribeList();
    if (subscribes.isEmpty()) {
      var text = Text.literal("現在、接続しているチャンネルはありません");
      context.getSource().sendFeedback(text);
    } else {
      var text = Text.literal("現在、以下のチャンネルに接続しています\n");
      for (var broadcaster : subscribes) {
        text.append(Text.literal(broadcaster.getDisplayNameAndLogin())
            .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                    Text.literal(broadcaster.getChannelUrl()))).withClickEvent(
                    new ClickEvent(ClickEvent.Action.OPEN_URL, broadcaster.getChannelUrl()))
                .withColor(Formatting.LIGHT_PURPLE)));
        text.append(" ");
      }
      context.getSource().sendFeedback(text);
    }

    return 1;
  }
}
