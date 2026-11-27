# Preview renderer

Renders the app's artwork to PNG and GIF files on a desktop, without a phone.

The art code in `app/src/main/java/.../art/` draws only through the `Pen` interface.
`AwtPen` here implements `Pen` with desktop Java2D; the app has an Android implementation.

    tools/preview/render.sh              # compiles and writes everything to tools/preview/out/
    tools/preview/render.sh sheet hero   # only some of: sheet hero strip gif decor alarm boards poke film check

Outputs:

- `hero_*.png`: the window at five times of day, and with the lantern off
- `mochi_sheet.png`: every activity; `poke.png`: both tap reactions
- `alarm_strip.png`, `alarm_page.png`: the alarm run in the hero, and the alarm page
- `decor.png`, `decor_night.png`, `status_boards.png`: the settings pieces and the three status boards
- `anim_*.gif`, and `frames/*_samples.png` (ten stills from each GIF side by side)
- `frames/film_*.png`: film strips of the moments that are easy to get wrong (turns, lying
  down, waking, the alarm laps, the alarm page's window)

`check` prints numeric checks and fails loudly if one is broken: the hero/wall seam, text
contrast at every hour, live draw-call budgets, the alarm page rectangles at awkward screen
sizes, and a long director soak (days of simulated time with awkward time steps, random
alarms and random taps). Draw-call counts are printed as the renders run.

Files: `Render` (entry point), `Sheets`, `Heroes`, `Anim`, `Pages`, `Checks` (one per kind of
output) and `GifOut` (animated GIF writer). The lettering on the mock pages is desktop
text standing in for the Android labels.
