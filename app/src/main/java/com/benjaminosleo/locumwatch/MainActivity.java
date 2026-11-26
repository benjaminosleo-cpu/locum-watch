package com.benjaminosleo.locumwatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.benjaminosleo.locumwatch.ArtKit.ArtDrawable;
import com.benjaminosleo.locumwatch.ArtKit.PlankButton;
import com.benjaminosleo.locumwatch.ArtKit.ToggleView;
import com.benjaminosleo.locumwatch.ArtKit.VineSlider;
import com.benjaminosleo.locumwatch.art.Decor;
import com.benjaminosleo.locumwatch.art.Hero;

import java.util.List;

/**
 * The main page: the cottage window looking out at Mochi, then (scrolling down) the
 * alarm switch and volume, health checks, settings and the recent-decisions log.
 * The look is drawn by the "art" package; what the controls do is unchanged.
 */
public class MainActivity extends Activity {
    private Prefs prefs;
    private HeroView hero;
    private TextView headline, subline;
    private TextView status, log, diag;
    private ToggleView armed;
    private PlankButton accessBtn, batteryBtn;
    private int parchmentSeed = 1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        getWindow().setStatusBarColor(0xFF4A2F1C);
        getWindow().setNavigationBarColor(0xFF4A2F1C);
        getWindow().getDecorView().setBackgroundColor(ArtKit.WALL_FALLBACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // ---------- the window ----------
        int screenW = getResources().getDisplayMetrics().widthPixels;
        float s = screenW / Hero.W; // pixels per art unit in the hero
        FrameLayout heroBox = new FrameLayout(this);
        hero = new HeroView(this, prefs);
        hero.setOnLanternTap(() -> setArmed(!prefs.isArmed(), "lantern"));
        heroBox.addView(hero, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        // status lines on the calm strip of wall under the sill
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.VERTICAL);
        strip.setGravity(Gravity.CENTER);
        headline = text(17, ArtKit.INK, true);
        headline.setGravity(Gravity.CENTER);
        subline = text(12, ArtKit.INK_SOFT, false);
        subline.setGravity(Gravity.CENTER);
        subline.setMaxLines(2);
        strip.addView(headline);
        strip.addView(subline);
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(
            Math.round((Hero.STATUS_R - Hero.STATUS_L + 60) * s), Math.round((Hero.STATUS_B - Hero.STATUS_T + 14) * s));
        sl.leftMargin = Math.round((Hero.STATUS_L - 30) * s);
        sl.topMargin = Math.round((Hero.STATUS_T - 7) * s);
        heroBox.addView(strip, sl);
        root.addView(heroBox);

        // ---------- the rest of the cottage ----------
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackground(new ArtKit.WallDrawable(this, Hero.H));
        page.setPadding(dp(16), dp(4), dp(16), dp(24));
        root.addView(page);

        // Alarm
        sign(page, "Alarm");
        LinearLayout alarmCard = card(page);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView armedLabel = text(17, ArtKit.INK, false);
        armedLabel.setText("Locum alarm switched on");
        row.addView(armedLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        armed = new ToggleView(this);
        armed.setOn(prefs.isArmed());
        armed.setListener(on -> setArmed(on, "switch"));
        row.addView(armed);
        alarmCard.addView(row);

        final TextView volLabel = text(15, ArtKit.INK, false);
        volLabel.setPadding(0, dp(10), 0, 0);
        volLabel.setText("Alarm volume: " + prefs.alarmVolumePercent() + "%");
        alarmCard.addView(volLabel);
        VineSlider vol = new VineSlider(this, 9, prefs.alarmVolumePercent() / 10 - 1); // 10% steps
        vol.setListener(new VineSlider.Listener() {
            @Override public void onMoved(int step) { volLabel.setText("Alarm volume: " + (step + 1) * 10 + "%"); }
            @Override public void onReleased(int step) {
                int pct = (step + 1) * 10;
                if (pct != prefs.alarmVolumePercent()) {
                    prefs.setAlarmVolumePercent(pct);
                    prefs.addLog("Alarm volume set to " + pct + "%");
                }
            }
        });
        alarmCard.addView(vol);
        button(alarmCard, "Test the alarm (30 seconds)",
            v -> AlarmService.start(this, "TEST", "Test alarm from the app. Tap STOP to silence it.", true));

        shelf(page, Decor.SHELF_APOTHECARY);

        // Checks
        sign(page, "Checks");
        LinearLayout checks = card(page);
        status = text(13, ArtKit.INK, false);
        status.setLineSpacing(dp(2), 1f);
        status.setPadding(0, 0, 0, dp(6));
        checks.addView(status);
        accessBtn = button(checks, "Step 1: allow notification access",
            v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        batteryBtn = button(checks, "Step 2: stop battery saving for this app", v -> askBattery());
        button(checks, "Reconnect to WhatsApp notifications", v -> {
            WaListener.forceRebind(this, "button");
            Toast.makeText(this, "Reconnecting…", Toast.LENGTH_SHORT).show();
            status.postDelayed(this::refresh, 2000);
        });
        button(checks, "Send status to her iPhone", v -> {
            WatchService.probeThen(this, () ->
                Commands.reply(this, "Locum alarm status", Commands.statusText(this), false));
            Toast.makeText(this, "Sending…", Toast.LENGTH_SHORT).show();
        });

        shelf(page, Decor.SHELF_LIBRARY);

        // Settings
        sign(page, "Settings");
        LinearLayout settings = card(page);
        EditText groups = field(settings, "WhatsApp groups to watch (one per line)", prefs.groupsRaw(), true);
        EditText topic = field(settings, "Switch code (ntfy topic)", prefs.topic(), false);
        EditText cooldown = number(settings, "Quiet minutes after an alarm", prefs.cooldownMinutes());
        EditText minutes = number(settings, "Ring for up to (minutes)", prefs.alarmMinutes());
        EditText poToken = field(settings, "Pushover API token (optional)", prefs.pushoverToken(), false);
        EditText poUser = field(settings, "Pushover user key (optional)", prefs.pushoverUser(), false);
        button(settings, "Save settings", v -> {
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

        shelf(page, Decor.SHELF_BREWING);

        // Logs
        sign(page, "Message log");
        log = text(12, ArtKit.INK, false);
        log.setTypeface(Typeface.MONOSPACE);
        card(page).addView(log);

        sign(page, "Diagnostics");
        diag = text(11, ArtKit.INK, false);
        diag.setTypeface(Typeface.MONOSPACE);
        card(page).addView(diag);

        TextView footer = text(12, ArtKit.INK_SOFT, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        footer.setText("Locum Watch " + Commands.version(this) + "  ·  Mochi is on duty");
        page.addView(footer);

        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.addView(root);
        // Don't pop the keyboard up or jump to the first text field on opening.
        root.setFocusableInTouchMode(true);
        setContentView(sv);
        WatchService.start(this);
    }

    private void setArmed(boolean on, String how) {
        if (on != prefs.isArmed()) {
            prefs.setArmed(on);
            prefs.addLog("Switched " + (on ? "ON" : "OFF") + " on this phone (" + how + ")");
            WatchService.refresh(this);
            if ("lantern".equals(how)) {
                Toast.makeText(this, on ? "Locum alarm ON" : "Locum alarm OFF", Toast.LENGTH_SHORT).show();
            }
        }
        armed.setOn(on);
        hero.refreshNow();
        refreshStatusAndLog();
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
        boolean on = prefs.isArmed();
        List<String> problems = Commands.problems(this);
        headline.setText(on ? "Mochi is on watch" : "Alarm is OFF");
        if (!problems.isEmpty()) {
            subline.setTextColor(ArtKit.BAD);
            subline.setText("⚠ " + problems.get(0));
        } else {
            subline.setTextColor(ArtKit.INK_SOFT);
            subline.setText(on ? "The lantern is lit: locum requests will ring"
                : "Tap the lantern to switch the alarm on");
        }
        status.setText(Commands.statusText(this));
        String l = prefs.log();
        log.setText(l.isEmpty() ? "Nothing yet. Messages in the watched groups will appear here." : l);
        String d = prefs.diag();
        diag.setText(d.isEmpty() ? "Nothing yet." : d);
        if (armed.isOn() != on) {
            armed.setOn(on);
            hero.refreshNow();
        }
    }

    private void refresh() {
        boolean access = WaListener.isEnabled(this);
        accessBtn.setText(access ? "Notification access: allowed" : "Step 1: allow notification access");
        accessBtn.setStyle(access ? Decor.BUTTON_GOOD : Decor.BUTTON_ATTENTION);
        boolean battery = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        batteryBtn.setText(battery ? "Battery saving: off for this app" : "Step 2: stop battery saving for this app");
        batteryBtn.setStyle(battery ? Decor.BUTTON_GOOD : Decor.BUTTON_ATTENTION);
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

    // ---------- building blocks ----------
    private TextView text(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.SERIF, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    /** A hanging wooden sign with a section title. */
    private void sign(LinearLayout page, String title) {
        TextView t = text(17, ArtKit.CREAM, true);
        t.setText(title);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(13), 0, 0);
        t.setBackground(new ArtDrawable(this, 0xFF6B4630,
            (p, w, h, pressed, focused) -> ArtKit.decor().sectionHeader(p, 0, 0, w, h)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(190), dp(48));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(10);
        page.addView(t, lp);
    }

    /** A parchment card that holds a group of controls. */
    private LinearLayout card(LinearLayout page) {
        final int seed = parchmentSeed++;
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(20));
        c.setBackground(new ArtDrawable(this, 0xFFFBF0D2,
            (p, w, h, pressed, focused) -> ArtKit.decor().parchment(p, 0, 0, w, h, seed)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        page.addView(c, lp);
        return c;
    }

    /** A shelf of cottage bits and pieces, as a divider between sections. */
    private void shelf(LinearLayout page, final int kind) {
        View v = new View(this);
        v.setBackground(new ArtDrawable(this, 0x00000000,
            (p, w, h, pressed, focused) -> ArtKit.decor().shelf(p, kind, 12, Decor.SHELF_ABOVE, w - 24)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            dp(Math.round(Decor.SHELF_ABOVE + Decor.SHELF_BELOW)));
        lp.topMargin = dp(8);
        page.addView(v, lp);
    }

    private PlankButton button(LinearLayout col, String label, View.OnClickListener l) {
        PlankButton b = new PlankButton(this, label);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        col.addView(b, lp);
        return b;
    }

    private EditText field(LinearLayout col, String label, String value, boolean multiline) {
        TextView t = text(13, ArtKit.INK_SOFT, false);
        t.setText(label);
        t.setPadding(0, dp(10), 0, dp(3));
        col.addView(t);
        EditText e = new EditText(this);
        e.setText(value);
        e.setTextColor(ArtKit.INK);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTypeface(Typeface.SERIF);
        e.setMinHeight(dp(40));
        e.setPadding(dp(10), dp(8), dp(10), dp(9));
        e.setInputType(InputType.TYPE_CLASS_TEXT
            | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0)
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setBackground(new ArtDrawable(this, 0xFFF5E6BE,
            (p, w, h, pressed, focused) -> ArtKit.decor().field(p, 0, 0, w, h, focused)));
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

    private int dp(int v) { return ArtKit.dp(this, v); }
}
