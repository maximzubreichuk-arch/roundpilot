# Interval Timer — Full MVP v1

Generated from the approved project specification v1.2 and the current local Android project skeleton.

## Product
- Android / Kotlin / Jetpack Compose / Material 3
- Simplified MVVM
- No backend
- Local presets/settings
- Work / Rest / Rounds / Warm-up / Cooldown
- Pause / Resume / Skip / Stop / Complete
- Dark + Light + System theme
- Sound + vibration
- Notification + exact-alarm phase wake-up for background/screen-off

## Important
`USE_EXACT_ALARM` is used because the app is a timer and its core user-facing functionality requires precise timing. Review Play policy and Play Console declarations before production release.

## Build
Open the project in Android Studio and run Gradle Sync + Assemble Project + unit tests. The development environment must have Android SDK 37 installed.

## Scope
Ads, Google Play Billing and Pro entitlements are intentionally not active in this build. They are the later monetization stage in the project roadmap and should be added only after the core MVP is stable.
