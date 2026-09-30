# RoundPilot — release fix 4

## Voice announcement fix

- Every non-empty phase is announced at the moment the phase starts.
- This includes the default 5-second GET_READY and short COOLDOWN phases.
- For a 5-second phase, the phase voice replaces the theoretical 5-second beep. Countdown beeps are scheduled for 4, 3, 2, 1, preventing voice/beep overlap.
- For shorter phases the countdown similarly starts on the next second, so the phase announcement is never intentionally suppressed just because the phase is short.
- For phases longer than five seconds, the existing final-five-second countdown remains 5, 4, 3, 2, 1.

## Verification

- TimerCuePolicy was checked independently with Kotlin/JVM for 5s GET_READY, 5s COOLDOWN, normal 30s countdown, and no 5s beep on a 5s phase.
- Gradle Android tests could not be executed in this environment because the Gradle wrapper requires downloading Gradle 9.5.0 from services.gradle.org and external network access is unavailable.
