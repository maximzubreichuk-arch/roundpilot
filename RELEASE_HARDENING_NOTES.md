# Interval Timer — Background Reliability Hardening

## Scope
This build is a reliability-focused hardening pass. No new product features were added.

## Timing model
- `TimerEngine` is the single source of truth for timer state and phase transitions.
- All timer deadlines use `SystemClock.elapsedRealtime()` (monotonic time); wall-clock changes cannot move the workout backwards.
- The engine does not own a ticker. `TimerForegroundService` drives `tick()` while `AlarmManager` provides an exact phase-boundary wake-up safety net.
- `TimerAlarmReceiver` never advances a phase itself.
- A late tick/restore catches up from the original absolute timeline and emits every crossed phase transition.
- Pause/Resume preserves sub-second remaining time.
- `Rest = 0` advances directly to the next work round.

## Foreground/background
- Foreground service starts immediately for an active workout and uses `START_STICKY` for process recreation.
- Alarm-driven service commands are handled synchronously in `onStartCommand()` so a boundary cannot wait behind an unrelated coroutine launch.
- The service has a continuous ticker for UI/state freshness and exact alarms for phase-boundary recovery.
- Terminal states stop the foreground service and remove its ongoing notification.
- A malformed/obsolete persisted session is rejected and cleared.
- Persisted active sessions include a schema version and Android boot count so an elapsedRealtime session is never interpreted across a device reboot.

## Notification
- Running notification uses Android's chronometer correctly: the monotonic deadline is converted to the wall-clock `Notification.when` value expected by SystemUI.
- Running notification therefore counts down without per-second notification rewrites and cannot be initialized with an elapsedRealtime value in the wrong time base.
- Paused state is represented explicitly instead of leaving an old running notification visible.
- A restored running session recreates its current notification/scheduled boundary without replaying the phase voice cue.

## Audio / vibration
- Countdown cues are driven from the same engine state used by the timer.
- Countdown beeps are de-duplicated per second and fire for the final 5..1 seconds.
- Every emitted phase transition triggers its phase-change cue/voice/vibration through the controller, including transitions discovered during catch-up.
- Existing language/voice behavior is preserved.

## UI safety
- Workout start creates the timer state synchronously before navigation, removing the start race that could render an empty workout state.
- Workout `Idle` renders a progress indicator instead of a blank screen.
- Workout progress animation uses monotonic time, matching the engine deadline time base.

## Validation performed in this environment
- Comprehensive `TimerEngine` smoke/test harness: PASS.
- Notification elapsedRealtime-to-SystemUI time-base regression: PASS.
- Static source scan: no legacy timer wall-clock / RTC wakeup paths remain in timer logic.
- ZIP integrity (`unzip -t`): PASS.
- Full Android Gradle build was not possible in this environment because Gradle 9.5.0 is not locally installed and external distribution download was unavailable.

## Physical-device release checks still required
1. Start a 15s Work / 15s Rest workout.
2. Background the app, open another app for 30–60s, then return.
3. Confirm Work -> Rest -> next Work transitions happen while the timer UI is not visible.
4. Confirm phase voice, boundary beep, countdown beeps and vibration occur in background/screen-off.
5. Confirm notification never shows a negative timer and changes phase at the correct boundary.
6. Verify lock/unlock and process recreation during Work and Rest.
7. Verify Pause/Resume and Stop confirmation.
