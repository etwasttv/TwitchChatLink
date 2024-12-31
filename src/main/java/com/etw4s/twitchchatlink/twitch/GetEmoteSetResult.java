package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.model.TwitchEmoteInfo;
import java.util.List;

public class GetEmoteSetResult {
  List<TwitchEmoteInfo> emoteInfos;
  String emoteSetId;
  Status status;

  static GetEmoteSetResult success(String emoteSetId, List<TwitchEmoteInfo> infos) {
    var result = new GetEmoteSetResult();
    result.emoteInfos = infos;
    result.emoteSetId = emoteSetId;
    result.status = Status.Success;
    return result;
  }

  static GetEmoteSetResult badRequest(String emoteSetId) {
    var result = new GetEmoteSetResult();
    result.status = Status.BadRequest;
    result.emoteSetId = emoteSetId;
    return result;
  }

  static GetEmoteSetResult unauthorized(String emoteSetId) {
    var result = new GetEmoteSetResult();
    result.status = Status.Unauthorized;
    result.emoteSetId = emoteSetId;
    return result;
  }

  public Status getStatus() {
    return status;
  }

  public String getEmoteSetId() {
    return emoteSetId;
  }

  public List<TwitchEmoteInfo> getEmoteInfos() {
    return emoteInfos;
  }

  public enum Status {
    Success,
    BadRequest,
    Unauthorized
  }
}
