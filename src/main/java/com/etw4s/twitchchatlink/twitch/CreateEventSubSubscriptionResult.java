package com.etw4s.twitchchatlink.twitch;

public record CreateEventSubSubscriptionResult(Status status, String subscriptionId, String type) {

  public static CreateEventSubSubscriptionResult success(String subscriptionId, String type) {
    return new CreateEventSubSubscriptionResult(Status.Success, subscriptionId, type);
  }

  public static CreateEventSubSubscriptionResult fail() {
    return new CreateEventSubSubscriptionResult(Status.Fail, null, null);
  }

  public enum Status {
    Success,
    Fail
  }
}
