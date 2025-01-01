package com.etw4s.twitchchatlink.twitch;

import org.apache.http.HttpStatus;

public class TwitchApiException extends RuntimeException {
  private final int status;

  TwitchApiException(int status) {
    super();
    this.status = status;
  }

  TwitchApiException(String message, int status) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
