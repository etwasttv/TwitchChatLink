package com.etw4s.twitchchatlink.model;

public record TwitchChannel(
    String id,
    String login,
    String displayName,
    LiveStatus liveStatus
) {
  public String getUrl() {
    return "https://www.twitch.tv/" + login;
  }

  public enum LiveStatus {
    Online,
    Offline,
    Unknown
  }
}
