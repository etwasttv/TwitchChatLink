package com.etw4s.twitchchatlink.emote;

public class UnicodeProvider {
    private static final int MIN_UNICODE = 0xE000;
    private static final int MAX_UNICODE = 0xF8FF;
    private int offset = 0;

    public synchronized String getNextUnicode() {
        String unicode = new String(Character.toChars(MIN_UNICODE + offset++));
        if (MIN_UNICODE + offset > MAX_UNICODE) {
            offset = 0;
        }
        return unicode;
    }
}
