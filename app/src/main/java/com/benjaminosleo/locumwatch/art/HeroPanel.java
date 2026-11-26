package com.benjaminosleo.locumwatch.art;

/**
 * Puts the hero together: garden, Mochi and the window interior, in the right order.
 * The panel is {@link Hero#W} x {@link Hero#H} design units; scale the Pen by the screen
 * density before calling.
 *
 * Per frame, back to front:
 * <pre>
 *   [bitmap A]  drawSceneStatic(pen)             cached; redraw after setHour()
 *               drawSceneLive(pen, t, director)  every frame
 *   [bitmap B]  drawFrameStatic(pen)             cached; redraw after setHour() or setArmed()
 *               drawFrameLive(pen, t, director)  every frame
 * </pre>
 */
public final class HeroPanel {
    public final GardenScene garden = new GardenScene();
    public final WindowFrame frame = new WindowFrame();
    public final Mochi mochi = new Mochi();
    private float hour = Palette.DAY;
    private boolean armed = true;

    public HeroPanel() { setHour(Palette.DAY); }

    /** Sets the time of day, 0..24. Allocates: call when the hour has moved a few minutes, then refresh both bitmaps. */
    public void setHour(float hour) {
        this.hour = hour;
        garden.setHour(hour);
        frame.setHour(hour);
        mochi.setLight(garden.palette());
    }

    /** Lantern lit (alarm switched on) or dark. Refresh the frame bitmap afterwards. */
    public void setArmed(boolean armed) { this.armed = armed; frame.setArmed(armed); }

    public float hour() { return hour; }
    public boolean armed() { return armed; }

    /** STATIC. The garden background for the current hour. */
    public void drawSceneStatic(Pen p) { garden.drawBackground(p); }

    /** STATIC. Wall, window frame, sill and props for the current hour and armed state (window opening left clear). */
    public void drawFrameStatic(Pen p) { frame.drawStatic(p); }

    /** ANIMATED. The moving garden and Mochi, drawn over the scene bitmap. */
    public void drawSceneLive(Pen p, double t, MochiDirector director) {
        MochiPose pose = director.pose;
        garden.setSprigPull(director.sprigPull());
        garden.drawLiveBack(p, t);
        boolean close = pose.view == MochiPose.FRONT && pose.scale > 1.25f;
        p.save();
        p.clipRect(Hero.WIN_L, Hero.WIN_T, Hero.WIN_R, Hero.WIN_B + 2f);
        if (!close) mochi.draw(p, pose);
        p.restore();
        garden.drawLiveFront(p, t);
        if (close) {
            // right up at the window: Mochi is in front of the foreground grass
            p.save();
            p.clipRect(Hero.WIN_L, Hero.WIN_T, Hero.WIN_R, Hero.WIN_B + 2f);
            mochi.draw(p, pose);
            p.restore();
        }
    }

    /** ANIMATED. Lantern flame and glints, plus Mochi's paws on the sill when up at the window. Draw over the frame bitmap. */
    public void drawFrameLive(Pen p, double t, MochiDirector director) {
        frame.drawLive(p, t);
        mochi.drawOverSill(p, director.pose);
    }
}
