package com.etw4s.twitchchatlink.twitch;

public record DeleteEventSubSubscriptionResult(Status status) {

  static DeleteEventSubSubscriptionResult success() {
    return new DeleteEventSubSubscriptionResult(Status.Success);
  }

  static DeleteEventSubSubscriptionResult badRequest() {
    return new DeleteEventSubSubscriptionResult(Status.BadRequest);
  }

  static DeleteEventSubSubscriptionResult unauthorized() {
    return new DeleteEventSubSubscriptionResult(Status.Unauthorized);
  }

  static DeleteEventSubSubscriptionResult notFound() {
    return new DeleteEventSubSubscriptionResult(Status.NotFound);
  }

  public enum Status {
    Success,
    BadRequest,
    Unauthorized,
    NotFound
  }
}
