package com.benjaminosleo.locumwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/** Plays the bedside alarm at full alarm volume until stopped or timed out. */
public class AlarmService extends Service {
    private static final String TAG = "LocumWatch";
    static final String CH_ALARM = "alarm";
    static final String ACTION_START = "com.benjaminosleo.locumwatch.ALARM_START";
    static final String ACTION_STOP = "com.benjaminosleo.locumwatch.ALARM_STOP";
    static final String ACTION_FINISHED = "com.benjaminosleo.locumwatch.ALARM_FINISHED";
    static final String EXTRA_SENDER = "sender";
    static final String EXTRA_TEXT = "text";
    static final String EXTRA_TEST = "test";
    private static final int NOTIF_ID = 2;
    private static final int TEST_SECONDS = 30;

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int savedVolume = -1;
    private boolean testAlarm;
    private android.media.ToneGenerator beeper;
    private final Runnable timeout = () -> finishAlarm(true);
    private final Runnable beepLoop = new Runnable() {
        @Override public void run() {
            if (beeper == null) return;
            beeper.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000);
            handler.postDelayed(this, 1500);
        }
    };

    public static void start(Context c, String sender, String text, boolean test) {
        Intent i = new Intent(c, AlarmService.class).setAction(ACTION_START)
            .putExtra(EXTRA_SENDER, sender).putExtra(EXTRA_TEXT, text).putExtra(EXTRA_TEST, test);
        c.startForegroundService(i);
    }

    public static void stop(Context c) {
        c.startService(new Intent(c, AlarmService.class).setAction(ACTION_STOP));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (!ACTION_START.equals(action)) {
            // Must still call startForeground if started with startForegroundService.
            startForeground(NOTIF_ID, buildNotification("Locum Watch", "Stopping alarm", null));
            finishAlarm(false);
            return START_NOT_STICKY;
        }
        String sender = intent.getStringExtra(EXTRA_SENDER);
        String text = intent.getStringExtra(EXTRA_TEXT);
        boolean test = intent.getBooleanExtra(EXTRA_TEST, false);
        testAlarm = test;

        Intent show = new Intent(this, AlarmActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SENDER, sender).putExtra(EXTRA_TEXT, text).putExtra(EXTRA_TEST, test);
        startForeground(NOTIF_ID, buildNotification(
            test ? "TEST ALARM" : "LOCUM REQUEST: " + sender, text, show));
        try { startActivity(show); } catch (Throwable t) { Log.w(TAG, "activity start blocked", t); }

        startSound();
        Prefs p = new Prefs(this);
        long ms = test ? TEST_SECONDS * 1000L : p.alarmMinutes() * 60_000L;
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, ms);

        if (test) {
            // A test also checks the iPhone side: urgent, so Pushover (if set up) sends a
            // short Critical Alert too.
            Commands.reply(this, "Locum alarm: test",
                "The bedside phone is ringing now (" + TEST_SECONDS + " seconds).", true, 120);
        } else {
            Commands.reply(this, "LOCUM: " + sender, text, true);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String title, String text, Intent show) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CH_ALARM) == null) {
            NotificationChannel ch = new NotificationChannel(CH_ALARM, "Locum alarm",
                NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null); // the service plays the sound itself
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = new Notification.Builder(this, CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true);
        PendingIntent stopPi = PendingIntent.getService(this, 1,
            new Intent(this, AlarmService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        b.addAction(new Notification.Action.Builder(null, "STOP", stopPi).build());
        if (show != null) {
            PendingIntent showPi = PendingIntent.getActivity(this, 2, show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.setContentIntent(showPi).setFullScreenIntent(showPi, true);
        }
        return b.build();
    }

    private void startSound() {
        stopSound();
        WaListener.allowSound();
        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocumWatch:alarm");
        wakeLock.acquire(61 * 60_000L);

        AudioManager am = getSystemService(AudioManager.class);
        savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        Uri[] candidates = {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        };
        for (Uri uri : candidates) {
            if (uri == null) continue;
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(attrs);
                mp.setDataSource(this, uri);
                mp.setLooping(true);
                mp.prepare();
                mp.start();
                player = mp;
                break;
            } catch (Exception e) {
                Log.w(TAG, "could not play " + uri, e);
            }
        }

        if (player == null) {
            // No ringtone file could be played: fall back to generated beeps on the alarm stream.
            new Prefs(this).addLog("No alarm sound file could be played; using beeps instead");
            try {
                beeper = new android.media.ToneGenerator(AudioManager.STREAM_ALARM, 100);
                handler.post(beepLoop);
            } catch (Throwable t) {
                Log.e(TAG, "no sound at all", t);
                new Prefs(this).addLog("ERROR: the alarm could not make any sound: " + t);
            }
        }

        vibrator = getSystemService(Vibrator.class);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 900, 600}, 0), attrs);
        }
    }

    private void stopSound() {
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(beepLoop);
        if (beeper != null) { beeper.release(); beeper = null; }
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
        if (vibrator != null) { vibrator.cancel(); vibrator = null; }
        if (savedVolume >= 0) {
            AudioManager am = getSystemService(AudioManager.class);
            am.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0);
            savedVolume = -1;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }

    private void finishAlarm(boolean timedOut) {
        if (timedOut && !testAlarm) {
            int min = new Prefs(this).alarmMinutes();
            new Prefs(this).addLog("Alarm rang for " + min + " min and nobody stopped it");
            Commands.reply(this, "Locum alarm was not answered",
                "The bedside phone rang for " + min + " minutes and nobody tapped STOP.", false);
        }
        stopSound();
        sendBroadcast(new Intent(ACTION_FINISHED).setPackage(getPackageName()));
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopSound();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
