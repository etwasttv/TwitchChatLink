package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TokenHandler implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    LOGGER.info("Redirect Server handle \"/token\"");
    Map<String, String> params = loadParams(exchange);
    String token = params.get("access_token");

    String message = null;
    try {
      message = AuthManager.getInstance().saveToken(token).get()
          ? "認証成功<br>閉じてMinecraftに戻れます"
          : "認証失敗<br>閉じてMinecraftに戻れます";
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

    try (var template = getClass().getResourceAsStream(
        "/assets/twitchchatlink/template/TokenPage.html")) {
      if (template == null) {
        return;
      }
      String response = new String(template.readAllBytes());

      response = response.replace("${message}", message);

      exchange.getResponseHeaders().set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, response.getBytes().length);
      exchange.getResponseBody().write(response.getBytes());
    }
  }

  private Map<String, String> loadParams(HttpExchange exchange) {
    String query = exchange.getRequestURI().getQuery();
    HashMap<String, String> params = new HashMap<>();
    if (query != null) {
      for (String param : query.split("&")) {
        String[] splits = param.split("=");
        params.put(splits[0], splits[1]);
      }
    }
    return params;
  }
}
