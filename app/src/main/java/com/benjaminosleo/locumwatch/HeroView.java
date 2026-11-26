package com.benjaminosleo.locumwatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.benjaminosleo.locumwatch.art.Hero;
import com.benjaminosleo.locumwatch.art.HeroPanel;
import com.benjaminosleo.locumwatch.art.MochiDirector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The window onto the garden, with Mochi the capybara.
 *
 * The detailed, unchanging parts (garden background; window frame and sill) are painted
 * once into two bitmaps on a background thread and repainted only when the light changes
 * (every few minutes) or the lantern is lit or put out. Each frame then draws: garden
 * bitmap, moving garden + Mochi, frame bitmap, flame and paws.
 */
final class HeroView extends View {
    private static final long FRAME_MS = 33;          // about 30 frames a second
    private static final long IDLE_MS = 500;          // when scrolled out of view
    private static final float RELIGHT_HOURS = 0.08f; // repaint the bitmaps every ~5 minutes

    private static final ExecutorService PAINTER = Executors.newSingleThreadExecutor();

    private final Prefs prefs;
    private final AndroidPen pen = new AndroidPen();
    private final HeroPanel panel = new HeroPanel();
    private final MochiDirector director = new MochiDirector(System.currentTimeMillis());
    private final Rect visible = new Rect();

    private Bitmap sceneBmp, frameBmp;
    private float bmpHour = -99f;
    private boolean bmpArmed;
    private int bmpWidth;
    private boolean painting, broken, running;

    private float hour = ArtKit.hourNow();
    private float panelHour = -99f;
    private boolean armed;
    private long lastFrame, lastSlowCheck;
    private double t;
    private long alarmEndedAt;
    private Runnable onLanternTap;

    HeroView(Context c, Prefs prefs) {
        super(c);
        this.prefs = prefs;
        director.reset(hour);
    }

    /** Re-read the on/off state straight away (after it was changed). */
    void refreshNow() { lastSlowCheck = 0; invalidate(); }

    /** Called when the lantern on the sill is tapped (it shows whether the alarm is on). */
    void setOnLanternTap(Runnable r) { onLanternTap = r; }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, Math.round(w * Hero.H / Hero.W));
    }

    // ---- animation clock ----
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;
            invalidate();
            postDelayed(this, getLocalVisibleRect(visible) ? FRAME_MS : IDLE_MS);
        }
    };

    private void setRunning(boolean on) {
        if (on == running) return;
        running = on;
        removeCallbacks(ticker);
        if (on) {
            lastFrame = 0;
            post(ticker);
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setRunning(getWindowVisibility() == VISIBLE);
    }

    @Override protected void onDetachedFromWindow() {
        setRunning(false);
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        setRunning(visibility == VISIBLE && isAttachedToWindow());
    }

    // ---- drawing ----
    @Override protected void onDraw(Canvas canvas) {
        if (broken || getWidth() == 0) {
            canvas.drawColor(ArtKit.WALL_FALLBACK);
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastSlowCheck > 1000) {
            lastSlowCheck = now;
            hour = ArtKit.hourNow();
            armed = prefs.isArmed();
            alarmEndedAt = prefs.alarmEndedAt();
            if (Math.abs(hour - panelHour) > RELIGHT_HOURS) {
                panel.setHour(hour);
                panelHour = hour;
            }
            panel.setArmed(armed);
            if (!painting && (sceneBmp == null || bmpWidth != getWidth() || bmpArmed != armed
                    || Math.abs(hour - bmpHour) > RELIGHT_HOURS)) {
                repaintBitmaps(getWidth(), getHeight(), hour, armed);
            }
        }
        if (sceneBmp == null) {
            canvas.drawColor(ArtKit.WALL_FALLBACK);
            return;
        }

        double dt = lastFrame == 0 ? 0 : Math.min(2.0, (now - lastFrame) / 1000.0);
        lastFrame = now;
        t += dt;
        boolean ringing = AlarmService.isRinging();
        double since = ringing || alarmEndedAt == 0 ? MochiDirector.NEVER
            : Math.max(0, (System.currentTimeMillis() - alarmEndedAt) / 1000.0);

        float s = getWidth() / Hero.W;
        int save = canvas.save();
        try {
            director.step(dt, hour, ringing, since);
            canvas.drawBitmap(sceneBmp, 0, 0, null);
            canvas.save();
            canvas.scale(s, s);
            panel.drawSceneLive(pen.begin(canvas), t, director);
            canvas.restore();
            canvas.drawBitmap(frameBmp, 0, 0, null);
            canvas.scale(s, s);
            panel.drawFrameLive(pen.begin(canvas), t, director);
            canvas.restoreToCount(save);
        } catch (Throwable e) {
            canvas.restoreToCount(save);
            broken = true;
            ArtKit.fault("hero", e);
            prefs.addLog("The garden picture hit a fault and was switched off (alarm unaffected): " + e);
        }
    }

    /** Paints the two detailed layers off the main thread, then swaps them in. */
    private void repaintBitmaps(final int w, final int h, final float atHour, final boolean lit) {
        painting = true;
        PAINTER.execute(() -> {
            Bitmap scene = null, frame = null;
            Throwable fault = null;
            try {
                HeroPanel p = new HeroPanel(); // own copy: the live one is in use on the main thread
                p.setHour(atHour);
                p.setArmed(lit);
                float s = w / Hero.W;
                scene = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(scene);
                c.scale(s, s);
                p.drawSceneStatic(new AndroidPen().begin(c));
                frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                c = new Canvas(frame);
                c.scale(s, s);
                p.drawFrameStatic(new AndroidPen().begin(c));
            } catch (Throwable e) {
                fault = e;
            }
            final Bitmap fs = scene, ff = frame;
            final Throwable err = fault;
            post(() -> {
                painting = false;
                if (err != null) {
                    broken = true;
                    ArtKit.fault("hero bitmaps", err);
                    prefs.addLog("The garden picture could not be painted (alarm unaffected): " + err);
                } else {
                    sceneBmp = fs;
                    frameBmp = ff;
                    bmpWidth = w;
                    bmpHour = atHour;
                    bmpArmed = lit;
                }
                invalidate();
            });
        });
    }

    // ---- the lantern is a switch ----
    @Override public boolean onTouchEvent(MotionEvent e) {
        float s = getWidth() / Hero.W;
        float x = e.getX() / s, y = e.getY() / s;
        boolean onLantern = x > 262 && x < 330 && y > 335 && y < 432;
        if (!onLantern || onLanternTap == null) return false;
        if (e.getActionMasked() == MotionEvent.ACTION_UP) onLanternTap.run();
        return true;
    }
}
