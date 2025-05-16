package com.etw4s.twitchchatlink.twitch.response;

public class GetUsersResponse {

  public User[] data;

  public static class User {
    public String id;
    public String login;
    public String displayName;
  }
}
