package com.benjaminosleo.locumwatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.benjaminosleo.locumwatch.art.Decor;
import com.benjaminosleo.locumwatch.art.Pen;

import java.util.Calendar;

/**
 * Glue between the cottage artwork (the pure-Java "art" package) and Android views:
 * shared colours, the time of day, drawables that paint a piece of decor, and the
 * small custom controls (toggle, slider, wooden button).
 *
 * All drawing is wrapped so that a fault in the artwork can never stop a control working.
 */
final class ArtKit {
    private static final String TAG = "LocumWatch";

    static final int INK = 0xFF4A3424;        // dark brown text on parchment
    static final int INK_SOFT = 0xFF7A5F48;   // secondary text
    static final int CREAM = 0xFFFFF4DC;      // text on dark wood
    static final int WALL_FALLBACK = 0xFFF3E6C8;
    static final int GOOD = 0xFF2F6B2F, BAD = 0xFF9E2B1E;

    private ArtKit() {}

    /** Local time as a decimal hour, 0..24: drives the lighting of every piece of art. */
    static float hourNow() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60f;
    }

    // ---- shared decor painter (UI thread only) ----
    private static Decor decor;
    private static float decorHour = -99f;
    /** Changes whenever the decor lighting changes, so cached bitmaps know to rebuild. */
    static int decorGeneration;

    static Decor decor() {
        float h = hourNow();
        if (decor == null) decor = new Decor();
        if (Math.abs(h - decorHour) > 0.25f) {
            decor.setHour(h);
            decorHour = h;
            decorGeneration++;
        }
        return decor;
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static void fault(String where, Throwable t) {
        Log.e(TAG, "art fault in " + where, t);
    }

    /** Paints one piece of decor into a rectangle w by h units (dp). */
    interface Piece {
        void paint(Pen p, float w, float h, boolean pressed, boolean focused);
    }

    /** A Drawable that paints a decor piece, scaled from dp units to pixels. */
    static final class ArtDrawable extends Drawable {
        private final Piece piece;
        private final float density;
        private final int fallback;
        private final AndroidPen pen = new AndroidPen();
        private final Paint plain = new Paint();
        private boolean pressed, focused, broken;

        ArtDrawable(Context c, int fallbackColor, Piece piece) {
            this.piece = piece;
            this.fallback = fallbackColor;
            this.density = c.getResources().getDisplayMetrics().density;
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            if (!broken) {
                int save = canvas.save();
                try {
                    canvas.translate(b.left, b.top);
                    canvas.scale(density, density);
                    pen.begin(canvas);
                    piece.paint(pen, b.width() / density, b.height() / density, pressed, focused);
                    canvas.restoreToCount(save);
                    return;
                } catch (Throwable t) {
                    broken = true;
                    fault("drawable", t);
                    canvas.restoreToCount(save);
                }
            }
            plain.setColor(fallback);
            canvas.drawRect(b, plain);
        }

        @Override public boolean isStateful() { return true; }

        @Override protected boolean onStateChange(int[] state) {
            boolean p = false, f = false;
            for (int s : state) {
                if (s == android.R.attr.state_pressed) p = true;
                if (s == android.R.attr.state_focused) f = true;
            }
            if (p == pressed && f == focused) return false;
            pressed = p;
            focused = f;
            invalidateSelf();
            return true;
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /**
     * The cottage wall behind the settings. The wall pattern repeats exactly every
     * Decor.WALL_PERIOD units, so one small bitmap is tiled down the whole page.
     */
    static final class WallDrawable extends Drawable {
        private final float density;
        private final float startOffset;
        private final Paint paint = new Paint();
        private final Paint plain = new Paint();
        private final Matrix shift = new Matrix();
        private Bitmap tile;
        private int tileWidth, generation = -1;
        private boolean broken;

        /** @param startOffsetUnits how far down the wall this view starts (the hero is 520 units tall) */
        WallDrawable(Context c, float startOffsetUnits) {
            density = c.getResources().getDisplayMetrics().density;
            startOffset = startOffsetUnits;
            plain.setColor(WALL_FALLBACK);
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            Decor d = decor();
            if (!broken && (tile == null || tileWidth != b.width() || generation != decorGeneration)) {
                try {
                    int h = Math.round(Decor.WALL_PERIOD * density);
                    Bitmap bmp = Bitmap.createBitmap(b.width(), h, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    c.scale(density, density);
                    d.wall(new AndroidPen().begin(c), b.width() / density, Decor.WALL_PERIOD, startOffset);
                    tile = bmp;
                    tileWidth = b.width();
                    generation = decorGeneration;
                    BitmapShader sh = new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.REPEAT);
                    shift.setTranslate(b.left, b.top);
                    sh.setLocalMatrix(shift);
                    paint.setShader(sh);
                } catch (Throwable t) {
                    broken = true;
                    fault("wall", t);
                }
            }
            canvas.drawRect(b, broken || tile == null ? plain : paint);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    // ---- controls ----

    /** A wooden plank button. Style is one of Decor.BUTTON_*. */
    static final class PlankButton extends TextView {
        private int style = Decor.BUTTON_NORMAL;

        PlankButton(Context c, String label) {
            super(c);
            setText(label);
            setGravity(Gravity.CENTER);
            setTextColor(INK);
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            setTypeface(Typeface.SERIF, Typeface.BOLD);
            setClickable(true);
            setFocusable(true);
            setMinHeight(dp(c, 48));
            int pad = dp(c, 18);
            setPadding(pad, dp(c, 6), pad, dp(c, 9));
            setBackground(new ArtDrawable(c, 0xFFC49A6C,
                (p, w, h, pressed, focused) -> decor().plankButton(p, 0, 0, w, h, style, pressed)));
        }

        void setStyle(int s) {
            if (s != style) {
                style = s;
                invalidate();
            }
        }
    }

    /** On/off toggle: a flame when on, a moon when off. */
    static final class ToggleView extends View {
        interface Listener { void onChanged(boolean on); }

        private boolean on;
        private Listener listener;
        private final AndroidPen pen = new AndroidPen();
        private final Paint plain = new Paint();
        private final float density;

        ToggleView(Context c) {
            super(c);
            density = c.getResources().getDisplayMetrics().density;
            setClickable(true);
            setOnClickListener(v -> {
                on = !on;
                invalidate();
                if (listener != null) listener.onChanged(on);
            });
        }

        void setListener(Listener l) { listener = l; }
        boolean isOn() { return on; }

        /** Sets the state without calling the listener. */
        void setOn(boolean v) {
            if (v != on) {
                on = v;
                invalidate();
            }
        }

        @Override protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(dp(getContext(), 64), dp(getContext(), 40));
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth() / density, h = getHeight() / density;
            int save = canvas.save();
            try {
                canvas.scale(density, density);
                // the art is drawn 52 x 28 units, centred in the larger touch area
                decor().toggle(pen.begin(canvas), (w - 52) / 2f, (h - 28) / 2f, (w + 52) / 2f, (h + 28) / 2f, on);
                canvas.restoreToCount(save);
            } catch (Throwable t) {
                canvas.restoreToCount(save);
                fault("toggle", t);
                plain.setColor(on ? 0xFFE0A030 : 0xFF6B5A4C);
                canvas.drawRect(0, 0, getWidth(), getHeight(), plain);
            }
        }
    }

    /** A stepped slider: a wooden track that fills with a leafy vine, and a round thumb. */
    static final class VineSlider extends View {
        interface Listener {
            void onMoved(int step);
            void onReleased(int step);
        }

        private final int steps; // positions 0..steps
        private int step;
        private boolean dragging;
        private Listener listener;
        private final AndroidPen pen = new AndroidPen();
        private final Paint plain = new Paint();
        private final float density;
        private static final float PAD = 22f; // units of track inset each side

        VineSlider(Context c, int steps, int initial) {
            super(c);
            this.steps = Math.max(1, steps);
            this.step = Math.max(0, Math.min(this.steps, initial));
            density = c.getResources().getDisplayMetrics().density;
        }

        void setListener(Listener l) { listener = l; }
        int step() { return step; }

        @Override protected void onMeasure(int wSpec, int hSpec) {
            setMeasuredDimension(MeasureSpec.getSize(wSpec), dp(getContext(), 48));
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    dragging = true;
                    move(e.getX());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    move(e.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                    move(e.getX());
                    // fall through
                case MotionEvent.ACTION_CANCEL:
                    dragging = false;
                    invalidate();
                    if (listener != null) listener.onReleased(step);
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }

        private void move(float xPx) {
            float w = getWidth() / density, x = xPx / density;
            float f = Math.max(0f, Math.min(1f, (x - PAD) / Math.max(1f, w - 2 * PAD)));
            int s = Math.round(f * steps);
            if (s != step) {
                step = s;
                if (listener != null) listener.onMoved(step);
            }
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth() / density, h = getHeight() / density;
            float f = step / (float) steps, mid = h / 2f;
            int save = canvas.save();
            try {
                canvas.scale(density, density);
                pen.begin(canvas);
                decor().sliderTrack(pen, PAD, mid - 14, w - PAD, mid + 14, f);
                decor().sliderThumb(pen, PAD + (w - 2 * PAD) * f, mid, 13, dragging);
                canvas.restoreToCount(save);
            } catch (Throwable t) {
                canvas.restoreToCount(save);
                fault("slider", t);
                plain.setColor(0xFF8A6A48);
                canvas.drawRect(PAD * density, getHeight() / 2f - 6, getWidth() - PAD * density, getHeight() / 2f + 6, plain);
                plain.setColor(0xFF4E7F3A);
                canvas.drawCircle((PAD + (w - 2 * PAD) * f) * density, getHeight() / 2f, 13 * density, plain);
            }
        }
    }
}
