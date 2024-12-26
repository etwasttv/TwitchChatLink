package com.etw4s.twitchchatlink.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.minecraft.util.Identifier;

public abstract class TwitchEmote {
  private final String id;
  private final String name;

  protected TwitchEmote(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public abstract Identifier getIdentifier();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TwitchEmote that = (TwitchEmote) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  public abstract void animate(long delta);

  public static String parseIdentifierPath(String path) {
    return URLEncoder.encode(path, StandardCharsets.UTF_8).toLowerCase().replace("/", "//")
        .replace("%", "/");
  }
}
