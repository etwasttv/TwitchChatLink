package com.etw4s.twitchchatlink.model;

import net.minecraft.util.Identifier;

public class StaticEmoji extends BaseEmoji {

  public StaticEmoji(String id, String name) {
    super(id, name);
  }

  @Override
  public Identifier getIdentifier() {
    return Identifier.of("twitchchatlink", parseIdentifierPath(getName()));
  }

  @Override
  public void animate(long delta) {

  }
}
