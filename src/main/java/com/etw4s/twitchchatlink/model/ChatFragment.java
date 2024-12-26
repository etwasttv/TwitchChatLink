package com.etw4s.twitchchatlink.model;

public class ChatFragment {

  ChatFragmentType type;
  String text;
  String emoteId;
  String emoteSetId;

  public ChatFragment(ChatFragmentType type, String text, String emoteId, String emoteSetId) {
    this.type = type;
    this.text = text;
    this.emoteId = emoteId;
    this.emoteSetId = emoteSetId;
  }

  public ChatFragment(String text, String emoteId, String emoteSetId) {
    this(ChatFragmentType.Emote, text, emoteId, emoteSetId);
  }

  public ChatFragment(String text) {
    this(ChatFragmentType.Text, text, null, null);
  }

  public String getText() {
    return text;
  }

  public String getEmoteSetId() {
    return emoteSetId;
  }

  public ChatFragmentType getType() {
    return type;
  }

  public String getEmoteId() {
    return emoteId;
  }

  public enum ChatFragmentType {
    Emote,
    Text
  }
}
