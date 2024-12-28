package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.TwitchChatLink;
import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RedirectHandler implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    LOGGER.info("Redirect Server handle: {} \"/\"", method);

    if (method.equals("GET")) {
      handleGet(exchange);
    } else if (method.equals("POST")) {
      handlePost(exchange);
    }
  }

  private void handleGet(HttpExchange exchange) throws IOException {
    var query = exchange.getRequestURI().getQuery();
    if (query != null) {
      try (var template = getClass().getResourceAsStream(
          "/assets/twitchchatlink/template/Page.html")) {
        if (template == null) {
          return;
        }
        String response = new String(template.readAllBytes());
        response = response.replace("${message}", "認証に失敗しました");
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
      }
      return;
    }

    try (var template = getClass().getResourceAsStream(
        "/assets/twitchchatlink/template/AuthRedirectPage.html")) {
      if (template == null) {
        return;
      }
      String response = new String(template.readAllBytes());
      exchange.getResponseHeaders().set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, response.getBytes().length);
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes());
      os.close();
      exchange.close();
    }
  }

  private void handlePost(HttpExchange exchange) throws IOException {
    try (InputStream input = exchange.getRequestBody()) {
      Gson gson = TwitchChatLinkGson.getGson();
      var body = gson.fromJson(new InputStreamReader(input), TokenPostBody.class);

      AuthManager.getInstance().saveToken(body.accessToken)
          .whenComplete((result, ex) -> {
            try (var template = getClass().getResourceAsStream(
                "/assets/twitchchatlink/template/Page.html")) {
              if (template == null) {
                return;
              }
              String resBody = "OK";
              exchange.getResponseHeaders().set("Content-Type", "text/html");
              exchange.sendResponseHeaders(HttpStatus.SC_ACCEPTED, resBody.length());
              OutputStream os = exchange.getResponseBody();
              os.write(resBody.getBytes());
              os.close();
            } catch (IOException e) {
              try {
                var resBody = "NG";
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(400, resBody.length());
                OutputStream os = exchange.getResponseBody();
                os.write(resBody.getBytes());
                os.close();
              } catch (IOException exception) {
                exception.printStackTrace();
              }
            } finally {
              exchange.close();
            }
          });
    }
  }
}
