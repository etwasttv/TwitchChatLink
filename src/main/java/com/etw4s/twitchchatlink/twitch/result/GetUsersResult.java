package com.etw4s.twitchchatlink.twitch.result;

import com.etw4s.twitchchatlink.model.TwitchChannel;
import java.util.List;

public record GetUsersResult(List<TwitchChannel> channels) {

}
