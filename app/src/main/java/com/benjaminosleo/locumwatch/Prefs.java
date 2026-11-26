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
    @Override public long lastAlarmAt(String g) { return sp.getLong("lastAlarm_" + g, 0); }
    @Override public void setLastAlarmAt(String g, long t) {
        sp.edit().putLong("lastAlarm_" + g, t).putLong("lastAlarm", t).apply();
    }
    /** Most recent alarm in any group (for the status report). */
    public long lastAlarmAt() { return sp.getLong("lastAlarm", 0); }
    @Override public long lastSeenMessageTime(String g) { return sp.getLong("seen_" + g, 0); }
    @Override public void setLastSeenMessageTime(String g, long t) { sp.edit().putLong("seen_" + g, t).apply(); }
    @Override public long cooldownMillis() { return cooldownMinutes() * 60_000L; }

    // ---------- settings ----------
    public int cooldownMinutes() { return sp.getInt("cooldownMin", 60); }
    public void setCooldownMinutes(int m) { sp.edit().putInt("cooldownMin", Math.max(0, m)).apply(); }
    /** Alarm loudness as a percentage of the phone's maximum alarm volume (10-100). */
    public int alarmVolumePercent() { return sp.getInt("alarmVol", 100); }
    public void setAlarmVolumePercent(int v) { sp.edit().putInt("alarmVol", Math.max(10, Math.min(100, v))).apply(); }

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

    /** Last WhatsApp notification of any kind (any chat): proves the listener is alive. */
    public long lastAnyNotifAt() { return sp.getLong("lastAnyAt", 0); }
    public String lastAnyNotif() { return sp.getString("lastAny", ""); }
    public void setLastAnyNotif(long at, String chat) {
        sp.edit().putLong("lastAnyAt", at).putString("lastAny", chat).apply();
    }

    public void listen(SharedPreferences.OnSharedPreferenceChangeListener l) { sp.registerOnSharedPreferenceChangeListener(l); }
    public void unlisten(SharedPreferences.OnSharedPreferenceChangeListener l) { sp.unregisterOnSharedPreferenceChangeListener(l); }

    public void addLog(String line) { push("log", "dd/MM HH:mm", line, MAX_LOG_LINES); }

    /** Raw record of every watched-group notification, for troubleshooting. */
    public void addDiag(String line) { push("diag", "dd/MM HH:mm:ss", line, 40); }
    public String diag() { return sp.getString("diag", ""); }

    private void push(String key, String fmt, String line, int max) {
        synchronized (Prefs.class) { // several Prefs objects and threads share one file
            String stamp = new SimpleDateFormat(fmt, Locale.UK).format(new Date());
            String[] lines = (stamp + "  " + line + "\n" + sp.getString(key, "")).split("\n");
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < Math.min(lines.length, max); i++) b.append(lines[i]).append('\n');
            sp.edit().putString(key, b.toString()).commit();
        }
    }

    public long lastKarenAt() { return sp.getLong("lastKarenAt", 0); }
    public void setLastKarenAt(long t) { sp.edit().putLong("lastKarenAt", t).apply(); }

    /** Phone clock minus ntfy server clock, in seconds (0 = unknown or exact). */
    public long clockSkewSeconds() { return sp.getLong("clockSkew", 0); }
    public void setClockSkewSeconds(long s) { if (s != clockSkewSeconds()) sp.edit().putLong("clockSkew", s).apply(); }

    public String log() { return sp.getString("log", ""); }
}
