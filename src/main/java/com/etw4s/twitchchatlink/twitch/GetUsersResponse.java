package com.etw4s.twitchchatlink.twitch;

class GetUsersResponse {

  User[] data;

  static class User {
    String id;
    String login;
    String displayName;
  }
}
