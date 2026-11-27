package com.benjaminosleo.locumwatch.art;

/**
 * Layout of the hero panel (the cottage window with the garden beyond), in design
 * units equal to Android dp. Everything in the hero is drawn in these coordinates.
 */
public final class Hero {
    private Hero() {}

    /** Size of the whole hero panel. */
    public static final float W = 360f, H = 520f;
    /** The window opening: the garden is only visible inside this rectangle. */
    public static final float WIN_L = 36f, WIN_T = 44f, WIN_R = 324f, WIN_B = 396f;
    /** Top surface of the window sill runs from SILL_TOP down to SILL_FRONT; its front edge ends at SILL_BOTTOM. */
    public static final float SILL_TOP = 396f, SILL_FRONT = 418f, SILL_BOTTOM = 430f;
    /** The lantern on the sill plus a little slack: a 48 x 80 touch target for switching the alarm on and off. */
    public static final float LANTERN_L = 274f, LANTERN_T = 342f, LANTERN_R = 322f, LANTERN_B = 422f;
    /** Where the status board ({@link Decor#statusBoard}) hangs under the sill: 236 x 64, cords tied to the sill's underside. */
    public static final float BOARD_L = 62f, BOARD_T = 430f, BOARD_R = 298f, BOARD_B = 494f;
    /** Strip of wall under the sill (the status board hangs in it). */
    public static final float STATUS_L = 70f, STATUS_T = 446f, STATUS_R = 290f, STATUS_B = 506f;

    /** Horizon line of the garden and the middle of the grassy clearing (Mochi's favourite spot). */
    public static final float HORIZON = 176f, SPOT_X = 180f, SPOT_Y = 304f;
    /** The clearing Mochi runs round: centre and radii of the track. */
    public static final float RING_X = 180f, RING_Y = 306f, RING_RX = 90f, RING_RY = 24f;
    /** Where Mochi stands (ground point) and how big Mochi is when up at the window. */
    public static final float AT_WINDOW_Y = 447f, AT_WINDOW_SCALE = 1.9f;
    /** Tip of the low leafy sprig Mochi stretches up to nibble. */
    public static final float SPRIG_X = 264f, SPRIG_Y = 208f;

    /** Size Mochi is drawn at when standing on the grass at height y (nearer is bigger). */
    public static float depthScale(float y) {
        return Art.clamp(0.84f + (y - 270f) * 0.0042f, 0.7f, 1.6f);
    }
}
