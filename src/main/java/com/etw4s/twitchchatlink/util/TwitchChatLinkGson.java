package com.etw4s.twitchchatlink.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TwitchChatLinkGson {

  private static Gson gson = new GsonBuilder()
      .setFieldNamingStrategy(field -> {
        String name = field.getName();
        StringBuilder result = new StringBuilder();
        for (char c : name.toCharArray()) {
          if (Character.isUpperCase(c)) {
            result.append("_").append(Character.toLowerCase(c));
          } else {
            result.append(c);
          }
        }
        return result.toString();
      })
      .create();

  public static Gson getGson() {
    return gson;
  }
}
