package com.benjaminosleo.locumwatch;

import java.util.HashMap;
import java.util.Map;

final class MemoryState implements LocumEngine.State {
    boolean armed = true;
    final Map<String, Long> lastAlarm = new HashMap<>();
    long cooldown = 60L * 60 * 1000;
    final Map<String, Long> seen = new HashMap<>();
    public boolean isArmed() { return armed; }
    public long lastAlarmAt(String g) { return lastAlarm.getOrDefault(g, 0L); }
    public void setLastAlarmAt(String g, long t) { lastAlarm.put(g, t); }
    public long lastSeenMessageTime(String g) { return seen.getOrDefault(g, 0L); }
    public void setLastSeenMessageTime(String g, long t) { seen.put(g, t); }
    public long cooldownMillis() { return cooldown; }
}
