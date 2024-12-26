package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.util.TwitchChatLinkGson;
import com.google.gson.Gson;
import java.net.http.HttpClient;

public class TwitchApi {

  private static final HttpClient client = HttpClient.newBuilder().build();
  private static final Gson gson = TwitchChatLinkGson.getGson();
}
