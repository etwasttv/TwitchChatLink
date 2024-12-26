package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.apache.http.HttpStatus;

public class TokenValidator {

  private static final HttpClient client = HttpClient.newHttpClient();
  private static final Gson gson = TwitchChatLinkGson.getGson();

  public static CompletableFuture<Result> validate(String token) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://id.twitch.tv/oauth2/validate"))
        .header("Authorization", "OAuth " + token)
        .GET()
        .build();

    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(response -> {
          if (response.statusCode() == HttpStatus.SC_OK) {
            ValidateResponse validateResponse = gson.fromJson(response.body(),
                ValidateResponse.class);
            return new Result(true, validateResponse.userId, validateResponse.login);
          }
          return new Result(false, null, null);
        });
  }

  private static class ValidateResponse {

    String clientId;
    String login;
    String userId;
  }

  public record Result(boolean isValidated, String userId, String login) {

  }
}
