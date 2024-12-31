package com.etw4s.twitchchatlink.model;

public record TwitchUser(String id, String login, String displayName) {

  public String getChannelUrl() {
    return "https://www.twitch.tv/" + login;
  }

  public String getDisplayNameAndLogin() {
    return displayName + "(" + login + ")";
  }
}