package com.etw4s.twitchchatlink.twitch;

public record DeleteEventSubSubscriptionResult(Status status) {

  public enum Status {
    Success,
    BadRequest,
    Unauthorized,
    NotFound
  }
}
