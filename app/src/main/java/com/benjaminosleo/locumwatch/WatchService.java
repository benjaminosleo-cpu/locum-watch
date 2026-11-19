package com.benjaminosleo.locumwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Always-on foreground service. It keeps the app alive, listens to the private ntfy
 * topic for "on", "off", "status" and "test" from her iPhone shortcuts, and runs a
 * heartbeat that proves the WhatsApp notification feed is still being delivered.
 */
public class WatchService extends Service {
    private static final String TAG = "LocumWatch";
    static final String CH_STATUS = "status";
    private static final int NOTIF_ID = 1;
    private static final long COMMAND_MAX_AGE_S = 10 * 60; // ignore commands older than 10 min
    private static final long BEAT_MS = 5 * 60_000L;

    private static volatile WatchService instance;

    // Result of the last feed check that actually RAN. While the phone is asleep (unplugged
    // and idle) checks don't run at all; that must not be reported as a failure.
    private static volatile boolean feedFailing;
    private static volatile long lastCheckOkAt; // wall clock, 0 = never

    static boolean feedFailing() { return feedFailing; }
    static long lastCheckOkAt() { return lastCheckOkAt; }
    static void markFeedOk() { feedFailing = false; lastCheckOkAt = System.currentTimeMillis(); }

    /**
     * Run a feed check right now, then run {@code after} (on the main thread) once the
     * result is known, so status reports and the daily check-in are based on a fresh check.
     */
    static void probeThen(Context c, Runnable after) { probeThen(c.getApplicationContext(), after, true); }

    private static void probeThen(Context c, Runnable after, boolean mayRetry) {
        PowerManager.WakeLock wl = c.getSystemService(PowerManager.class)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocumWatch:probe");
        wl.acquire(30_000); // keeps the phone awake for the check; released by timeout
        WatchService s = instance;
        Handler main = new Handler(Looper.getMainLooper());
        if (s == null) {
            // The app has only just been started (e.g. by the check-in alarm): give the
            // service and the listener a few seconds to come up, then check once.
            start(c);
            if (mayRetry) main.postDelayed(() -> probeThen(c, after, false), 8_000);
            else main.post(after);
            return;
        }
        main.post(() -> {
            if (!WaListener.isConnected()) WaListener.requestRebindNow(c);
            long sent = SystemClock.elapsedRealtime();
            s.getSystemService(NotificationManager.class).notify(NOTIF_ID, s.buildNotification());
            main.postDelayed(() -> {
                s.checkBeat(sent);
                after.run();
            }, 6_000);
        });
    }
    private volatile boolean running;
    private Thread worker;
    private volatile HttpURLConnection current;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int missedBeats;
    private int beatsDone;

    public static void start(Context c) {
        try {
            c.startForegroundService(new Intent(c, WatchService.class));
        } catch (Throwable t) {
            Log.w(TAG, "could not start WatchService", t);
        }
    }

    /** Update the ongoing notification (e.g. after switching on/off). */
    public static void refresh(Context c) { start(c); }

    /** Drop the ntfy connection so it reconnects with the current switch code. */
    static void restartStream() {
        WatchService s = instance;
        if (s == null) return;
        HttpURLConnection c = s.current;
        if (c != null) Net.async(c::disconnect);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startForeground(NOTIF_ID, buildNotification());
        running = true;
        worker = new Thread(this::streamLoop, "ntfy-listener");
        worker.start();
        Scheduler.scheduleAll(this);
        // First heartbeats soon after start (catches a listener left unbound by an update
        // or reboot within about a minute), then every 5 minutes.
        handler.postDelayed(beat, 20_000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    // ---------- heartbeat: is Android still handing us notifications? ----------
    private final Runnable beat = new Runnable() {
        @Override public void run() {
            final long sent = SystemClock.elapsedRealtime();
            // Re-posting our own notification should come straight back through WaListener.
            getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
            handler.postDelayed(() -> checkBeat(sent), 15_000);
            beatsDone++;
            handler.postDelayed(this, beatsDone < 3 ? 40_000 : BEAT_MS);
        }
    };

    private void checkBeat(long sent) {
        if (!WaListener.isEnabled(this)) {
            missedBeats = 0;
            return;
        }
        if (WaListener.lastSelfSeenAt >= sent) {
            missedBeats = 0;
            markFeedOk();
            return;
        }
        feedFailing = true;
        missedBeats++;
        if (missedBeats == 1) {
            WaListener.requestRebindNow(this);
        } else {
            missedBeats = 0;
            WaListener.forceRebind(this, "notifications had stopped arriving");
        }
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CH_STATUS) == null) {
            NotificationChannel ch = new NotificationChannel(CH_STATUS, "Running",
                NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Prefs p = new Prefs(this);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Locum Watch: alarm " + (p.isArmed() ? "ON" : "OFF"))
            .setContentText(WaListener.isEnabled(this) ? "Watching the locum group"
                : "Needs notification access: tap to open")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build();
    }

    // ---------- the on/off switch (ntfy) ----------
    private void streamLoop() {
        long backoff = 5_000;
        Prefs p = new Prefs(this);
        while (running) {
            String topic = p.topic();
            if (topic.isEmpty()) { sleep(60_000); continue; }
            try {
                String since = p.ntfyLastId();
                String url = "https://ntfy.sh/" + URLEncoder.encode(topic, "UTF-8") + "/json"
                    + (since.isEmpty() ? "" : "?since=" + URLEncoder.encode(since, "UTF-8"));
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                current = c;
                c.setConnectTimeout(20_000);
                c.setReadTimeout(120_000); // ntfy sends a keepalive every ~45 s
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    backoff = 5_000;
                    String line;
                    while (running && (line = r.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        if (!topic.equals(p.topic())) break; // switch code changed in settings
                        JSONObject o = new JSONObject(line);
                        String event = o.optString("event");
                        long serverTime = o.optLong("time", 0);
                        long nowS = System.currentTimeMillis() / 1000;
                        if ("open".equals(event) && serverTime > 0) {
                            // The server's clock tells us if this phone's clock has drifted.
                            p.setClockSkewSeconds(nowS - serverTime);
                        }
                        if (!"message".equals(event)) continue;
                        p.setNtfyLastId(o.optString("id"));
                        String msg = o.optString("message");
                        // Judge age by the server's clock where we can, so a wrong phone clock
                        // cannot make every command look stale.
                        long ageS = nowS - p.clockSkewSeconds() - serverTime;
                        if (ageS > COMMAND_MAX_AGE_S) {
                            p.addLog("Ignored old switch command \"" + WaListener.shorten(msg, 20)
                                + "\" (" + (ageS / 60) + " min old)");
                            continue;
                        }
                        handleWithWakeLock(msg);
                    }
                } finally {
                    c.disconnect();
                    current = null;
                }
            } catch (Exception e) {
                Log.w(TAG, "ntfy stream dropped: " + e);
            }
            if (running) {
                sleep(backoff);
                backoff = Math.min(backoff * 2, 60_000);
            }
        }
    }

    private void handleWithWakeLock(String msg) {
        PowerManager.WakeLock wl = getSystemService(PowerManager.class)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocumWatch:command");
        wl.acquire(30_000); // released by timeout, so the async reply has time to send
        try {
            Commands.handle(this, msg);
        } catch (Throwable t) {
            Log.e(TAG, "command failed", t);
            new Prefs(this).addLog("ERROR handling switch command: " + t);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    @Override
    public void onDestroy() {
        running = false;
        instance = null;
        handler.removeCallbacksAndMessages(null);
        HttpURLConnection c = current;
        if (c != null) Net.async(c::disconnect);
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
