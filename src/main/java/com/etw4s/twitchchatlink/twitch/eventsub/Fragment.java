package com.etw4s.twitchchatlink.twitch.eventsub;

class Fragment {
  String type;
  String text;
  Emote emote;

  static class Emote {
    String id;
    String emoteSetId;
  }
}
