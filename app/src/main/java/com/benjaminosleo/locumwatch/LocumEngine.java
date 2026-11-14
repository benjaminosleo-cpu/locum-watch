package com.benjaminosleo.locumwatch;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decides what to do with each parsed message: alarm, ignore, or log why not.
 * Pure Java; the Android side supplies the state (armed, last alarm, last seen times).
 */
public final class LocumEngine {

    public enum Outcome {
        ALARM("ALARM"),
        NOT_REQUEST("not a request"),
        SWITCHED_OFF("request, but alarm switched OFF"),
        COOLDOWN("request, in quiet hour after last alarm"),
        DUPLICATE("already seen"),
        TOO_OLD("request, but message too old"),
        OWN_MESSAGE("her own message");

        public final String label;
        Outcome(String label) { this.label = label; }
    }

    /** State the engine reads and updates. Android backs this with SharedPreferences. */
    public interface State {
        boolean isArmed();
        long lastAlarmAt(String group);
        void setLastAlarmAt(String group, long t);
        long lastSeenMessageTime(String group);
        void setLastSeenMessageTime(String group, long t);
        long cooldownMillis();
    }

    public static final long MAX_AGE_MS = 60L * 60 * 1000;     // ignore messages older than 1 h
    private static final long DEDUPE_WINDOW_MS = 3L * 60 * 60 * 1000;

    // Timed messages already judged (group|time|sender|text -> message time), kept for 6 hours.
    private final Map<String, Long> recentTimed = new java.util.HashMap<>();
    // Messages without a timestamp (title/text fallback), kept for 3 hours of clock time.
    private final Map<String, Long> recentUntimed = new LinkedHashMap<>();
    // Messages can arrive slightly out of order (sender's clock, late delivery).
    private static final long OUT_OF_ORDER_GRACE_MS = 10L * 60 * 1000;
    private static final long TIMED_WINDOW_MS = 6L * 60 * 60 * 1000;

    public synchronized Outcome evaluate(MessageParsing.Msg m, long now, State st) {
        // A timestamp well in the future is bogus; don't let it block later messages.
        long time = m.time > now + 5 * 60_000L ? now : m.time;

        // WhatsApp re-posts the recent conversation with every new message, so each
        // message is judged once: by exact key while it is recent, and anything clearly
        // older than the newest message seen (that survives a restart) is a repeat.
        String key = m.group + "|" + time + "|" + m.sender + "|" + m.text;
        if (time > 0) {
            long last = st.lastSeenMessageTime(m.group);
            if (recentTimed.containsKey(key)) return Outcome.DUPLICATE;
            if (time < last - OUT_OF_ORDER_GRACE_MS) return Outcome.DUPLICATE;
            long newest = Math.max(last, time);
            recentTimed.put(key, time);
            recentTimed.values().removeIf(t -> t < newest - TIMED_WINDOW_MS);
            st.setLastSeenMessageTime(m.group, newest);
        } else {
            prune(now);
            if (recentUntimed.containsKey(key)) return Outcome.DUPLICATE;
            recentUntimed.put(key, now);
        }

        // Per-message data (has a timestamp) with no sender = a message she sent herself.
        if (time > 0 && m.sender.isEmpty()) return Outcome.OWN_MESSAGE;
        if (!LocumFilter.isAlarm(m.sender, m.text)) return Outcome.NOT_REQUEST;
        if (time > 0 && now - time > MAX_AGE_MS) return Outcome.TOO_OLD;
        if (!st.isArmed()) return Outcome.SWITCHED_OFF;
        // The quiet hour is per group, so a test-group alarm never silences the real group.
        if (now - st.lastAlarmAt(m.group) < st.cooldownMillis()) return Outcome.COOLDOWN;
        st.setLastAlarmAt(m.group, now);
        return Outcome.ALARM;
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Long>> it = recentUntimed.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > DEDUPE_WINDOW_MS) it.remove();
        }
        while (recentUntimed.size() > 500) {
            it = recentUntimed.entrySet().iterator();
            it.next();
            it.remove();
        }
    }
}
