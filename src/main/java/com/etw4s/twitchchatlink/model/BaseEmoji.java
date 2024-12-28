package com.etw4s.twitchchatlink.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import net.minecraft.util.Identifier;

public abstract class BaseEmoji {

  private final String id;
  private final String name;

  protected BaseEmoji(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public abstract Set<Identifier> getAllIdentifiers();

  public abstract Identifier getIdentifier();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaseEmoji that = (BaseEmoji) o;
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
