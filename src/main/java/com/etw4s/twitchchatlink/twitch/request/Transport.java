package com.etw4s.twitchchatlink.twitch.request;

public class Transport {
  public String method;
  String sessionId;

  public Transport(String method, String sessionId) {
    this.method = method;
    this.sessionId = sessionId;
  }
}
