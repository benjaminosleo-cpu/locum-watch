package com.benjaminosleo.locumwatch;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;

/** Starts everything after a reboot or update, and handles the scheduled alarms. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        switch (a) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                WatchService.start(c);
                Scheduler.scheduleAll(c);
                if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
                    // Android 9 often leaves the listener unbound after an update: force it.
                    WaListener.ensureBound(c, true);
                } else {
                    try {
                        NotificationListenerService.requestRebind(new ComponentName(c, WaListener.class));
                    } catch (Throwable ignored) { }
                }
                break;
            case Scheduler.ACTION_WATCHDOG:
                WatchService.start(c);
                WaListener.ensureBound(c, false);
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
