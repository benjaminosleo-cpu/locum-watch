package com.benjaminosleo.locumwatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Status, setup buttons, settings and the recent-decisions log. */
public class MainActivity extends Activity {
    private Prefs prefs;
    private TextView status;
    private TextView log;
    private TextView diag;
    private Switch armed;
    private Button accessBtn;
    private Button batteryBtn;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        col.setPadding(pad, pad, pad, pad);

        armed = new Switch(this);
        armed.setText("Locum alarm");
        armed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        armed.setTypeface(Typeface.DEFAULT_BOLD);
        armed.setOnCheckedChangeListener((b, on) -> {
            if (on != prefs.isArmed()) {
                prefs.setArmed(on);
                prefs.addLog("Switched " + (on ? "ON" : "OFF") + " on this phone");
                WatchService.refresh(this);
            }
        });
        col.addView(armed);

        status = text(14);
        status.setPadding(0, dp(8), 0, dp(8));
        col.addView(status);

        accessBtn = button(col, "Step 1: allow notification access",
            v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        batteryBtn = button(col, "Step 2: stop battery saving for this app", v -> askBattery());
        button(col, "Reconnect to WhatsApp notifications", v -> {
            WaListener.forceRebind(this, "button");
            Toast.makeText(this, "Reconnecting\u2026", Toast.LENGTH_SHORT).show();
            status.postDelayed(this::refresh, 2000);
        });
        button(col, "Test the alarm (30 seconds)",
            v -> AlarmService.start(this, "TEST", "Test alarm from the app. Tap STOP ALARM to silence it.", true));
        button(col, "Send status to her iPhone", v -> {
            Commands.reply(this, "Locum alarm status", Commands.statusText(this), false);
            Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show();
        });

        col.addView(heading("Settings"));
        EditText groups = field(col, "WhatsApp groups to watch (one per line)", prefs.groupsRaw(), true);
        EditText topic = field(col, "Switch code (ntfy topic)", prefs.topic(), false);
        EditText cooldown = number(col, "Quiet minutes after an alarm", prefs.cooldownMinutes());
        EditText minutes = number(col, "Ring for up to (minutes)", prefs.alarmMinutes());
        EditText poToken = field(col, "Pushover API token (optional)", prefs.pushoverToken(), false);
        EditText poUser = field(col, "Pushover user key (optional)", prefs.pushoverUser(), false);
        button(col, "Save settings", v -> {
            prefs.setGroupsRaw(groups.getText().toString());
            if (!topic.getText().toString().trim().equals(prefs.topic())) {
                prefs.setTopic(topic.getText().toString());
                WatchService.restartStream();
            }
            prefs.setCooldownMinutes(parse(cooldown, 60));
            prefs.setAlarmMinutes(parse(minutes, 10));
            prefs.setPushover(poToken.getText().toString(), poUser.getText().toString());
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            refresh();
        });

        col.addView(heading("What it decided recently"));
        log = text(12);
        log.setTypeface(Typeface.MONOSPACE);
        col.addView(log);

        col.addView(heading("Diagnostics: what WhatsApp delivered"));
        diag = text(11);
        diag.setTypeface(Typeface.MONOSPACE);
        col.addView(diag);

        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(col);
        setContentView(sv);
        WatchService.start(this);
    }

    // Refresh the screen whenever the app records something, while it is open.
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener live =
        (sp, key) -> runOnUiThread(this::refreshStatusAndLog);

    @Override
    protected void onResume() {
        super.onResume();
        if (!WaListener.isConnected()) WaListener.requestRebindNow(this);
        refresh();
        prefs.listen(live);
    }

    @Override
    protected void onPause() {
        prefs.unlisten(live);
        super.onPause();
    }

    private void refreshStatusAndLog() {
        status.setText(Commands.statusText(this));
        String l = prefs.log();
        log.setText(l.isEmpty() ? "Nothing yet. Messages in the watched groups will appear here." : l);
        String d = prefs.diag();
        diag.setText(d.isEmpty() ? "Nothing yet." : d);
        armed.setChecked(prefs.isArmed());
    }

    private void refresh() {
        armed.setChecked(prefs.isArmed());
        status.setText(Commands.statusText(this));
        boolean access = WaListener.isEnabled(this);
        accessBtn.setText(access ? "Notification access: allowed" : "Step 1: allow notification access");
        accessBtn.setTextColor(access ? Color.rgb(20, 120, 40) : Color.rgb(180, 20, 20));
        boolean battery = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        batteryBtn.setText(battery ? "Battery saving: off for this app" : "Step 2: stop battery saving for this app");
        batteryBtn.setTextColor(battery ? Color.rgb(20, 120, 40) : Color.rgb(180, 20, 20));
        refreshStatusAndLog();
    }

    private void askBattery() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    // ---------- small UI helpers ----------
    private TextView text(int sp) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return t;
    }

    private TextView heading(String s) {
        TextView t = text(18);
        t.setText(s);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(20), 0, dp(4));
        return t;
    }

    private Button button(LinearLayout col, String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        col.addView(b);
        return b;
    }

    private EditText field(LinearLayout col, String label, String value, boolean multiline) {
        TextView t = text(13);
        t.setText(label);
        t.setPadding(0, dp(8), 0, 0);
        col.addView(t);
        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(InputType.TYPE_CLASS_TEXT
            | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0)
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        col.addView(e);
        return e;
    }

    private EditText number(LinearLayout col, String label, int value) {
        EditText e = field(col, label, String.valueOf(value), false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private static int parse(EditText e, int dflt) {
        try { return Integer.parseInt(e.getText().toString().trim()); } catch (Exception x) { return dflt; }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
