package com.etw4s.twitchchatlink.emote;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etw4s.twitchchatlink.TwitchChatLink;

public class UnicodeMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchChatLink.MOD_NAME);
    private static final int MIN_UNICODE = 0xE000;
    private static final int MAX_UNICODE = 0xF8FF;
    private int offset = 0;

    private static final Map<String, String> unicodeMap = Collections.synchronizedMap(new HashMap<>());

    public synchronized String getNextUnicode() {
        String unicode = new String(Character.toChars(MIN_UNICODE + offset++));
        if (MIN_UNICODE + offset > MAX_UNICODE) {
            offset = 0;
        }
        return unicode;
    }

    public Set<Map.Entry<String, String>> detectUnusedUnicode(Set<String> unicode) {
        var unusedUnicodes = unicodeMap.entrySet().stream()
                .filter(entry -> !unicode.contains(entry.getKey())).collect(Collectors.toSet());
        unusedUnicodes.forEach(e -> unicodeMap.remove(e.getKey()));
        LOGGER.debug("{} Unicode are not used", unusedUnicodes.size());
        return unusedUnicodes;
    }

    public synchronized String getUnicode(String name) {
        var unicodeOptional = unicodeMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(name)).findFirst();

        if (unicodeOptional.isPresent()) {
            return unicodeOptional.get().getKey();
        }

        var unicode = getNextUnicode();
        unicodeMap.put(unicode, name);
        return unicode;
    }

    public String getNameByUnicode(String unicode) {
        return unicodeMap.get(unicode);
    }

    public boolean IsUsedUnicode(String unicode) {
        return unicodeMap.get(unicode) != null;
    }
}
