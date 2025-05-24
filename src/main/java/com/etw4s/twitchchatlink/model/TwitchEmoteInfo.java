package com.etw4s.twitchchatlink.model;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

public record TwitchEmoteInfo(String id, String name, String[] format, String[] scale,
    String[] theme, String template) {

  public URL getUrl(String format, String scale, String theme) throws MalformedURLException {
    return URI.create(template.replace("{{id}}", id)
        .replace("{{format}}", format == null ? this.format[0] : format)
        .replace("{{theme_mode}}", theme == null ? this.theme[this.theme.length - 1] : theme)
        .replace("{{scale}}", scale == null ? this.scale[0] : scale)).toURL();
  }

  public boolean isAnimated() {
    return Arrays.asList(format()).contains("animated");
  }
}
