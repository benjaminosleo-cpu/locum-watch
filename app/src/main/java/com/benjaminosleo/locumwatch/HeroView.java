package com.benjaminosleo.locumwatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

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
 *
 * Everything that touches the artwork runs inside a try/catch: if the picture ever
 * faults, this view shows a plain wall colour and the rest of the app carries on.
 */
final class HeroView extends View {
    private static final float RELIGHT_HOURS = 0.08f; // repaint the bitmaps every ~5 minutes
    private static final int MAX_FAULTS = 3;          // then the picture stays off until the app restarts

    // How often to draw, in milliseconds between frames.
    private static final long LIVELY_MS = 33;     // ~30 a second: alarm, afterglow, or just touched
    private static final long ACTIVE_MS = 42;     // Mochi is up and about
    private static final long CALM_MS = 83;       // ~12 a second: Mochi asleep or loafing
    private static final long DOZE_MS = 1000;     // nobody has touched the phone for 10 minutes
    private static final long SAVER_MS = 500;     // Battery Saver is on
    private static final long HIDDEN_MS = 500;    // scrolled out of view
    private static final long FRESH_MS = 60_000;  // "just touched" lasts a minute
    private static final long UNTOUCHED_MS = 10 * 60_000;
    private static final long AFTERGLOW_MS = 200_000;

