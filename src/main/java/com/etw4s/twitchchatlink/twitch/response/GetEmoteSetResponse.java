package com.etw4s.twitchchatlink.twitch.response;

public class GetEmoteSetResponse {

  public Data[] data;
  public String template;

  public static class Data {
    public String id;
    public String name;
    public String[] format;
    public String[] scale;
    public String[] theme_mode;
  }
}
