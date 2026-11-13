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

## Build and install

GitHub Actions runs the unit tests (including a replay of the whole group history) and
builds a signed APK on every push. Pushing a tag `vX.Y.Z` publishes a GitHub Release with
the APK attached, which Obtainium picks up.

The signing key is in `keystore/` (private repo) so every build is signed the same way.

## Tuning the filter

`app/src/main/java/.../LocumFilter.java`. Every message in `app/src/test/resources/history.tsv`
(anonymised) must keep the decision recorded there, so `LocumFilterHistoryTest` fails if a
change breaks an earlier case. Add new examples as tests when the filter is changed.
