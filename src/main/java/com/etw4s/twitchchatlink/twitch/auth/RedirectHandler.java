package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RedirectHandler implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    LOGGER.info("Redirect Server handle \"/\"");
    try (var template = getClass().getResourceAsStream(
        "/assets/twitchchatlink/template/AuthRedirectPage.html")) {
      if (template == null) {
        return;
      }
      String response = new String(template.readAllBytes());
      exchange.getResponseHeaders().set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, response.getBytes().length);
      exchange.getResponseBody().write(response.getBytes());
    }
  }
}
