package com.etw4s.twitchchatlink.mixin;

import com.etw4s.twitchchatlink.DrawContextExtension;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin implements DrawContextExtension {

  @Shadow
  abstract void drawTexturedQuad(Identifier texture, int x1, int x2, int y1, int y2, int z,
      float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha);

  @Shadow public abstract void drawTexture(Identifier texture, int x, int y, int u, int v,
      int width, int height);

  @Override
  public void twitchChatLink$drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v,
      int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
    this.drawTexture(texture, x, x + width, y, y + height, 0, regionWidth, regionHeight, u, v, textureWidth, textureHeight, color);
  }

  @Unique
  void drawTexture(Identifier texture, int x1, int x2, int y1, int y2, int z, int regionWidth,
      int regionHeight, float u, float v, int textureWidth, int textureHeight, int color) {
    this.drawTexturedQuad(texture, x1, x2, y1, y2, z, (u + 0.0F) / (float) textureWidth,
        (u + (float) regionWidth) / (float) textureWidth, (v + 0.0F) / (float) textureHeight,
        (v + (float) regionHeight) / (float) textureHeight, ColorHelper.Argb.getRed(color)/255f,
        ColorHelper.Argb.getGreen(color)/255f, ColorHelper.Argb.getBlue(color)/255f,
        ColorHelper.Argb.getAlpha(color)/255f);
  }
}
