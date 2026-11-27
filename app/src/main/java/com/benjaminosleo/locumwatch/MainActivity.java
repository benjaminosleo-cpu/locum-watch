package com.benjaminosleo.locumwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.FrameMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
 * The main page: the cottage window looking out at Mochi, with a status board hanging
 * under the sill, then (scrolling down) the alarm switch and volume, health checks,
 * settings and the recent-decisions log.
 * The look is drawn by the "art" package; what the controls do is unchanged.
 */
public class MainActivity extends Activity {
    private static final int LOG_SHORT = 15;           // entries shown until "Show all" is tapped
    private static final int WARN = 0xFF8A1F14;        // warnings on parchment
    private static final int CREAM_SOFT = 0xFFF2E3C4;  // second line on the status board
    private static final long RELIGHT_MS = 5 * 60_000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private ScrollView scroll;
    private LinearLayout root, page;
    private HeroView hero;
    private LinearLayout board;
    private int boardStyle = Decor.STATUS_OK;
    private boolean boardHasProblem;
    private TextView headline, subline;
    private View checksSign;
    private TextView status, log, diag, perf;
    private ToggleView armed;
    private PlankButton accessBtn, batteryBtn, logMore;
    private boolean showAllLog;
    private int parchmentSeed = 1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        getWindow().setStatusBarColor(0xFF4A2F1C);
        getWindow().setNavigationBarColor(0xFF4A2F1C);
        getWindow().getDecorView().setBackgroundColor(ArtKit.WALL_FALLBACK);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipChildren(false);

