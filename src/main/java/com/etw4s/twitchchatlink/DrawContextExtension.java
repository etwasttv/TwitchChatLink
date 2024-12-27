package com.etw4s.twitchchatlink;

import net.minecraft.util.Identifier;

public interface DrawContextExtension {
  void twitchChatLink$drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color);
}
