# RoundPilot — fixed launcher icon

The launcher icon is intentionally implemented as a fixed legacy bitmap instead of an Adaptive Icon.
This avoids OEM-specific adaptive-mask scaling differences observed during physical-device testing.

## Implementation
- Opaque artwork based on the approved RoundPilot design.
- No transparent adaptive foreground layer.
- No `mipmap-anydpi-v26` adaptive icon resources remain.
- Density-specific PNGs are provided for mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi.
- `android:icon` and `android:roundIcon` continue to point to `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.
- Google Play 512x512 icon uses the same artwork.

The launcher may still apply its own presentation behavior to legacy icons on some OEM launchers, but the app itself no longer supplies an adaptive mask or foreground scaling that can crop the artwork.
