package com.etw4s.twitchchatlink.client;

import com.etw4s.twitchchatlink.event.TwitchChatEvent.TwitchChatListener;
import com.etw4s.twitchchatlink.model.ChatFragment.ChatFragmentType;
import com.etw4s.twitchchatlink.model.TwitchChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class TwitchChatEventListener implements
    TwitchChatListener {

  @Override
  public void onReceive(TwitchChat chat) {
    MinecraftClient client = MinecraftClient.getInstance();

    if (client.player == null || client.world == null) {
      return;
    }

    TextColor chatterColor = TextColor.fromRgb(Integer.parseInt(chat.getColor().substring(1), 16));
    MutableText chatter = Text.literal(chat.getChatter().displayName())
        .setStyle(Style.EMPTY.withColor(chatterColor));
    MutableText separator = Text.literal(": ").setStyle(Style.EMPTY.withColor(Formatting.WHITE));
    MutableText text = Text.empty();
    for (var fragment : chat.getFragments()) {
      Style style = fragment.getType() == ChatFragmentType.Emote
          ? Style.EMPTY.withColor(Formatting.GRAY)
          : Style.EMPTY.withColor(Formatting.WHITE);
      String t = fragment.getType() == ChatFragmentType.Emote
          ? EmoteManager.getInstance().getOrMappingUnicode(fragment.getText())
          : fragment.getText();
      text.append(Text.literal(t).setStyle(style));
    }
    MutableText full = Text.empty().append(chatter).append(separator).append(text);

    client.player.sendMessage(full);
  }
}
