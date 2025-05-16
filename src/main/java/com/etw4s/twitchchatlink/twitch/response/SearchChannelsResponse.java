package com.etw4s.twitchchatlink.twitch.response;

public class SearchChannelsResponse {

  public Data[] data;
  public Pagination pagination;

  public record Data(
      String broadcasterLogin,
      String displayName,
      String id,
      boolean isLive) {

  }

  public record Pagination(String cursor) {

  }
}
