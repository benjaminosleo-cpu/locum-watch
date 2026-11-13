package com.benjaminosleo.locumwatch;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java helpers for turning WhatsApp notification fields into (group, sender, text). */
public final class MessageParsing {
    private MessageParsing() {}

    public static final class Msg {
        public final String group;
        public final String sender;
        public final String text;
        public final long time; // 0 when unknown

        public Msg(String group, String sender, String text, long time) {
            this.group = group;
            this.sender = sender == null ? "" : sender.trim();
            this.text = text == null ? "" : text;
            this.time = time;
        }
    }

    private static final Pattern COUNT_SUFFIX =
        Pattern.compile("\\s*\\(\\d+\\s+(new\\s+)?messages?\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENDER_AT_GROUP = Pattern.compile("^(.+?) @ (.+)$");
    private static final Pattern GROUP_COLON_SENDER = Pattern.compile("^(.+?):\\s+(.+)$");
    private static final Pattern TEXT_SENDER = Pattern.compile("^([^:\\n]{1,60}):\\s+([\\s\\S]+)$");

    public static String cleanTitle(String title) {
        if (title == null) return "";
        return COUNT_SUFFIX.matcher(title).replaceAll("").trim();
    }

    /** Returns the watched group named in this title, or null. Case-insensitive "contains". */
    public static String matchGroup(String title, List<String> groups) {
        String t = cleanTitle(title).toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return null;
        for (String g : groups) {
            String gg = g.trim();
            if (!gg.isEmpty() && t.contains(gg.toLowerCase(Locale.ROOT))) return gg;
        }
        return null;
    }

    /**
     * Fallback for notifications without per-message data: title + text only.
     * Handles "Group" + "Sender: text", "Sender @ Group" + "text", "Group: Sender" + "text".
     */
    public static Msg parseTitleText(String title, String text, List<String> groups) {
        String group = matchGroup(title, groups);
        if (group == null) return null;
        String t = cleanTitle(title);
        String body = text == null ? "" : text.trim();
        String sender = "";
        String gl = group.toLowerCase(Locale.ROOT);

        Matcher m = SENDER_AT_GROUP.matcher(t);
        if (m.find() && m.group(2).toLowerCase(Locale.ROOT).contains(gl)) sender = m.group(1);
        if (sender.isEmpty()) {
            m = GROUP_COLON_SENDER.matcher(t);
            if (m.find() && m.group(1).toLowerCase(Locale.ROOT).contains(gl)) sender = m.group(2);
        }
        if (sender.isEmpty()) {
            m = TEXT_SENDER.matcher(body);
            if (m.find()) {
                sender = m.group(1);
                body = m.group(2);
            }
        }
        if (body.isEmpty()) return null;
        return new Msg(group, sender, body, 0);
    }
}
