package com.etw4s.twitchchatlink.twitch;

import com.etw4s.twitchchatlink.model.TwitchChannel;
import java.util.List;

public record SearchChannelsResult(
    List<TwitchChannel> channels,
    String cursor
) {

}