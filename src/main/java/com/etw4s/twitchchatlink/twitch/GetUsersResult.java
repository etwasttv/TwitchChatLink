package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.model.TwitchUser;
import java.util.List;

public record GetUsersResult(List<TwitchUser> users) {

  public static GetUsersResult success(List<TwitchUser> users) {
    return new GetUsersResult(users);
  }
}