        // ---------- the window ----------
        int screenW = getResources().getDisplayMetrics().widthPixels;
        float s = screenW / Hero.W; // pixels per art unit in the hero
        FrameLayout heroBox = new FrameLayout(this);
        hero = new HeroView(this, prefs);
        hero.setOnLanternTap(this::onLanternTap);
        heroBox.addView(hero, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        // the status board hanging under the sill: is the alarm on, is anything wrong
        board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setGravity(Gravity.CENTER);
        int boardW = Math.round((Hero.BOARD_R - Hero.BOARD_L) * s), boardH = Math.round((Hero.BOARD_B - Hero.BOARD_T) * s);
        board.setPadding(dp(10), Math.round(boardH * Decor.STATUS_BOARD_TOP_FRACTION), dp(10), dp(4));
        board.setBackground(new ArtDrawable(this, 0xFF5A3A26,
            (p, w, h, pressed, focused) -> ArtKit.decor().statusBoard(p, 0, 0, w, h, boardStyle)));
        headline = text(19, ArtKit.CREAM, true);
        headline.setGravity(Gravity.CENTER);
        headline.setIncludeFontPadding(false);
        subline = text(13, CREAM_SOFT, false);
        subline.setGravity(Gravity.CENTER);
        subline.setMaxLines(1);
        subline.setEllipsize(TextUtils.TruncateAt.END);
        subline.setIncludeFontPadding(false);
        subline.setPadding(0, dp(2), 0, 0);
        board.addView(headline);
        board.addView(subline);
        board.setOnClickListener(v -> {
            // with a problem showing, the board takes you to the checks
            if (boardHasProblem && checksSign != null) scroll.smoothScrollTo(0, page.getTop() + checksSign.getTop() - dp(8));
        });
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(boardW, boardH);
        bl.leftMargin = Math.round(Hero.BOARD_L * s);
        bl.topMargin = Math.round(Hero.BOARD_T * s);
        heroBox.addView(board, bl);
        root.addView(heroBox);

        // ---------- the rest of the cottage ----------
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipChildren(false);
        page.setBackground(new ArtKit.WallDrawable(this, Hero.H));
        page.setPadding(dp(16), dp(4), dp(16), dp(24));
        root.addView(page);

        // Alarm
        sign(page, "Alarm");
        LinearLayout alarmCard = card(page);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView armedLabel = text(17, ArtKit.INK, false);
        armedLabel.setText("Locum alarm");
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

        // Checks: the two setup steps first, then the full report
        checksSign = sign(page, "Checks");
        LinearLayout checks = card(page);
        accessBtn = button(checks, "Step 1: notification access",
            v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        ((LinearLayout.LayoutParams) accessBtn.getLayoutParams()).topMargin = 0;
        batteryBtn = button(checks, "Step 2: stop battery saving", v -> askBattery());
        status = text(13, ArtKit.INK, false);
        status.setLineSpacing(dp(3), 1f);
        status.setPadding(0, dp(12), 0, dp(2));
        checks.addView(status);
        button(checks, "Reconnect to WhatsApp", v -> {
            WaListener.forceRebind(this, "button");
            Toast.makeText(this, "Reconnecting\u2026", Toast.LENGTH_SHORT).show();
            status.postDelayed(this::refresh, 2000);
        });
        button(checks, "Send status to her iPhone", v -> {
            WatchService.probeThen(this, () ->
                Commands.reply(this, "Locum alarm status", Commands.statusText(this), false));
            Toast.makeText(this, "Sending\u2026", Toast.LENGTH_SHORT).show();
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
        LinearLayout logCard = card(page);
        log = text(12, ArtKit.INK, false);
        log.setTypeface(Typeface.MONOSPACE);
        logCard.addView(log);
        logMore = button(logCard, "Show all", v -> {
            showAllLog = !showAllLog;
            refreshStatusAndLog();
        });

        sign(page, "Diagnostics");
        LinearLayout diagCard = card(page);
        perf = text(12, ArtKit.INK, false);
        perf.setText("Animation: measuring\u2026");
        perf.setPadding(0, 0, 0, dp(8));
        diagCard.addView(perf);
        diag = text(11, ArtKit.INK, false);
        diag.setTypeface(Typeface.MONOSPACE);
        diagCard.addView(diag);

        TextView footer = text(12, ArtKit.INK, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        footer.setText("Locum Watch " + Commands.version(this) + "  \u00B7  Mochi is on duty");
        page.addView(footer);

        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.addView(root);
        // Don't pop the keyboard up or jump to the first text field on opening.
        root.setFocusableInTouchMode(true);
        setContentView(scroll);
        WatchService.start(this);
    }

    /** Any touch anywhere wakes the animation up to full speed for a minute. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN && hero != null) hero.noteInteraction();
        return super.dispatchTouchEvent(ev);
    }

    /** The lantern lights with one tap; putting it out asks first, so a stray touch can't disarm. */
    private void onLanternTap() {
        if (!prefs.isArmed()) {
            setArmed(true, "lantern");
            Toast.makeText(this, "Locum alarm ON", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Switch the locum alarm off?")
            .setMessage("Locum requests will not ring until it is switched back on.")
            .setPositiveButton("Switch off", (d, w) -> setArmed(false, "lantern"))
            .setNegativeButton("Keep it on", null)
            .show();
    }

    private void setArmed(boolean on, String how) {
        if (on != prefs.isArmed()) {
            prefs.setArmed(on);
            prefs.addLog("Switched " + (on ? "ON" : "OFF") + " on this phone (" + how + ")");
            WatchService.refresh(this);
        }
        armed.setOn(on);
        hero.refreshNow();
        refreshStatusAndLog();
    }

    // Refresh the screen when the app records something while it is open. One incoming
    // message writes several values in a row, so wait a moment and refresh once.
    private final Runnable refreshSoon = this::refreshStatusAndLog;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener live = (sp, key) -> {
        main.removeCallbacks(refreshSoon);
        main.postDelayed(refreshSoon, 200);
    };

    // The room's lighting follows the time of day: check every few minutes while open.
    private final Runnable relight = new Runnable() {
        @Override public void run() {
            if (ArtKit.refreshLight()) ArtKit.redrawAll(root);
            main.postDelayed(this, RELIGHT_MS);
        }
    };

    // Real frame timings from Android, shown under Diagnostics.
    private int frames, slowFrames;
    private long frameNanos, perfSince;
    private final Window.OnFrameMetricsAvailableListener frameWatch = (window, metrics, dropped) -> {
        long total = metrics.getMetric(FrameMetrics.TOTAL_DURATION);
        frames++;
        frameNanos += total;
        if (total > 34_000_000L) slowFrames++;
        long now = android.os.SystemClock.elapsedRealtime();
        if (perfSince == 0) perfSince = now;
        if (now - perfSince >= 5000 && frames > 0) {
            perf.setText("Animation: " + Math.round(frames * 1000f / (now - perfSince)) + " frames a second, "
                + (frameNanos / frames / 1_000_000L) + " ms each on average, "
                + Math.round(slowFrames * 100f / frames) + "% slow");
            frames = 0;
            slowFrames = 0;
            frameNanos = 0;
            perfSince = now;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!WaListener.isConnected()) WaListener.requestRebindNow(this);
        main.removeCallbacks(relight);
        relight.run();
        refresh();
        prefs.listen(live);
        try {
            getWindow().addOnFrameMetricsAvailableListener(frameWatch, main);
        } catch (Throwable ignored) { }
    }

    @Override
    protected void onPause() {
        prefs.unlisten(live);
        main.removeCallbacks(relight);
        main.removeCallbacks(refreshSoon);
        try {
            getWindow().removeOnFrameMetricsAvailableListener(frameWatch);
        } catch (Throwable ignored) { }
        perfSince = 0;
        super.onPause();
    }

    private void refreshStatusAndLog() {
        boolean on = prefs.isArmed();
        List<String[]> problems = Commands.problemPairs(this);

        // the board under the window: state first, then the first problem if there is one
        headline.setText(on ? "Alarm ON" : "Alarm OFF");
        int style;
        if (!problems.isEmpty()) {
            style = Decor.STATUS_PROBLEM;
            subline.setText(problems.get(0)[0] + (problems.size() > 1 ? "  +" + (problems.size() - 1) : ""));
        } else if (on) {
            style = Decor.STATUS_OK;
            subline.setText("Mochi is on watch");
        } else {
            style = Decor.STATUS_OFF;
            subline.setText("Tap the lantern to switch on");
        }
        boardHasProblem = !problems.isEmpty();
        if (style != boardStyle) {
            boardStyle = style;
            board.invalidate();
        }

        status.setText(warningsInRed(Commands.statusText(this)));
        log.setText(logText(prefs.log()));
        String d = prefs.diag();
        diag.setText(d.isEmpty() ? "Nothing yet." : d);
        if (armed.isOn() != on) {
            armed.setOn(on);
            hero.refreshNow();
        }
    }

    /** The status report with its warning lines picked out in bold red. */
    private static CharSequence warningsInRed(String report) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        for (String line : report.split("\n")) {
            int start = b.length();
            b.append(line).append('\n');
            if (line.startsWith("\u26A0")) {
                b.setSpan(new ForegroundColorSpan(WARN), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                b.setSpan(new StyleSpan(Typeface.BOLD), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (b.length() > 0) b.delete(b.length() - 1, b.length());
        return b;
    }

    /** The message log: newest first, a gap between entries, alarms in bold red, the rest behind "Show all". */
    private CharSequence logText(String raw) {
        if (raw.isEmpty()) {
            logMore.setVisibility(View.GONE);
            return "Nothing yet. Messages in the watched groups will appear here.";
        }
        String[] lines = raw.split("\n");
        int shown = showAllLog ? lines.length : Math.min(lines.length, LOG_SHORT);
        logMore.setVisibility(lines.length > LOG_SHORT ? View.VISIBLE : View.GONE);
        logMore.setText(showAllLog ? "Show fewer" : "Show all " + lines.length);
        SpannableStringBuilder b = new SpannableStringBuilder();
        for (int i = 0; i < shown; i++) {
            String line = lines[i];
            int start = b.length();
            b.append(line);
            // "dd/MM HH:mm" stamp in a softer ink
            if (line.length() > 11) {
                b.setSpan(new ForegroundColorSpan(ArtKit.INK_SOFT), start, start + 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (line.contains("-> ALARM")) {
                b.setSpan(new ForegroundColorSpan(WARN), start + Math.min(11, line.length()), b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                b.setSpan(new StyleSpan(Typeface.BOLD), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (i < shown - 1) {
                b.append('\n');
                int gap = b.length();
                b.append('\n');
                b.setSpan(new RelativeSizeSpan(0.45f), gap, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return b;
    }

    private void refresh() {
        boolean access = WaListener.isEnabled(this);
        accessBtn.setText(access ? "Notification access: allowed" : "Step 1: notification access");
        accessBtn.setStyle(access ? Decor.BUTTON_GOOD : Decor.BUTTON_ATTENTION);
        boolean battery = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        batteryBtn.setText(battery ? "Battery saving: off" : "Step 2: stop battery saving");
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
    private View sign(LinearLayout page, String title) {
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
        return t;
    }

    /** A parchment card that holds a group of controls. */
    private LinearLayout card(LinearLayout page) {
        final int seed = parchmentSeed++;
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setClipChildren(false); // the buttons' drop shadows hang a little below their boxes
        c.setPadding(dp(18), dp(18), dp(18), dp(20));
        c.setBackground(new ArtDrawable(this, 0xFFFBF0D2,
            (p, w, h, pressed, focused) -> ArtKit.decor().parchment(p, 0, 0, w, h, seed)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(4);
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
        TextView t = text(13, ArtKit.INK, false);
        t.setText(label);
        t.setPadding(0, dp(10), 0, dp(3));
        col.addView(t);
        EditText e = new EditText(this);
        e.setText(value);
        e.setTextColor(ArtKit.INK);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTypeface(Typeface.SERIF);
        e.setMinHeight(dp(44));
        e.setPadding(dp(10), dp(9), dp(10), dp(10));
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
