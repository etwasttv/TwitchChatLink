package com.etw4s.twitchchatlink.twitch.auth;

import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.http.HttpStatus;

public class TokenValidator {

  private final HttpClient client = HttpClient.newHttpClient();
  private final Gson gson = TwitchChatLinkGson.getGson();

  public ValidationResult validate(String token) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://id.twitch.tv/oauth2/validate"))
        .header("Authorization", "OAuth " + token)
        .GET()
        .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == HttpStatus.SC_OK) {
        ValidateResponse validateResponse = gson.fromJson(response.body(),
            ValidateResponse.class);
        return new ValidationResult(true, validateResponse.userId, validateResponse.login);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new ValidationResult(false, null, null);
  }

  private static class ValidateResponse {
    String login;
    String userId;
  }

  public record ValidationResult(boolean isValidated, String userId, String login) {

  }
}
