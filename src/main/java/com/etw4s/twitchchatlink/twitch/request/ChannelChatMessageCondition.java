package com.etw4s.twitchchatlink.twitch.request;

public class ChannelChatMessageCondition extends BaseCondition {
  public String broadcasterUserId;
  public String userId;

  public ChannelChatMessageCondition(String broadcasterUserId, String userId) {
    this.broadcasterUserId = broadcasterUserId;
    this.userId = userId;
  }
}
