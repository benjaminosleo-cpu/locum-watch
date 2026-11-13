package com.benjaminosleo.locumwatch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Calendar;

/** Daily 20:30 check-in and a 15-minute watchdog that restarts the service if killed. */
final class Scheduler {
    static final String ACTION_CHECKIN = "com.benjaminosleo.locumwatch.CHECKIN";
    static final String ACTION_WATCHDOG = "com.benjaminosleo.locumwatch.WATCHDOG";
    private static final int CHECKIN_HOUR = 20, CHECKIN_MIN = 30;

    private Scheduler() {}

    static void scheduleAll(Context c) {
        scheduleCheckIn(c);
        scheduleWatchdog(c);
    }

    static void scheduleCheckIn(Context c) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, CHECKIN_HOUR);
        cal.set(Calendar.MINUTE, CHECKIN_MIN);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 60_000) cal.add(Calendar.DAY_OF_YEAR, 1);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi(c, ACTION_CHECKIN, 10));
    }

    static void scheduleWatchdog(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi(c, ACTION_WATCHDOG, 11));
    }

    private static PendingIntent pi(Context c, String action, int code) {
        Intent i = new Intent(c, BootReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
