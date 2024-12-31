package com.etw4s.twitchchatlink.twitch;

public record CreateEventSubSubscriptionResult(Status status, String subscriptionId, String type) {

  public static CreateEventSubSubscriptionResult success(String subscriptionId, String type) {
    return new CreateEventSubSubscriptionResult(Status.Success, subscriptionId, type);
  }

  public static CreateEventSubSubscriptionResult badRequest() {
    return new CreateEventSubSubscriptionResult(Status.BadRequest, null, null);
  }

  public static CreateEventSubSubscriptionResult unauthorized() {
    return new CreateEventSubSubscriptionResult(Status.Unauthorized, null, null);
  }

  public static CreateEventSubSubscriptionResult forbidden() {
    return new CreateEventSubSubscriptionResult(Status.Forbidden, null, null);
  }

  public static CreateEventSubSubscriptionResult conflict() {
    return new CreateEventSubSubscriptionResult(Status.Conflict, null, null);
  }

  public static CreateEventSubSubscriptionResult tooManyRequests() {
    return new CreateEventSubSubscriptionResult(Status.TooManyRequests, null, null);
  }

  public enum Status {
    Success,
    BadRequest,
    Unauthorized,
    Forbidden,
    Conflict,
    TooManyRequests,
  }
}
