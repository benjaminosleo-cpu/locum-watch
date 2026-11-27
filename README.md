# Locum Watch

A small Android app for a spare phone that watches her WhatsApp locum group and
rings a loud bedside alarm when someone posts a locum request.

- Reads WhatsApp notifications on the spare phone (linked to her WhatsApp as a companion device).
- Decides whether each message is a locum request using a filter tuned on the group's own
  history (Apr 2025 - Nov 2026): Karen's shift offers, plus same-day requests from anyone else.
  Swaps, "gone/taken/left" updates and replies are ignored.
- Rings the alarm at full alarm volume through Do Not Disturb, with the message on screen,
  until STOP is tapped (default up to 10 minutes). Then stays quiet for 60 minutes.
- On/off switch from her iPhone via a private ntfy topic: `on`, `off`, `status`, `test`.
  Replies go to `<topic>-reply` (subscribe in the free ntfy iPhone app).
- Optional: add Pushover keys in Settings to also send an iPhone Critical Alert.
- Daily 20:30 check-in to `<topic>-reply` so a dead or unlinked phone gets noticed.

## The look

The app is drawn as the inside of a witch's cottage. The main page is a window onto a
garden where Mochi the capybara sleeps, grazes and plays; when an alarm rings Mochi races
round and jumps up at the window, and stays lively for a few minutes afterwards.

- The light follows the real sun in South Manchester (`SunClock`), so winter afternoons go dark early.
- The board hanging under the window says whether the alarm is ON or OFF and turns red with
  the first problem if anything needs attention (tap it to jump to Checks).
- The lantern on the sill is lit when the alarm is on. One tap lights it; putting it out asks first.
- Tap Mochi and she reacts. Settings, checks and the message log are further down the page.

All artwork is drawn in code in `app/src/main/java/.../art/` (pure Java, no image files),
through the small `Pen` interface. `AndroidPen` draws it in the app; `tools/preview/render.sh`
renders the same code to PNGs and GIFs on a desktop so changes can be checked without a phone,
and `ArtTest` draws every layer at every time of day through a checking pen on each build.

The artwork is never allowed to get in the way of the alarm: every drawing call is guarded,
and if any of it fails the page falls back to plain colours with the same message and
buttons. To save the spare phone's battery the animation slows down when Mochi is asleep
and when the screen has not been touched for a while. Diagnostics shows the measured frame rate.

## Build and install

Every push to `main` runs the unit tests (a replay of the whole group history, and the artwork checks),
builds a signed APK and, if everything passes, publishes it as GitHub Release
`v<VERSION>.<run number>`. Obtainium follows these releases. Bump `VERSION` for bigger changes.
After installing any update on the spare phone, run a test alarm.

The signing key is in `keystore/` (private repo) so every build is signed the same way.

## Tuning the filter

`app/src/main/java/.../LocumFilter.java`. Every message in `app/src/test/resources/history.tsv`
(anonymised) must keep the decision recorded there, so `LocumFilterHistoryTest` fails if a
change breaks an earlier case. Add new examples as tests when the filter is changed.