    private static final ExecutorService PAINTER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            r.run();
        }, "garden-painter");
        t.setDaemon(true);
        return t;
    });
    private static boolean loggedPaintTime;

    private final Prefs prefs;
    private final PowerManager power;
    private final AndroidPen pen = new AndroidPen();
    private final Rect visible = new Rect();
    private final int slop;

    // The artwork objects belong to the main thread only (the painter thread makes its own).
    private HeroPanel panel;
    private MochiDirector director;

    private Bitmap sceneBmp, frameBmp;
    private float bmpHour = -99f;
    private boolean bmpArmed;
    private int bmpWidth;
    private boolean painting, broken, running;
    private int faults;

    private float hour = ArtKit.hourNow();
    private float panelHour = -99f;
    private boolean armed, saver;
    private long lastFrame, lastSlowCheck, lastInvalidate;
    private long lastInteraction = SystemClock.elapsedRealtime();
    private double t;
    private long alarmEndedAt;
    private Runnable onLanternTap;
    private float downX, downY;
    private boolean downOnLantern, downOnMochi;

    HeroView(Context c, Prefs prefs) {
        super(c);
        this.prefs = prefs;
        this.power = c.getSystemService(PowerManager.class);
        this.slop = ViewConfiguration.get(c).getScaledTouchSlop();
    }

    /** Called when the lantern on the sill is tapped (it shows whether the alarm is on). */
    void setOnLanternTap(Runnable r) { onLanternTap = r; }

    /** Re-read the on/off state straight away (after it was changed). */
    void refreshNow() {
        lastSlowCheck = 0;
        invalidate();
    }

    /** Someone is using the phone: animate at full rate for the next minute. */
    void noteInteraction() { lastInteraction = SystemClock.elapsedRealtime(); }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = getDefaultSize(getSuggestedMinimumWidth(), wSpec);
        setMeasuredDimension(w, Math.round(w * Hero.H / Hero.W));
    }

    // ---- animation clock ----
    private long interval() {
        long now = SystemClock.elapsedRealtime();
        if (!getLocalVisibleRect(visible)) return HIDDEN_MS;
        if (AlarmService.isRinging()
                || (alarmEndedAt > 0 && System.currentTimeMillis() - alarmEndedAt < AFTERGLOW_MS)) return LIVELY_MS;
        if (saver) return SAVER_MS;
        long untouched = now - lastInteraction;
        if (untouched > UNTOUCHED_MS) return DOZE_MS;
        if (untouched < FRESH_MS) return LIVELY_MS;
        int act = director == null ? MochiDirector.SLEEP : director.activity();
        return act == MochiDirector.SLEEP || act == MochiDirector.REST ? CALM_MS : ACTIVE_MS;
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long iv = interval();
            long now = SystemClock.uptimeMillis();
            if (now - lastInvalidate >= iv - 6) {
                lastInvalidate = now;
                invalidate();
            }
            // Fast rates step in time with the screen's own refresh; slow rates just sleep.
            if (iv <= 100) postOnAnimation(this);
            else postDelayed(this, iv);
        }
    };

    private void setRunning(boolean on) {
        on = on && !broken;
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
        if (visibility == VISIBLE) {
            noteInteraction();
            if (broken && faults < MAX_FAULTS) broken = false; // a one-off fault gets another chance
        }
        setRunning(visibility == VISIBLE && isAttachedToWindow());
    }

    private void fault(String what, Throwable e) {
        broken = true;
        faults++;
        setRunning(false);
        ArtKit.fault(what, e);
        try {
            prefs.addLog("The garden picture hit a fault and was switched off (the alarm is unaffected): " + e);
        } catch (Throwable ignored) { }
        invalidate();
    }

    // ---- drawing ----
    @Override protected void onDraw(Canvas canvas) {
        if (broken || getWidth() == 0) {
            canvas.drawColor(ArtKit.WALL_FALLBACK);
            return;
        }
        int save = canvas.save();
        try {
            if (panel == null) {
                panel = new HeroPanel();
                director = new MochiDirector(System.currentTimeMillis());
                director.reset(hour);
            }
            long now = SystemClock.elapsedRealtime();
            if (now - lastSlowCheck > 1000) {
                lastSlowCheck = now;
                hour = ArtKit.hourNow();
                armed = prefs.isArmed();
                alarmEndedAt = prefs.alarmEndedAt();
                saver = power != null && power.isPowerSaveMode();
                if (Math.abs(hour - panelHour) > RELIGHT_HOURS) {
                    panel.setHour(hour);
                    panelHour = hour;
                }
                panel.setArmed(armed);
                if (!painting) {
                    boolean all = sceneBmp == null || bmpWidth != getWidth() || Math.abs(hour - bmpHour) > RELIGHT_HOURS;
                    if (all || bmpArmed != armed) repaintBitmaps(getWidth(), getHeight(), hour, armed, all);
                }
            }
            if (sceneBmp == null || frameBmp == null) {
                canvas.drawColor(ArtKit.WALL_FALLBACK);
                canvas.restoreToCount(save);
                return;
            }

            double dt = lastFrame == 0 ? 0 : Math.min(2.0, (now - lastFrame) / 1000.0);
            lastFrame = now;
            t += dt;
            boolean ringing = AlarmService.isRinging();
            double since = ringing || alarmEndedAt == 0 ? MochiDirector.NEVER
                : Math.max(0, (System.currentTimeMillis() - alarmEndedAt) / 1000.0);

            float s = getWidth() / Hero.W;
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
            canvas.drawColor(ArtKit.WALL_FALLBACK);
            fault("hero", e);
        }
    }

    /**
     * Paints the detailed layers off the main thread, then swaps them in. When only the
     * lantern changed, just the frame layer is repainted (about a third of the work).
     */
    private void repaintBitmaps(final int w, final int h, final float atHour, final boolean lit, final boolean sceneToo) {
        painting = true;
        PAINTER.execute(() -> {
            Bitmap scene = null, frame = null;
            Throwable fault = null;
            long started = SystemClock.elapsedRealtime();
            try {
                HeroPanel p = new HeroPanel(); // own copy: the live one is in use on the main thread
                p.setHour(atHour);
                p.setArmed(lit);
                float s = w / Hero.W;
                Canvas c;
                if (sceneToo) {
                    scene = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    c = new Canvas(scene);
                    c.scale(s, s);
                    p.drawSceneStatic(new AndroidPen().begin(c));
                }
                frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                c = new Canvas(frame);
                c.scale(s, s);
                p.drawFrameStatic(new AndroidPen().begin(c));
            } catch (Throwable e) {
                fault = e;
            }
            final long took = SystemClock.elapsedRealtime() - started;
            final Bitmap fs = scene, ff = frame;
            final Throwable err = fault;
            post(() -> {
                painting = false;
                if (err != null) {
                    fault("hero bitmaps", err);
                    return;
                }
                if (fs != null) {
                    sceneBmp = fs;
                    bmpHour = atHour;
                    bmpWidth = w;
                }
                frameBmp = ff;
                bmpArmed = lit;
                if (sceneToo && !loggedPaintTime) {
                    loggedPaintTime = true;
                    prefs.addDiag("Garden picture painted in " + took + " ms");
                }
                invalidate();
            });
        });
    }

    // ---- taps: the lantern is a switch, and Mochi likes attention ----
    @Override public boolean onTouchEvent(MotionEvent e) {
        float s = getWidth() / Hero.W;
        if (s <= 0) return false;
        float x = e.getX() / s, y = e.getY() / s;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                noteInteraction();
                downX = e.getX();
                downY = e.getY();
                downOnLantern = onLanternTap != null
                    && x >= Hero.LANTERN_L && x <= Hero.LANTERN_R && y >= Hero.LANTERN_T && y <= Hero.LANTERN_B;
                downOnMochi = false;
                if (!downOnLantern && !broken && director != null) {
                    try {
                        downOnMochi = director.hitTest(x, y);
                    } catch (Throwable t) {
                        fault("hit test", t);
                    }
                }
                return downOnLantern || downOnMochi; // anywhere else, let the page scroll
            case MotionEvent.ACTION_UP:
                // only a tap counts: a finger that slid (starting a scroll) does nothing
                if (Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop) return true;
                if (downOnLantern) {
                    onLanternTap.run();
                } else if (downOnMochi && !broken && director != null) {
                    try {
                        director.poke();
                    } catch (Throwable t) {
                        fault("poke", t);
                    }
                }
                return true;
            default:
                return true;
        }
    }
}
