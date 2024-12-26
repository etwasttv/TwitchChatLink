package com.etw4s.twitchchatlink.model;

import java.util.List;

public class TwitchChat {

  TwitchUser broadcaster;
  TwitchUser chatter;
  String text;
  List<ChatFragment> fragments;

  public TwitchChat(TwitchUser broadcaster, TwitchUser chatter, String text, List<ChatFragment> fragments) {
    this.broadcaster = broadcaster;
    this.chatter = chatter;
    this.text = text;
    this.fragments = fragments;
  }

  public TwitchUser getBroadcaster() {
    return broadcaster;
  }

  public TwitchUser getChatter() {
    return chatter;
  }

  public String getText() {
    return text;
  }

  public List<ChatFragment> getFragments() {
    return fragments;
  }
}
