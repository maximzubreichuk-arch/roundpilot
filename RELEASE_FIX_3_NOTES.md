# Release Fix 3 — final reliability/polish pass

Date: 2026-09-16

## Fixed

### 1. Start / GET READY audio sequence
- Removed the phase-boundary beep from `GET_READY`.
- `GET_READY` now announces the phase by voice only.
- Countdown beep policy is now exactly `4, 3, 2, 1`.
- At a 5-second warm-up the expected sequence is:
  `5 + Get ready voice -> 4 + beep -> 3 + beep -> 2 + beep -> 1 + beep -> Work transition`.
- The same rule applies to short phases: no beep at second 5.

### 2. Countdown setting
- The existing Countdown preference remains a global setting.
- It controls the final-second countdown cues (beeps and their associated vibration cue).
- The numerical timer itself is never disabled: it is the core timer display.
- This keeps the setting semantically stable and avoids turning off the actual workout clock.

### 3. Keep screen awake
- Replaced Activity/ContextWrapper based flag handling with the Compose host View's `keepScreenOn` property.
- The flag is applied only while a workout is in `Running` state and the preference is enabled.
- It is explicitly cleared when the workout view is disposed or the preference/state changes.
- No other source in the project writes the keep-screen flag.

### 4. Sound button when global Sound is OFF
- The workout sound button is now disabled when global Sound is OFF.
- It shows the muted icon and a disabled visual state.
- Tapping it cannot create a misleading local ON state while the global sound setting is OFF.
- Restored sessions also respect the global Sound preference.

### 5. Rare white/blank workout state
- The transient `Idle` UI now has an explicit Material background instead of relying on a transparent surface over the application background.
- The existing synchronous start-before-navigation flow is preserved so the normal workout route receives a running state before it is displayed.
- This is a defensive UI guard; a full Android device test remains required.

## Static verification performed
- Changed Kotlin files have balanced delimiters.
- `TimerCuePolicy` compiles and passes dedicated checks.
- Source scan confirms countdown policy is exactly `5, 4, 3, 2, 1` for phases of at least five seconds.
- Source scan confirms `GET_READY` is excluded from phase-boundary beep.
- Source scan confirms global Sound gates the workout sound control.
- Source scan confirms the only keep-screen implementation is the host View property.
- Countdown cues are scheduled with individual exact `AlarmManager` wake-ups in addition to the foreground-service ticker fallback.
- Countdown tones use the music stream so notification-volume settings cannot silently suppress workout beeps.
- No newly generated build artifacts were added to the project archive.
