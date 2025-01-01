package com.etw4s.twitchchatlink.twitch;

class SearchChannelsResponse {

  Data[] data;
  Pagination pagination;

  record Data(
      String broadcasterLogin,
      String displayName,
      String id,
      boolean isLive
  ) {

  }

  record Pagination(String cursor) {

  }
}
