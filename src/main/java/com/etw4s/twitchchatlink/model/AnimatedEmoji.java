package com.etw4s.twitchchatlink.model;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Identifier;

public class AnimatedEmoji extends BaseEmoji {

  private final int totalFrames;
  private final AtomicInteger frame = new AtomicInteger(0);
  private int totalDelay;

  public AnimatedEmoji(String id, String name, int totalFrames) {
    super(id, name);
    this.totalFrames = totalFrames;
  }

  public void setTotalDelay(int totalDelay) {
    this.totalDelay = totalDelay;
  }

  @Override
  public Identifier getIdentifier() {
    return getFrameIdentifier((int) (totalFrames * frame.get() / (double) totalDelay));
  }

  public Identifier getFrameIdentifier(int frame) {
    return Identifier.of("twitchchatlink", parseIdentifierPath(getName() + "-" + frame));
  }

  @Override
  public void animate(long delta) {
    frame.getAndAdd((int) delta);
    while (frame.get() >= totalDelay) {
      frame.getAndAdd(-totalDelay);
    }
  }
}
