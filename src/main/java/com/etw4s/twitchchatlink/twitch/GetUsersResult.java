package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.model.TwitchUser;
import java.util.List;

public record GetUsersResult(Status status, List<TwitchUser> users) {

  public static GetUsersResult OkResult(List<TwitchUser> users) {
    return new GetUsersResult(Status.Ok, users);
  }

  public static GetUsersResult Unauthorized() {
    return new GetUsersResult(Status.Unauthorized, null);
  }

  public static GetUsersResult ErrorResult() {
    return new GetUsersResult(Status.Error, null);
  }

  public enum Status {
    Ok,
    Unauthorized,
    Error,
  }
}
