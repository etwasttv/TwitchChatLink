package com.etw4s.twitchchatlink.model;

import java.util.List;

public class TwitchChat {

  TwitchUser broadcaster;
  TwitchUser chatter;
  String text;
  List<ChatFragment> fragments;
  String color;

  public TwitchChat(TwitchUser broadcaster, TwitchUser chatter, String text, List<ChatFragment> fragments, String color) {
    this.broadcaster = broadcaster;
    this.chatter = chatter;
    this.text = text;
    this.fragments = fragments;
    this.color = color == null ? "#a970ff" : color;
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

  public String getColor() {
    return color;
  }
}
