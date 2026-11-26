# Preview renderer

Renders the app's artwork to PNG and GIF files on a desktop, without a phone.

The art code in `app/src/main/java/.../art/` draws only through the `Pen` interface.
`AwtPen` here implements `Pen` with desktop Java2D; the app has an Android implementation.

    tools/preview/render.sh              # compiles and writes everything to tools/preview/out/
    tools/preview/render.sh sheet hero   # only some: sheet, hero, strip, gif, decor, alarm

Outputs: `hero_*.png` (the window at five times of day, and with the lantern off),
`mochi_sheet.png` (every activity), `alarm_strip.png`, `decor.png` and `decor_night.png`
(mock settings page), `alarm_page.png`, `anim_*.gif`, and `frames/*_samples.png`
(ten stills from each GIF side by side). Draw-call counts are printed as it runs.

Files: `Render` (entry point), `Sheets`, `Heroes`, `Anim`, `Pages` (one per kind of
output) and `GifOut` (animated GIF writer). The lettering on the mock pages is desktop
text standing in for the Android labels.
