package com.etw4s.twitchchatlink.model;

public record TwitchChannel(
    String id,
    String login,
    String displayName,
    boolean isLive
) {
  public String getUrl() {
    return "https://www.twitch.tv/" + login;
  }
}
