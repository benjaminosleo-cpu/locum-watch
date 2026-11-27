package com.benjaminosleo.locumwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
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
 * Full-screen alarm: Mochi racing about outside the cottage window, the request on
 * parchment, and a very large STOP button.
 *
 * The buttons are ordinary views laid over the artwork, so they work even before the
 * picture has finished painting, and still work if the picture fails to draw at all.
 * Nothing on this page is allowed to stop the STOP button appearing: if any part of the
 * artwork misbehaves (or the alarm had to be restarted by Android), the page falls back
 * to plain colours with the same message and buttons.
 */
public class AlarmActivity extends Activity {
    private static final int[] BODY_SIZES = {19, 17, 15}; // sp, largest that fits is used

    private TextView caption, sender, body;

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

        // The artwork is optional. "Plain" is asked for when Android had to restart the alarm.
        AlarmArt made = null;
        if (!getIntent().getBooleanExtra(AlarmService.EXTRA_PLAIN, false)) {
            try {
                made = new AlarmArt(); // used on the main thread only
                made.setHour(ArtKit.hourNow());
            } catch (Throwable t) {
                made = null;
                ArtKit.fault("alarm page setup", t);
            }
        }
        final AlarmArt art = made;

        // the request, on parchment: who it is from, then the message as large as will fit
        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(20), dp(14), dp(20), dp(16));
        caption = label(13, ArtKit.INK_SOFT, false);
        caption.setLetterSpacing(0.08f);
        note.addView(caption);
        sender = label(18, ArtKit.INK, true);
        sender.setMaxLines(1);
        sender.setEllipsize(TextUtils.TruncateAt.END);
        note.addView(sender);
        body = label(BODY_SIZES[0], ArtKit.INK, false);
        body.setPadding(0, dp(6), 0, 0);
        body.setLineSpacing(dp(2), 1f);
        note.addView(body);
        ScrollView message = new ScrollView(this);
        message.setFillViewport(true);
        message.setOverScrollMode(View.OVER_SCROLL_NEVER);
        // a long message scrolls: fade the cut edge and keep the scroll bar showing so that is obvious
        message.setVerticalFadingEdgeEnabled(true);
        message.setFadingEdgeLength(dp(28));
        message.setScrollbarFadingEnabled(false);
        message.setBackground(art == null ? new ColorDrawable(0xFFFBF0D2)
            : new ArtDrawable(this, 0xFFFBF0D2, (p, w, h, pressed, focused) -> ArtKit.decor().parchment(p, 0, 0, w, h, 4)));
        message.addView(note);

        TextView stop = label(40, ArtKit.CREAM, true);
        stop.setText("STOP");
        stop.setLetterSpacing(0.06f);
        stop.setClickable(true);
        stop.setBackground(art == null ? new ColorDrawable(0xFFC62828)
            : new ArtDrawable(this, 0xFFC62828, (p, w, h, pressed, focused) -> art.stopButton(p, 0, 0, w, h, pressed)));
        stop.setOnClickListener(v -> {
            AlarmService.stop(this);
            // back to the cottage window, where Mochi is still pleased with herself
            try {
                startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } catch (Exception ignored) { }
            finish();
        });

        TextView open = label(15, ArtKit.CREAM, true);
        open.setText("Stop and open WhatsApp");
        open.setClickable(true);
        open.setBackground(art == null ? new ColorDrawable(0xFF2E7D5B)
            : new ArtDrawable(this, 0xFF2E7D5B, (p, w, h, pressed, focused) -> art.secondaryButton(p, 0, 0, w, h, pressed)));
        open.setOnClickListener(v -> {
            AlarmService.stop(this);
            try {
                Intent wa = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                if (wa != null) startActivity(wa);
            } catch (Exception ignored) { }
            finish();
        });

        setContentView(new Scene(this, art, message, note, body, stop, open));
        show(getIntent());
        IntentFilter f = new IntentFilter(AlarmService.ACTION_FINISHED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(finished, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(finished, f);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        show(intent);
    }

    private void show(Intent i) {
        boolean test = i.getBooleanExtra(AlarmService.EXTRA_TEST, false);
        String from = i.getStringExtra(AlarmService.EXTRA_SENDER);
        String text = i.getStringExtra(AlarmService.EXTRA_TEXT);
        caption.setText(test ? "TEST ALARM" : "LOCUM REQUEST");
        sender.setText(test || from == null ? "" : from);
        sender.setVisibility(test || from == null || from.isEmpty() ? View.GONE : View.VISIBLE);
        body.setText(text == null ? "" : text);
    }

    @Override
    protected void onDestroy() {
        try { unregisterReceiver(finished); } catch (Exception ignored) { }
        super.onDestroy();
    }

    private TextView label(int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(Typeface.SERIF, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private int dp(int v) { return ArtKit.dp(this, v); }

    /** Paints the alarm artwork and places the message and buttons where the artwork leaves room. */
    private static final class Scene extends ViewGroup {
        private static final ExecutorService PAINTER = Executors.newSingleThreadExecutor();
        private static final int PLAIN = 0xFFF2CFA0; // warm wall colour until (or instead of) the picture
        private static final int MAX_FAULTS = 3;
        private static final int MESSAGE = 0, STOP = 1, SECONDARY = 2;

        private final AlarmArt art; // null: plain page
        private final View message, note, stop, open;
        private final TextView body;
        private final AndroidPen pen = new AndroidPen();
        private final float density;
        private final float[] r = new float[4];
        private Bitmap background;
        private boolean painting, broken, attached, visible, ticking, rectsBroken;
        private int faults;
        private long started;
        private CharSequence fitText;
        private int fitW, fitH;

        Scene(Context c, AlarmArt art, View message, View note, TextView body, TextView stop, View open) {
            super(c);
            this.art = art;
            this.message = message;
            this.note = note;
            this.body = body;
            this.stop = stop;
            this.open = open;
            broken = art == null;
            density = c.getResources().getDisplayMetrics().density;
            setWillNotDraw(false);
            setClipChildren(false); // button shadows and the parchment's edge spill a little
            addView(message);
            addView(stop);
            addView(open);
        }

        /** Where a piece goes, in art units. Falls back to a simple stack if the artwork's sums fail. */
        private void rect(int which, float uw, float uh) {
            if (!rectsBroken) {
                try {
                    if (which == MESSAGE) AlarmArt.messageRect(uw, uh, r);
                    else if (which == STOP) AlarmArt.stopRect(uw, uh, r);
                    else AlarmArt.secondaryRect(uw, uh, r);
                    if (r[2] - r[0] >= 120 && r[3] - r[1] >= (which == SECONDARY ? 0 : 60)
                            && r[0] >= 0 && r[1] >= 0 && r[2] <= uw + 0.5f && r[3] <= uh + 0.5f) return;
                    throw new IllegalStateException("alarm page layout off-screen: piece " + which + " on " + uw + "x" + uh);
                } catch (Throwable t) {
                    rectsBroken = true;
                    ArtKit.fault("alarm page layout", t);
                    requestLayout();
                }
            }
            r[0] = 18;
            r[2] = uw - 18;
            if (which == MESSAGE) { r[1] = uh * 0.06f; r[3] = uh * 0.58f; }
            else if (which == STOP) { r[1] = uh * 0.61f; r[3] = uh * 0.85f; }
            else { r[1] = uh * 0.88f; r[3] = uh * 0.96f; }
        }

        @Override protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec), h = MeasureSpec.getSize(hSpec);
            setMeasuredDimension(w, h);
            float uw = w / density, uh = h / density;
            rect(MESSAGE, uw, uh);
            fitBody(Math.round((r[2] - r[0]) * density), Math.round((r[3] - r[1]) * density));
            measure(message);
            rect(STOP, uw, uh);
            ((TextView) stop).setTextSize(TypedValue.COMPLEX_UNIT_PX, (r[3] - r[1]) * density * 0.36f);
            measure(stop);
            rect(SECONDARY, uw, uh);
            measure(open);
        }

        /** Use the largest message text that fits without scrolling; the smallest size scrolls if it must. */
        private void fitBody(int width, int height) {
            CharSequence text = body.getText();
            if (width == fitW && height == fitH && TextUtils.equals(text, fitText)) return;
            fitW = width;
            fitH = height;
            fitText = text == null ? null : text.toString();
            for (int sp : BODY_SIZES) {
                body.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
                note.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                if (note.getMeasuredHeight() <= height) break;
            }
        }

        private void measure(View v) {
            v.measure(MeasureSpec.makeMeasureSpec(Math.max(0, Math.round((r[2] - r[0]) * density)), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.max(0, Math.round((r[3] - r[1]) * density)), MeasureSpec.EXACTLY));
        }

        @Override protected void onLayout(boolean changed, int l, int t, int rr, int b) {
            float uw = getWidth() / density, uh = getHeight() / density;
            rect(MESSAGE, uw, uh);
            place(message);
            rect(STOP, uw, uh);
            place(stop);
            rect(SECONDARY, uw, uh);
            place(open);
        }

        private void place(View v) {
            int left = Math.round(r[0] * density), top = Math.round(r[1] * density);
            v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
        }

        // ---- animation: only while the page is actually on screen ----
        private final Runnable ticker = new Runnable() {
            @Override public void run() {
                if (!ticking) return;
                invalidate();
                postOnAnimationDelayed(this, 33);
            }
        };

        private void updateTicker() {
            boolean want = attached && visible && !broken;
            if (want == ticking) return;
            ticking = want;
            removeCallbacks(ticker);
            if (want) post(ticker);
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached = true;
            visible = getWindowVisibility() == VISIBLE;
            started = SystemClock.elapsedRealtime();
            updateTicker();
        }

        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            visible = visibility == VISIBLE;
            // a picture that failed gets another go each time the page comes back, a few times at most
            if (visible && broken && art != null && faults < MAX_FAULTS) {
                broken = false;
                invalidate();
            }
            updateTicker();
        }

        @Override protected void onDetachedFromWindow() {
            attached = false;
            updateTicker();
            super.onDetachedFromWindow();
        }

        private void fail(String where, Throwable t) {
            broken = true;
            faults++;
            background = null;
            ArtKit.fault(where, t);
            updateTicker();
            invalidate();
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
                canvas.drawColor(PLAIN);
                fail("alarm page", t);
            }
        }

        private void paintBackground(final int w, final int h) {
            painting = true;
            final float hour = ArtKit.hourNow();
            try {
                PAINTER.execute(() -> {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
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
                        if (err != null) fail("alarm page bitmap", err);
                        else background = done;
                        invalidate();
                    });
                });
            } catch (Throwable t) {
                painting = false;
                fail("alarm page painter", t);
            }
        }
    }
}
