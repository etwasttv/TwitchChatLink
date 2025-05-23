package com.etw4s.twitchchatlink.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.etw4s.twitchchatlink.model.ChatFragment.ChatFragmentType;

public class TwitchChat {

  TwitchUser broadcaster;
  TwitchUser chatter;
  String text;
  List<ChatFragment> fragments;
  String color;

  public TwitchChat(TwitchUser broadcaster, TwitchUser chatter, String text, List<ChatFragment> fragments,
      String color) {
    this.broadcaster = broadcaster;
    this.chatter = chatter;
    this.text = text;
    this.fragments = fragments;
    // Todo: バリデーション
    this.color = color == null || color.length() != 7 ? "#a970ff" : color;
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

  public Set<ChatFragment> getEmotes() {
    return getFragments().stream().filter(f -> f.getType() == ChatFragmentType.Emote)
        .collect(Collectors.toSet());
  }
}
