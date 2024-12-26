package com.etw4s.twitchchatlink.twitch;

class GetEmoteSetResponse {

  Data[] data;
  String template;

  static class Data {
    String id;
    String name;
    String[] format;
    String[] scale;
    String[] theme_mode;
  }
}
