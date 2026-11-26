package com.benjaminosleo.locumwatch;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.BatteryManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The on/off switch commands from her iPhone, status text and the daily check-in. */
final class Commands {
    private static final long STALE_MS = 72L * 60 * 60 * 1000;

    private Commands() {}

    static void handle(Context c, String raw) {
        String cmd = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        Prefs p = new Prefs(c);
        switch (cmd) {
            case "on":
                p.setArmed(true);
                p.addLog("Switched ON from iPhone");
                WatchService.refresh(c);
                reply(c, "Locum alarm is ON", "Locum requests will set off the bedside alarm.", false);
                break;
            case "off":
                p.setArmed(false);
                p.addLog("Switched OFF from iPhone");
                WatchService.refresh(c);
                reply(c, "Locum alarm is OFF", "You'll still get normal WhatsApp notifications.", false);
                break;
            case "status":
                // Check the feed right now, so the report is not based on an old check.
                WatchService.probeThen(c, () -> reply(c, "Locum alarm status", statusText(c), false));
                break;
            case "test":
                p.addLog("Test alarm from iPhone");
                AlarmService.start(c, "TEST", "Test alarm from the switch. Tap STOP ALARM to silence it.", true);
                break;
            default:
                // Ignore anything else (including our own replies, which use a different topic).
        }
    }

    /** Sends a message to her iPhone: ntfy always (free), Pushover too if set up. */
    static void reply(Context c, String title, String message, boolean urgent) {
        reply(c, title, message, urgent, Math.max(60, new Prefs(c).alarmMinutes() * 60));
    }

    /** urgent = ntfy max priority + Pushover emergency (repeats for expireSeconds unless acknowledged). */
    static void reply(Context c, String title, String message, boolean urgent, int expireSeconds) {
        Prefs p = new Prefs(c);
        String topic = p.replyTopic();
        boolean po = p.hasPushover();
        String token = p.pushoverToken(), user = p.pushoverUser();
        int expire = expireSeconds;
        Net.async(() -> {
            Net.ntfy(topic, title, message, urgent ? 5 : 3);
            if (po) Net.pushover(token, user, title, message, urgent ? 2 : 0, expire);
        });
    }

    /** Things that would stop the alarm working. Empty = healthy. */
    static java.util.List<String> problems(Context c) {
        Prefs p = new Prefs(c);
        java.util.List<String> out = new java.util.ArrayList<>();
        if (!WaListener.isEnabled(c)) out.add("Notification access is NOT granted");
        else if (!WaListener.isReceiving()) out.add("Notifications are not reaching the app (it will try to reconnect)");
        android.os.PowerManager pm = c.getSystemService(android.os.PowerManager.class);
        if (pm.isPowerSaveMode()) out.add("Battery Saver is ON: turn it off, it delays WhatsApp");
        if (!pm.isIgnoringBatteryOptimizations(c.getPackageName())) out.add("Battery saving is not switched off for this app");
        if (!isCharging(c)) out.add(UNPLUGGED);
        long skew = Math.abs(p.clockSkewSeconds());
        if (skew > 120) out.add("The phone's clock is off by " + (skew / 60) + " min: turn on automatic date and time");
        long any = p.lastAnyNotifAt();
        if (any > 0 && System.currentTimeMillis() - any > STALE_MS) {
            out.add("No WhatsApp notification from any chat for " + ago(any).replace(" ago", "")
                + ": check WhatsApp is still linked");
        }
        return out;
    }

    static final String UNPLUGGED =
        "Unplugged: the phone may go to sleep and delay alarms. Plug it back in.";

    static void checkIn(Context c) {
        java.util.List<String> problems = problems(c);
        String title = problems.isEmpty() ? "Locum Watch OK"
            : problems.size() == 1 && problems.get(0).equals(UNPLUGGED) ? "Locum Watch: the spare phone is unplugged"
            : "Locum Watch: please check the spare phone";
        reply(c, title, statusText(c), false);
    }

    static String statusText(Context c) {
        Prefs p = new Prefs(c);
        StringBuilder b = new StringBuilder();
        for (String problem : problems(c)) b.append("\u26A0 ").append(problem).append('\n');
        b.append("Alarm: ").append(p.isArmed() ? "ON" : "OFF")
            .append(", volume ").append(p.alarmVolumePercent()).append("%\n");
        b.append("WhatsApp feed: ").append(!WaListener.isEnabled(c) ? "no access"
            : !WaListener.isReceiving() ? "NOT live"
            : WatchService.lastCheckOkAt() == 0 ? "live"
            : "live (checked " + ago(WatchService.lastCheckOkAt()) + ")").append('\n');
        long any = p.lastAnyNotifAt();
        b.append("Last WhatsApp notification (any chat): ")
            .append(any == 0 ? "none yet" : when(any) + " (" + ago(any) + ", " + p.lastAnyNotif() + ")").append('\n');
        long wa = p.lastWhatsAppAt();
        b.append("Last group message seen: ").append(wa == 0 ? "none yet" : when(wa) + " (" + ago(wa) + ")").append('\n');
        if (!p.lastWhatsApp().isEmpty()) b.append("  ").append(p.lastWhatsApp()).append('\n');
        long k = p.lastKarenAt();
        b.append("Last message from Karen: ").append(k == 0 ? "none yet" : when(k) + " (" + ago(k) + ")").append('\n');
        long al = p.lastAlarmAt();
        b.append("Last alarm: ").append(al == 0 ? "none yet" : when(al)).append('\n');
        b.append("Battery: ").append(battery(c)).append('\n');
        b.append("Pushover: ").append(p.hasPushover() ? "on" : "not set up").append('\n');
        b.append("Version: ").append(version(c));
        return b.toString();
    }

    private static boolean isCharging(Context c) {
        Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return i == null || i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    private static String battery(Context c) {
        Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return "unknown";
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int pct = scale > 0 ? Math.round(level * 100f / scale) : level;
        return pct + "%" + (plugged != 0 ? " (charging)" : " (NOT charging)");
    }

    static String when(long t) {
        return new SimpleDateFormat("EEE d MMM HH:mm", Locale.UK).format(new Date(t));
    }

    static String ago(long t) {
        long min = (System.currentTimeMillis() - t) / 60_000;
        if (min < 60) return min + " min ago";
        if (min < 48 * 60) return (min / 60) + " h ago";
        return (min / (60 * 24)) + " days ago";
    }

    static String version(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
