package com.benjaminosleo.locumwatch;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/** Reads WhatsApp notifications and sets off the alarm for locum requests. */
public class WaListener extends NotificationListenerService {
    private static final String TAG = "LocumWatch";
    private static final LocumEngine ENGINE = new LocumEngine();
    private static volatile WaListener instance;
    private static volatile long intentionalReconnectAt;

    @Override
    public void onListenerConnected() {
        instance = this;
        WatchService.start(this);
    }

    @Override
    public void onListenerDisconnected() {
        instance = null;
        if (System.currentTimeMillis() - intentionalReconnectAt > 10_000) {
            new Prefs(this).addLog("WhatsApp notification access dropped by Android; reconnecting");
        }
        requestRebind(new ComponentName(this, WaListener.class));
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (!"com.whatsapp".equals(pkg) && !"com.whatsapp.w4b".equals(pkg)) return;
        try {
            handle(sbn.getNotification());
        } catch (Throwable t) {
            Log.e(TAG, "handle failed", t);
            new Prefs(this).addLog("ERROR reading notification: " + t);
        }
    }

    private void handle(Notification n) {
        Bundle ex = n.extras;
        if (ex == null) return;
        // WhatsApp's bundle summary ("4 new messages") repeats what the chat notification holds.
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        Prefs p = new Prefs(this);
        List<String> groups = p.groups();

        String conv = str(ex.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        String title = str(ex.getCharSequence(Notification.EXTRA_TITLE));
        String text = str(ex.getCharSequence(Notification.EXTRA_TEXT));

        p.setLastAnyNotif(System.currentTimeMillis(), conv.isEmpty() ? title : conv);

        String group = MessageParsing.matchGroup(conv, groups);
        if (group == null) group = MessageParsing.matchGroup(title, groups);
        if (group == null) return;

        List<MessageParsing.Msg> msgs = new ArrayList<>();
        Parcelable[] arr = ex.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (arr != null && arr.length > 0) {
            // MessagingStyle: one bundle per message, with its own sender and time.
            for (Parcelable pa : arr) {
                if (!(pa instanceof Bundle)) continue;
                Bundle b = (Bundle) pa;
                CharSequence t = b.getCharSequence("text");
                if (t == null) continue;
                CharSequence s = b.getCharSequence("sender");
                if (s == null && Build.VERSION.SDK_INT >= 28) {
                    Object person = b.get("sender_person");
                    if (person instanceof android.app.Person) s = ((android.app.Person) person).getName();
                }
                msgs.add(new MessageParsing.Msg(group, s == null ? "" : s.toString(), t.toString(),
                    b.getLong("time", 0)));
            }
        } else {
            MessageParsing.Msg m = MessageParsing.parseTitleText(title, text, groups);
            if (m != null) msgs.add(m);
        }

        if (msgs.isEmpty()) {
            p.addLog(group + " | notification with no readable message (title: " + shorten(title, 40)
                + ", text: " + shorten(text, 60) + ")");
            return;
        }
        long now = System.currentTimeMillis();
        for (MessageParsing.Msg m : msgs) {
            LocumEngine.Outcome o = ENGINE.evaluate(m, now, p);
            if (o == LocumEngine.Outcome.DUPLICATE) continue;
            String line = m.group + " | " + (m.sender.isEmpty() ? "(her)" : m.sender) + ": "
                + shorten(m.text, 120) + " -> " + o.label;
            p.addLog(line);
            p.setLastWhatsApp(now, line);
            if (o == LocumEngine.Outcome.ALARM) {
                AlarmService.start(this, m.sender.isEmpty() ? m.group : m.sender, m.text, false);
            }
        }
    }

    /** Make sure the phone itself is not in Do Not Disturb when the alarm rings. */
    static void allowSound() {
        WaListener l = instance;
        if (l == null) return;
        try {
            if (l.getCurrentInterruptionFilter() != NotificationListenerService.INTERRUPTION_FILTER_ALL) {
                l.requestInterruptionFilter(NotificationListenerService.INTERRUPTION_FILTER_ALL);
            }
        } catch (Throwable t) {
            Log.w(TAG, "could not change DND", t);
        }
    }

    static boolean isEnabled(Context c) {
        String flat = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(c.getPackageName());
    }

    static boolean isConnected() { return instance != null; }

    /**
     * Android 9 sometimes drops a notification listener (after an app restart or update)
     * and never reconnects it. If access is granted but we are not connected, ask for a
     * rebind; with force, also toggle the component, which makes the system bind afresh.
     */
    /** Gentle: just ask the system to rebind if we are not connected. */
    static void rebindIfNeeded(Context c) {
        if (!isEnabled(c) || isConnected()) return;
        try { requestRebind(new ComponentName(c, WaListener.class)); } catch (Throwable ignored) { }
    }

    static void ensureBound(Context c, boolean force) {
        if (!isEnabled(c)) return;
        if (isConnected() && !force) return;
        ComponentName cn = new ComponentName(c, WaListener.class);
        try { requestRebind(cn); } catch (Throwable ignored) { }
        if (force || !isConnected()) {
            intentionalReconnectAt = System.currentTimeMillis();
            android.content.pm.PackageManager pm = c.getPackageManager();
            pm.setComponentEnabledSetting(cn, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(cn, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP);
            try { requestRebind(cn); } catch (Throwable ignored) { }
            new Prefs(c).addLog("Reconnected to WhatsApp notifications" + (force ? "" : " (was disconnected)"));
        }
    }

    private static String str(CharSequence cs) { return cs == null ? "" : cs.toString(); }

    static String shorten(String s, int max) {
        String one = s.replace('\n', ' ');
        return one.length() <= max ? one : one.substring(0, max - 1) + "…";
    }
}
