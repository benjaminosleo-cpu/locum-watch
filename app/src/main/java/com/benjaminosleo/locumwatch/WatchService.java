package com.benjaminosleo.locumwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Always-on foreground service: keeps the app alive and listens to the private ntfy
 * topic for "on", "off", "status" and "test" from her iPhone shortcuts.
 */
public class WatchService extends Service {
    private static final String TAG = "LocumWatch";
    static final String CH_STATUS = "status";
    private static final int NOTIF_ID = 1;
    private static final long COMMAND_MAX_AGE_S = 10 * 60; // ignore commands older than 10 min

    private volatile boolean running;
    private Thread worker;
    private volatile HttpURLConnection current;

    public static void start(Context c) {
        try {
            c.startForegroundService(new Intent(c, WatchService.class));
        } catch (Throwable t) {
            Log.w(TAG, "could not start WatchService", t);
        }
    }

    /** Update the ongoing notification (e.g. after switching on/off). */
    public static void refresh(Context c) { start(c); }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIF_ID, buildNotification());
        running = true;
        worker = new Thread(this::streamLoop, "ntfy-listener");
        worker.start();
        Scheduler.scheduleAll(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
        return START_STICKY;
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
            .build();
    }

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
                        JSONObject o = new JSONObject(line);
                        if (!"message".equals(o.optString("event"))) continue;
                        p.setNtfyLastId(o.optString("id"));
                        long ageS = System.currentTimeMillis() / 1000 - o.optLong("time", 0);
                        if (ageS > COMMAND_MAX_AGE_S) continue; // replayed after an outage
                        handleWithWakeLock(o.optString("message"));
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
        Commands.handle(this, msg);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    @Override
    public void onDestroy() {
        running = false;
        HttpURLConnection c = current;
        if (c != null) c.disconnect();
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
