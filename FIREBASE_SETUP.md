# RoundPilot — Firebase setup

The Android project is wired for Firebase Analytics + Firebase Crashlytics. The actual Firebase project configuration is intentionally not included in source control.

## One-time Firebase console setup

1. Create a Firebase project for **RoundPilot**.
2. Enable Google Analytics for the Firebase project.
3. Add an Android app with package name **`com.roundpilot.intervaltimer`**.
4. Download the resulting **`google-services.json`**.
5. Put it at **`app/google-services.json`** (exact name).
6. Sync Gradle. The Google Services and Crashlytics plugins are applied automatically when this file exists.
7. Run the app once. Verify Analytics DebugView and Crashlytics in the Firebase console.

Firebase's current Android setup recommends the Firebase Android BoM and requires the Google Services plugin/config file for a project-specific setup.

## Events in RoundPilot

The app intentionally tracks a small, product-useful set without logging user-entered preset names or personal identifiers:

- `workout_started` — source (`quick_start` / `preset`) + timing configuration
- `workout_completed` — rounds and total duration
- `workout_paused` / `workout_resumed` / `workout_skipped` / `workout_stopped`
- `preset_started` / `preset_created` / `preset_updated` / `preset_deleted`
- `setting_changed` — sound, vibration, countdown sound, screen-on
- `theme_changed` / `language_changed`

Firebase Analytics automatically collects standard lifecycle events such as `first_open` and `app_open`; the app does not duplicate them. Crashlytics provides crash, non-fatal and ANR reporting once the Firebase configuration is present.
