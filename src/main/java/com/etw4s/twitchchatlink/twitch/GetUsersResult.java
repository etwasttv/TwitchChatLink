package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.model.TwitchUser;
import java.util.List;

public record GetUsersResult(Status status, List<TwitchUser> users) {

  public static GetUsersResult success(List<TwitchUser> users) {
    return new GetUsersResult(Status.Success, users);
  }

  public static GetUsersResult unauthorized() {
    return new GetUsersResult(Status.Unauthorized, null);
  }

  public static GetUsersResult badRequest() {
    return new GetUsersResult(Status.BadRequest, null);
  }

  public enum Status {
    Success,
    BadRequest,
    Unauthorized
  }
}
