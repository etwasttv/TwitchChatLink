package com.etw4s.twitchchatlink.twitch;

class ChannelChatMessageCondition extends BaseCondition {
  String broadcasterUserId;
  String userId;

  ChannelChatMessageCondition(String broadcasterUserId, String userId) {
    this.broadcasterUserId = broadcasterUserId;
    this.userId = userId;
  }
}
