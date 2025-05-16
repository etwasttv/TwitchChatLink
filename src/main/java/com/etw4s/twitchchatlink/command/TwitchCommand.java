package com.etw4s.twitchchatlink.command;

import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.model.TwitchChannel;
import com.etw4s.twitchchatlink.twitch.SearchChannelsResult;
import com.etw4s.twitchchatlink.twitch.TwitchApi;
import com.etw4s.twitchchatlink.twitch.TwitchApiException;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpStatus;

public class TwitchCommand {

  private static final Style PRIMARY_STYLE = Style.EMPTY.withColor(Formatting.DARK_AQUA);
  private static final Style SECONDARY_STYLE = Style.EMPTY.withColor(Formatting.DARK_GRAY);
  private static final Style SUCCESS_STYLE = Style.EMPTY.withColor(Formatting.GREEN);
  private static final Style ERROR_STYLE = Style.EMPTY.withColor(Formatting.RED);

  private final AuthManager authManager = new AuthManager();

  public void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
    dispatcher.register(ClientCommandManager.literal("twitch")
        .then(ClientCommandManager.literal("auth").executes(c -> this.authHandler(c)))
        .then(ClientCommandManager.literal("search")
            .then(ClientCommandManager.argument("query", StringArgumentType.greedyString())
                .executes(c -> this.searchHandler(c))
                .then(ClientCommandManager.argument("cursor", StringArgumentType.string())
                    .executes(c -> this.searchHandler(c)))))
        .then(ClientCommandManager.literal("connect")
            .executes(c -> this.connectToDefault(c))
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .executes(c -> this.connectHandler(c))))
        .then(ClientCommandManager.literal("list").executes(c -> this.listHandler(c)))
        .then(ClientCommandManager.literal("disconnect")
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .suggests(getSubscribesSuggestion())
                .executes(c -> this.disconnectHandler(c))))
        .then(ClientCommandManager.literal("set-default")
            .then(ClientCommandManager.argument("login", StringArgumentType.word())
                .executes(c -> this.setDefaultHandler(c)))));
  }

  private int searchHandler(CommandContext<FabricClientCommandSource> context) {
    String query = StringArgumentType.getString(context, "query");
    String cursor = null;
    try {
      cursor = StringArgumentType.getString(context, "cursor");
    } catch (IllegalArgumentException ignored) {
    }
    var future = TwitchApi.searchChannels(query, false, 8, cursor);
    future.whenComplete(((result, throwable) -> {
      if (throwable != null) {
        if (throwable.getCause() instanceof TwitchApiException e) {
          handleTwitchApiException(context, e);
        } else {
          context.getSource().sendFeedback(
              Text.literal("検索できませんでした。")
                  .setStyle(ERROR_STYLE));
        }
        return;
      }

      MutableText resultText = getSearchResultText(query, result);
      context.getSource().sendFeedback(resultText);
    }));
    return 1;
  }

  private MutableText getSearchResultText(String query, SearchChannelsResult result) {
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
      connect.append(Text.literal("接続")
          .setStyle(SUCCESS_STYLE
              .withBold(true)));
      connect.append(Text.literal("]"));
      connect.setStyle(Style.EMPTY
          .withHoverEvent(
              new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                  Text.empty()
                      .append(Text.literal("クリックして"))
                      .append(Text.literal(data.displayName())
                          .setStyle(PRIMARY_STYLE.withItalic(true)))
                      .append(Text.literal("のチャットに接続する"))))
          .withClickEvent(
              new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/twitch connect " + data.login())));
      response.append(connect);
      response.append(" ");
      var channelInfo = Text.empty();
      channelInfo.append(
          Text.literal(data.displayName()).setStyle(PRIMARY_STYLE));
      channelInfo.append(Text.literal("(").setStyle(SECONDARY_STYLE));
      channelInfo.append(Text.literal(data.login())
          .setStyle(PRIMARY_STYLE.withItalic(true)));
      channelInfo.append(Text.literal(")").setStyle(SECONDARY_STYLE));
      channelInfo.setStyle(Style.EMPTY
          .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
              Text.empty()
                  .append(Text.literal("クリックで"))
                  .append(Text.literal("チャンネル"))
                  .append(Text.literal("を開く"))))
          .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, data.getUrl())));
      response.append(channelInfo);

      if (itr.hasNext()) {
        response.append("\n");
      }
    }
    if (result.cursor() != null) {
      response.append("\n");
      response.append("[");
      response.append(Text.literal("次のページ").setStyle(
          Style.EMPTY
              .withClickEvent(
                  new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                      "/twitch search " + query + " " + StringArgumentType.escapeIfRequired(
                          result.cursor())))
              .withUnderline(true)
              .withBold(true)
              .withItalic(true)
              .withColor(Formatting.GRAY)));
      response.append("]");
    }
    return response;
  }

  private int connectToDefault(CommandContext<FabricClientCommandSource> context) {
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
    String login = config.getDefaultLogin();
    if (login == null || login.isEmpty()) {
      context.getSource().sendFeedback(Text.literal("デフォルトの接続先が設定されていません"));
      return 0;
    }

    CompletableFuture.supplyAsync(() -> TwitchApi.getUsersByLogin(new String[] { login }))
        .whenComplete(((getUsersResult, throwable) -> {
          if (throwable != null) {
            if (throwable.getCause() instanceof TwitchApiException e) {
              handleTwitchApiException(context, e);
            } else {
              context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
            }
            return;
          }
          var users = getUsersResult.channels();
          if (users.isEmpty()) {
            context.getSource().sendFeedback(Text.literal(login + "は見つかりませんでした"));
            config.setDefaultLogin("");
            return;
          }
          var target = users.getFirst();
          connect(context, target);
        }));
    return 1;
  }

  private int setDefaultHandler(CommandContext<FabricClientCommandSource> context) {
    TwitchChatLinkConfig config = new TwitchChatLinkConfig();
    String login = StringArgumentType.getString(context, "login");

    CompletableFuture.supplyAsync(() -> TwitchApi.getUsersByLogin(new String[] { login }))
        .whenComplete(((getUsersResult, throwable) -> {
          if (throwable != null) {
            if (throwable.getCause() instanceof TwitchApiException e) {
              handleTwitchApiException(context, e);
            } else {
              context.getSource().sendFeedback(Text.literal("エラーが発生しました"));
            }
            return;
          }
          var users = getUsersResult.channels();
          if (users.isEmpty()) {
            context.getSource().sendFeedback(Text.literal(login + "は見つかりませんでした"));
            return;
          }
          var target = users.getFirst();
          config.setDefaultLogin(target.login());
          var text = Text.empty();
          text.append(getClickableChannelText(target));
          text.append(Text.literal("をデフォルトの接続先に設定しました")
              .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
          context.getSource().sendFeedback(text);
        }));
    return 1;
  }

  private int disconnectHandler(CommandContext<FabricClientCommandSource> context) {
    String login = StringArgumentType.getString(context, "login");
    var response = Text.empty();
    response.append(
        Text.literal(login).setStyle(PRIMARY_STYLE.withItalic(true)));
    response.append(
        Text.literal("から切断します").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    context.getSource().sendFeedback(response);
    EventSubClient.getInstance().unsubscribe(login).whenComplete((result, throwable) -> {
      if (throwable != null) {
        if (throwable.getCause() instanceof TwitchApiException e) {
          handleTwitchApiException(context, e);
        } else {
          context.getSource().sendFeedback(
              Text.literal("エラーが発生しました").setStyle(ERROR_STYLE));
        }
        return;
      }
      var text = Text.empty();
      text.append(
          Text.literal(login)
              .setStyle(PRIMARY_STYLE.withItalic(true)));
      text.append(
          Text.literal("から切断しました").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
      context.getSource().sendFeedback(text);
    });

    return 1;
  }

  private int connectHandler(CommandContext<FabricClientCommandSource> context) {
    String login = StringArgumentType.getString(context, "login");
    CompletableFuture.supplyAsync(() -> TwitchApi.getUsersByLogin(new String[] { login }))
        .whenComplete(((getUsersResult, throwable) -> {
          if (throwable != null) {
            if (throwable.getCause() instanceof TwitchApiException e) {
              handleTwitchApiException(context, e);
            } else {
              context.getSource().sendFeedback(
                  Text.literal("接続できませんでした。")
                      .setStyle(ERROR_STYLE));
            }
            return;
          }
          var users = getUsersResult.channels();
          if (users.isEmpty()) {
            var response = Text.empty();
            response.append(Text.literal(login).setStyle(PRIMARY_STYLE));
            response.append(Text.literal("は見つかりませんでした")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
            context.getSource().sendFeedback(response);
            return;
          }
          var target = users.getFirst();
          this.connect(context, target);
        }));
    var response = Text.empty();
    response.append(
        Text.literal(login).setStyle(PRIMARY_STYLE.withItalic(true)));
    response.append(
        Text.literal("に接続します").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
    context.getSource().sendFeedback(response);
    return 1;
  }

  private void connect(CommandContext<FabricClientCommandSource> context,
      TwitchChannel target) {
    EventSubClient.getInstance().subscribe(target).whenComplete((result, throwable) -> {
      if (throwable != null) {
        if (throwable.getCause() instanceof TwitchApiException e) {
          handleTwitchApiException(context, e);
        } else {
          context.getSource().sendFeedback(
              Text.literal("接続できませんでした。")
                  .setStyle(ERROR_STYLE));
        }
        return;
      }
      var text = Text.empty();
      text.append(getClickableChannelText(target));
      text.append(Text.literal("のチャットが表示されます")
          .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
      context.getSource().sendFeedback(text);
    });
  }

  private void handleTwitchApiException(CommandContext<FabricClientCommandSource> context,
      TwitchApiException e) {
    switch (e.getStatus()) {
      case HttpStatus.SC_UNAUTHORIZED -> {
        context.getSource().sendFeedback(Text.literal("認証に失敗しました")
            .setStyle(ERROR_STYLE));
        authManager.startAuth();
      }
      case HttpStatus.SC_BAD_REQUEST ->
        context.getSource().sendFeedback(Text.literal("リクエストが不正です")
            .setStyle(ERROR_STYLE));
      default -> context.getSource().sendFeedback(Text.literal("エラーが発生しました")
          .setStyle(ERROR_STYLE));
    }
  }

  private int authHandler(CommandContext<FabricClientCommandSource> context) {
    authManager.startAuth();
    return 1;
  }

  private SuggestionProvider<FabricClientCommandSource> getSubscribesSuggestion() {
    return (context, builder) -> CommandSource.suggestMatching(
        EventSubClient.getInstance().getSubscribeList().stream().map(TwitchChannel::login),
        builder);
  }

  private int listHandler(CommandContext<FabricClientCommandSource> context) {
    var subscribes = EventSubClient.getInstance().getSubscribeList();
    if (subscribes.isEmpty()) {
      var text = Text.literal("現在、接続しているチャンネルはありません")
          .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
      context.getSource().sendFeedback(text);
    } else {
      var text = Text.empty();
      text.append(Text.literal("現在、以下のチャンネルに接続しています\n")
          .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
      var itr = subscribes.iterator();
      while (itr.hasNext()) {
        var channel = itr.next();
        var disconnect = Text.empty();
        disconnect.append(Text.literal("["));
        disconnect.append(Text.literal("切断")
            .setStyle(Style.EMPTY
                .withColor(Formatting.RED)
                .withBold(true)));
        disconnect.append(Text.literal("]"));
        disconnect.setStyle(Style.EMPTY
            .withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.empty()
                        .append(Text.literal("クリックして"))
                        .append(Text.literal(channel.displayName())
                            .setStyle(PRIMARY_STYLE.withItalic(true)))
                        .append(Text.literal("のチャットから切断する"))))
            .withClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/twitch disconnect " + channel.login())));
        text.append(disconnect);
        text.append(Text.literal(" "));
        text.append(getClickableChannelText(channel));
        if (itr.hasNext()) {
          text.append("\n");
        }
      }
      context.getSource().sendFeedback(text);
    }

    return 1;
  }

  private Text getClickableChannelText(TwitchChannel channel) {
    var channelInfo = Text.empty();
    channelInfo.append(
        Text.literal(channel.displayName()).setStyle(PRIMARY_STYLE));
    channelInfo.append(Text.literal("(").setStyle(SECONDARY_STYLE));
    channelInfo.append(Text.literal(channel.login())
        .setStyle(PRIMARY_STYLE.withItalic(true)));
    channelInfo.append(Text.literal(")").setStyle(SECONDARY_STYLE));
    channelInfo.setStyle(Style.EMPTY
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            Text.empty()
                .append(Text.literal("クリックで"))
                .append(
                    Text.literal(channel.getUrl())
                        .setStyle(Style.EMPTY
                            .withColor(Formatting.BLUE)))
                .append(Text.literal("を開く"))))
        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, channel.getUrl())));
    return channelInfo;
  }
}
