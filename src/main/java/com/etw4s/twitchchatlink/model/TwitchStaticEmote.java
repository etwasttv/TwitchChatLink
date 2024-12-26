package com.etw4s.twitchchatlink.model;

import net.minecraft.util.Identifier;

public class TwitchStaticEmote extends TwitchEmote {

  public TwitchStaticEmote(String id, String name) {
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
