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
garden where Mochi the capybara sleeps, grazes and plays, lit for the real time of day;
when an alarm rings Mochi races round and jumps up at the window. The lantern on the sill
is lit when the alarm is switched on (tap it to switch). Settings are further down the page.

All artwork is drawn in code in `app/src/main/java/.../art/` (pure Java, no image files),
through the small `Pen` interface. `AndroidPen` draws it in the app; `tools/preview/render.sh`
renders the same code to PNGs and GIFs on a desktop so changes can be checked without a phone.
If any artwork ever fails to draw, the app falls back to plain colours and the alarm and
buttons keep working.

## Build and install

Every push to `main` runs the unit tests (including a replay of the whole group history),
builds a signed APK and, if everything passes, publishes it as GitHub Release
`v<VERSION>.<run number>`. Obtainium follows these releases. Bump `VERSION` for bigger changes.

The signing key is in `keystore/` (private repo) so every build is signed the same way.

## Tuning the filter

`app/src/main/java/.../LocumFilter.java`. Every message in `app/src/test/resources/history.tsv`
(anonymised) must keep the decision recorded there, so `LocumFilterHistoryTest` fails if a
change breaks an earlier case. Add new examples as tests when the filter is changed.
