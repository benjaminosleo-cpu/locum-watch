package com.benjaminosleo.locumwatch;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** All settings and state, stored in SharedPreferences. Also the engine's State. */
public final class Prefs implements LocumEngine.State {
    public static final String DEFAULT_TOPIC = "locum-zfclqv1g0owefr61";
    public static final String DEFAULT_GROUPS = "ENT Locums Group\nLocum Alarm Test";
    private static final int MAX_LOG_LINES = 60;

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("locumwatch", Context.MODE_PRIVATE);
    }

    // ---------- on/off ----------
    @Override public boolean isArmed() { return sp.getBoolean("armed", true); }
    public void setArmed(boolean on) { sp.edit().putBoolean("armed", on).apply(); }

    // ---------- engine state ----------
    @Override public long lastAlarmAt() { return sp.getLong("lastAlarm", 0); }
    @Override public void setLastAlarmAt(long t) { sp.edit().putLong("lastAlarm", t).apply(); }
    @Override public long lastSeenMessageTime(String g) { return sp.getLong("seen_" + g, 0); }
    @Override public void setLastSeenMessageTime(String g, long t) { sp.edit().putLong("seen_" + g, t).apply(); }
    @Override public long cooldownMillis() { return cooldownMinutes() * 60_000L; }

    // ---------- settings ----------
    public int cooldownMinutes() { return sp.getInt("cooldownMin", 60); }
    public void setCooldownMinutes(int m) { sp.edit().putInt("cooldownMin", Math.max(0, m)).apply(); }
    public int alarmMinutes() { return sp.getInt("alarmMin", 10); }
    public void setAlarmMinutes(int m) { sp.edit().putInt("alarmMin", Math.max(1, Math.min(60, m))).apply(); }

    public String groupsRaw() { return sp.getString("groups", DEFAULT_GROUPS); }
    public void setGroupsRaw(String g) { sp.edit().putString("groups", g).apply(); }
    public List<String> groups() {
        List<String> out = new ArrayList<>();
        for (String line : groupsRaw().split("\n")) if (!line.trim().isEmpty()) out.add(line.trim());
        return out;
    }

    public String topic() { return sp.getString("topic", DEFAULT_TOPIC).trim(); }
    public void setTopic(String t) { sp.edit().putString("topic", t.trim()).remove("ntfyLastId").apply(); }
    public String replyTopic() { return topic().isEmpty() ? "" : topic() + "-reply"; }

    public String pushoverToken() { return sp.getString("poToken", "").trim(); }
    public String pushoverUser() { return sp.getString("poUser", "").trim(); }
    public void setPushover(String token, String user) {
        sp.edit().putString("poToken", token.trim()).putString("poUser", user.trim()).apply();
    }
    public boolean hasPushover() { return !pushoverToken().isEmpty() && !pushoverUser().isEmpty(); }

    // ---------- bookkeeping ----------
    public String ntfyLastId() { return sp.getString("ntfyLastId", ""); }
    public void setNtfyLastId(String id) { sp.edit().putString("ntfyLastId", id).apply(); }

    public long lastWhatsAppAt() { return sp.getLong("lastWaAt", 0); }
    public String lastWhatsApp() { return sp.getString("lastWa", ""); }
    public void setLastWhatsApp(long at, String summary) {
        sp.edit().putLong("lastWaAt", at).putString("lastWa", summary).apply();
    }

    public synchronized void addLog(String line) {
        String stamp = new SimpleDateFormat("dd/MM HH:mm", Locale.UK).format(new Date());
        String all = stamp + "  " + line + "\n" + sp.getString("log", "");
        String[] lines = all.split("\n");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, MAX_LOG_LINES); i++) b.append(lines[i]).append('\n');
        sp.edit().putString("log", b.toString()).apply();
    }
    public String log() { return sp.getString("log", ""); }
}
