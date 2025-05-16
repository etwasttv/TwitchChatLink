package com.etw4s.twitchchatlink.twitch;

public class TwitchApiException extends RuntimeException {
  private final int status;

  TwitchApiException(int status) {
    super();
    this.status = status;
  }

  TwitchApiException(String message) {
    super(message);
    this.status = 0;
  }

  TwitchApiException(String message, int status) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
