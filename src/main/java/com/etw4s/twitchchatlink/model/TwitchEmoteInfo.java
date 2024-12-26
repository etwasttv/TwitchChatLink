package com.etw4s.twitchchatlink.model;

public record TwitchEmoteInfo(String id, String name, String[] format, String[] scale,
                              String[] theme, String template) {

  public String getUrl(String format, String scale, String theme) {
    return template.replace("{{id}}", id)
        .replace("{{format}}", format == null ? this.format[0] : format)
        .replace("{{theme_mode}}", theme == null ? this.theme[0] : theme)
        .replace("{{scale}}", scale == null ? this.scale[0] : scale);
  }
}
