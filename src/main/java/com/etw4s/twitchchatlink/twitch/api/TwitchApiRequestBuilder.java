package com.etw4s.twitchchatlink.twitch.api;

import java.net.http.HttpRequest;

import com.etw4s.twitchchatlink.TwitchChatLinkConfig;
import com.etw4s.twitchchatlink.TwitchChatLinkContracts;

public class TwitchApiRequestBuilder {

    public static HttpRequest.Builder newBuilder() {
        TwitchChatLinkConfig config = new TwitchChatLinkConfig();
        return HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + config.getToken())
                .header("Client-Id", TwitchChatLinkContracts.TWITCH_CLIENT_ID);
    }
}
