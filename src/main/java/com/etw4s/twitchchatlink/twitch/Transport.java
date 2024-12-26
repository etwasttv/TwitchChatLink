package com.etw4s.twitchchatlink.twitch;

class Transport {
  String method;
  String sessionId;

  Transport(String method, String sessionId) {
    this.method = method;
    this.sessionId = sessionId;
  }
}
