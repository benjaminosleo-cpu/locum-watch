package com.benjaminosleo.locumwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.benjaminosleo.locumwatch.ArtKit.ArtDrawable;
import com.benjaminosleo.locumwatch.art.AlarmArt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-screen alarm: Mochi bouncing at the cottage window, the request on parchment,
 * and a very large STOP button.
 *
 * The buttons are ordinary views laid over the artwork, so they work even before the
 * picture has finished painting, and still work if the picture fails to draw at all.
 */
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

        final AlarmArt art = new AlarmArt(); // used on the main thread only
        art.setHour(ArtKit.hourNow());

        // the request, on parchment
        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(22), dp(18), dp(22), dp(18));
        heading = new TextView(this);
        heading.setTextColor(ArtKit.INK);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        heading.setTypeface(Typeface.SERIF, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        note.addView(heading);
        body = new TextView(this);
        body.setTextColor(ArtKit.INK);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        body.setTypeface(Typeface.SERIF);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, dp(8), 0, 0);
        note.addView(body);
        ScrollView message = new ScrollView(this);
        message.setFillViewport(true);
        message.setBackground(new ArtDrawable(this, 0xFFFBF0D2,
            (p, w, h, pressed, focused) -> ArtKit.decor().parchment(p, 0, 0, w, h, 4)));
        message.addView(note);

        TextView stop = new TextView(this);
        stop.setText("STOP");
        stop.setTextColor(ArtKit.CREAM);
        stop.setTypeface(Typeface.SERIF, Typeface.BOLD);
        stop.setGravity(Gravity.CENTER);
        stop.setLetterSpacing(0.06f);
        stop.setClickable(true);
        stop.setBackground(new ArtDrawable(this, 0xFFC62828,
            (p, w, h, pressed, focused) -> art.stopButton(p, 0, 0, w, h, pressed)));
        stop.setOnClickListener(v -> { AlarmService.stop(this); finish(); });

        TextView open = new TextView(this);
        open.setText("Stop and open WhatsApp");
        open.setTextColor(ArtKit.CREAM);
        open.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        open.setTypeface(Typeface.SERIF, Typeface.BOLD);
        open.setGravity(Gravity.CENTER);
        open.setClickable(true);
        open.setBackground(new ArtDrawable(this, 0xFF2E7D5B,
            (p, w, h, pressed, focused) -> art.secondaryButton(p, 0, 0, w, h, pressed)));
        open.setOnClickListener(v -> {
            AlarmService.stop(this);
            Intent wa = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (wa != null) startActivity(wa);
            finish();
        });

        setContentView(new Scene(this, art, message, stop, open));
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
        heading.setText(test ? "Test alarm" : "Locum request\n" + (sender == null ? "" : sender));
        body.setText(text == null ? "" : text);
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(finished); } catch (Exception ignored) { }
        super.onDestroy();
    }

    private int dp(int v) { return ArtKit.dp(this, v); }

    /** Paints the alarm artwork and places the message and buttons where the artwork leaves room. */
    private static final class Scene extends ViewGroup {
        private static final ExecutorService PAINTER = Executors.newSingleThreadExecutor();
        private static final int PLAIN = 0xFFF2CFA0; // warm wall colour until (or instead of) the picture

        private final AlarmArt art;
        private final View message, stop, open;
        private final AndroidPen pen = new AndroidPen();
        private final float density;
        private final float[] r = new float[4];
        private Bitmap background;
        private boolean painting, broken, running;
        private long started;

        Scene(Context c, AlarmArt art, View message, TextView stop, View open) {
            super(c);
            this.art = art;
            this.message = message;
            this.stop = stop;
            this.open = open;
            density = c.getResources().getDisplayMetrics().density;
            setWillNotDraw(false);
            addView(message);
            addView(stop);
            addView(open);
        }

        @Override protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec), h = MeasureSpec.getSize(hSpec);
            setMeasuredDimension(w, h);
            float uw = w / density, uh = h / density;
            AlarmArt.messageRect(uw, uh, r);
            measure(message);
            AlarmArt.stopRect(uw, uh, r);
            ((TextView) stop).setTextSize(TypedValue.COMPLEX_UNIT_PX, (r[3] - r[1]) * density * 0.40f);
            measure(stop);
            AlarmArt.secondaryRect(uw, uh, r);
            measure(open);
        }

        private void measure(View v) {
            v.measure(MeasureSpec.makeMeasureSpec(Math.round((r[2] - r[0]) * density), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.round((r[3] - r[1]) * density), MeasureSpec.EXACTLY));
        }

        @Override protected void onLayout(boolean changed, int l, int t, int rr, int b) {
            float uw = getWidth() / density, uh = getHeight() / density;
            AlarmArt.messageRect(uw, uh, r);
            place(message);
            AlarmArt.stopRect(uw, uh, r);
            place(stop);
            AlarmArt.secondaryRect(uw, uh, r);
            place(open);
        }

        private void place(View v) {
            int left = Math.round(r[0] * density), top = Math.round(r[1] * density);
            v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
        }

        private final Runnable ticker = new Runnable() {
            @Override public void run() {
                if (!running) return;
                invalidate();
                postDelayed(this, 33);
            }
        };

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            running = true;
            started = SystemClock.elapsedRealtime();
            post(ticker);
        }

        @Override protected void onDetachedFromWindow() {
            running = false;
            removeCallbacks(ticker);
            super.onDetachedFromWindow();
        }

        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (broken || w == 0 || h == 0) {
                canvas.drawColor(PLAIN);
                return;
            }
            if (background == null || background.getWidth() != w || background.getHeight() != h) {
                canvas.drawColor(PLAIN);
                if (!painting) paintBackground(w, h);
                return;
            }
            int save = canvas.save();
            try {
                canvas.drawBitmap(background, 0, 0, null);
                canvas.scale(density, density);
                art.drawLive(pen.begin(canvas), w / density, h / density,
                    (SystemClock.elapsedRealtime() - started) / 1000.0);
                canvas.restoreToCount(save);
            } catch (Throwable t) {
                canvas.restoreToCount(save);
                broken = true;
                ArtKit.fault("alarm page", t);
            }
        }

        private void paintBackground(final int w, final int h) {
            painting = true;
            final float hour = ArtKit.hourNow();
            PAINTER.execute(() -> {
                Bitmap bmp = null;
                Throwable fault = null;
                try {
                    AlarmArt a = new AlarmArt(); // own copy for this thread
                    a.setHour(hour);
                    bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    c.scale(density, density);
                    a.drawStatic(new AndroidPen().begin(c), w / density, h / density);
                } catch (Throwable t) {
                    fault = t;
                }
                final Bitmap done = bmp;
                final Throwable err = fault;
                post(() -> {
                    painting = false;
                    if (err != null) {
                        broken = true;
                        ArtKit.fault("alarm page bitmap", err);
                    } else {
                        background = done;
                    }
                    invalidate();
                });
            });
        }
    }
}
