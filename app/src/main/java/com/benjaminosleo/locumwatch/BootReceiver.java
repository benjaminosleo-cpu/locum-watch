package com.benjaminosleo.locumwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts everything after a reboot or update, and handles the scheduled alarms. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        switch (a) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                // WatchService's heartbeat checks the notification feed within a minute
                // and forces a reconnect if Android has left it unbound.
                WatchService.start(c);
                Scheduler.scheduleAll(c);
                WaListener.requestRebindNow(c);
                break;
            case Scheduler.ACTION_WATCHDOG:
                WatchService.start(c);
                break;
            case Scheduler.ACTION_CHECKIN:
                WatchService.start(c);
                Commands.checkIn(c);
                Scheduler.scheduleCheckIn(c);
                break;
            default:
        }
    }
}
