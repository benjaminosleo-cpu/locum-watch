package com.benjaminosleo.locumwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Full-screen alarm: shows the request and a big STOP button. */
public class AlarmActivity extends Activity {
    private TextView heading;
    private TextView body;

    private final BroadcastReceiver finished = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { finish(); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(140, 20, 20));
        int pad = dp(24);
        root.setPadding(pad, pad * 2, pad, pad);

        heading = new TextView(this);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(heading);

        ScrollView sv = new ScrollView(this);
        body = new TextView(this);
        body.setTextColor(Color.WHITE);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        body.setPadding(0, dp(16), 0, dp(16));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button stop = new Button(this);
        stop.setText("STOP ALARM");
        stop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        stop.setOnClickListener(v -> { AlarmService.stop(this); finish(); });
        root.addView(stop, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140)));

        Button open = new Button(this);
        open.setText("Stop and open WhatsApp");
        open.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        open.setGravity(Gravity.CENTER);
        open.setOnClickListener(v -> {
            AlarmService.stop(this);
            Intent wa = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (wa != null) startActivity(wa);
            finish();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72));
        lp.topMargin = dp(12);
        root.addView(open, lp);

        setContentView(root);
        show(getIntent());
        IntentFilter f = new IntentFilter(AlarmService.ACTION_FINISHED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(finished, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(finished, f);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        show(intent);
    }

    private void show(Intent i) {
        boolean test = i.getBooleanExtra(AlarmService.EXTRA_TEST, false);
        String sender = i.getStringExtra(AlarmService.EXTRA_SENDER);
        String text = i.getStringExtra(AlarmService.EXTRA_TEXT);
        heading.setText(test ? "TEST ALARM" : "LOCUM REQUEST\n" + (sender == null ? "" : sender));
        body.setText(text == null ? "" : text);
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(finished); } catch (Exception ignored) { }
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
