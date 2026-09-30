package com.example.myintervaltimerproject.ui.workout

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.SystemClock
import android.view.WindowManager

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.data.preferences.AppLanguage
import com.example.myintervaltimerproject.data.preferences.AppPreferences
import com.example.myintervaltimerproject.timer.TimerConfig
import com.example.myintervaltimerproject.timer.TimerPhase
import com.example.myintervaltimerproject.timer.TimerState
import com.example.myintervaltimerproject.ui.components.TimerRing
import com.example.myintervaltimerproject.ui.theme.Dimens
import com.example.myintervaltimerproject.ui.theme.PurpleRest
import com.example.myintervaltimerproject.ui.theme.TealPrimary

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import java.util.Locale


@Composable
fun WorkoutScreen(
    config: TimerConfig,
    onBack: () -> Unit,
    app: IntervalTimerApp,
    vm: WorkoutViewModel = viewModel(
        factory =
            WorkoutViewModelFactory(
                app
            )
    )
) {

    val state by
    vm.state.collectAsState()


    LaunchedEffect(state::class) {
        app.analytics.setTimerState(
            state::class.simpleName ?: "unknown"
        )
    }


    /*
     * Current application settings.
     */
    val preferences by
    app.preferences.flow.collectAsState(
        initial =
            AppPreferences()
    )


    val controller = app.timerSessionController

    var sessionSoundEnabled by remember(controller) {
        mutableStateOf(controller.isSessionSoundEnabled)
    }


    // ---------------------------------------------------------
    // KEEP SCREEN AWAKE
    // ---------------------------------------------------------

    val context =
        LocalContext.current


    val keepScreenAwake =
        preferences.keepScreenOn &&
                state is TimerState.Running

    /*
     * Apply the flag to the Compose host View itself. This is deliberately
     * independent of ContextWrapper/Activity resolution and therefore also
     * works with the localized configuration context used by the app.
     */
    val hostView = LocalView.current

    DisposableEffect(hostView, keepScreenAwake) {
        hostView.keepScreenOn = keepScreenAwake

        onDispose {
            hostView.keepScreenOn = false
        }
    }


    // ---------------------------------------------------------
    // BACK
    // ---------------------------------------------------------

    BackHandler {
        app.analytics.logWorkoutStopped()
        vm.stop()
        onBack()
    }


    // ---------------------------------------------------------
    // STATE
    // ---------------------------------------------------------

    when (
        val currentState =
            state
    ) {

        // -----------------------------------------------------
        // IDLE
        // -----------------------------------------------------

        TimerState.Idle -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }


        // -----------------------------------------------------
        // RUNNING
        // -----------------------------------------------------

        is TimerState.Running -> {

            val accent =
                if (
                    currentState.phase ==
                    TimerPhase.REST
                ) {

                    PurpleRest

                } else {

                    TealPrimary
                }


            WorkoutBody(

                config =
                    config,

                phase =
                    currentState.phase,

                remaining =
                    currentState.remainingSeconds,

                phaseEndsAtElapsedRealtime =
                    currentState.phaseEndsAtElapsedRealtime,

                round =
                    currentState.currentRound,

                total =
                    currentState.totalRounds,

                accent =
                    accent,

                paused =
                    false,

                soundEnabled =
                    sessionSoundEnabled,

                globalSoundEnabled =
                    preferences.soundEnabled,

                language =
                    preferences.language,

                onPause = {
                    app.analytics.logWorkoutPaused()
                    vm.pause()
                },

                onResume = {
                    app.analytics.logWorkoutResumed()
                    vm.resume()
                },

                onSkip = {
                    app.analytics.logWorkoutSkipped()
                    vm.skip()
                },

                onSoundChange = { enabled ->

                    sessionSoundEnabled =
                        enabled


                    controller.setSessionSoundEnabled(enabled)
                },

                onStop = {
                    app.analytics.logWorkoutStopped()
                    vm.stop()
                    onBack()
                }
            )
        }


        // -----------------------------------------------------
        // PAUSED
        // -----------------------------------------------------

        is TimerState.Paused -> {

            val accent =
                if (
                    currentState.phase ==
                    TimerPhase.REST
                ) {

                    PurpleRest

                } else {

                    TealPrimary
                }


            WorkoutBody(

                config =
                    config,

                phase =
                    currentState.phase,

                remaining =
                    currentState.remainingSeconds,

                phaseEndsAtElapsedRealtime =
                    0L,

                round =
                    currentState.currentRound,

                total =
                    currentState.totalRounds,

                accent =
                    accent,

                paused =
                    true,

                soundEnabled =
                    sessionSoundEnabled,

                globalSoundEnabled =
                    preferences.soundEnabled,

                language =
                    preferences.language,

                onPause = {
                    app.analytics.logWorkoutPaused()
                    vm.pause()
                },

                onResume = {
                    app.analytics.logWorkoutResumed()
                    vm.resume()
                },

                onSkip = {
                    app.analytics.logWorkoutSkipped()
                    vm.skip()
                },

                onSoundChange = { enabled ->

                    sessionSoundEnabled =
                        enabled


                    controller.setSessionSoundEnabled(enabled)
                },

                onStop = {
                    app.analytics.logWorkoutStopped()
                    vm.stop()
                    onBack()
                }
            )
        }


        // -----------------------------------------------------
        // COMPLETED
        // -----------------------------------------------------

        is TimerState.Completed -> {

            LaunchedEffect(Unit) {
                app.analytics.logWorkoutCompleted(
                    completedRounds = currentState.completedRounds,
                    totalRounds = currentState.totalRounds,
                    totalTimeSeconds = currentState.totalTimeSeconds,
                )

            }


            FinishedScreen(

                completedRounds =
                    currentState.completedRounds,

                totalRounds =
                    currentState.totalRounds,

                totalTimeSeconds =
                    currentState.totalTimeSeconds,

                language =
                    preferences.language,

                onDone =
                    onBack
            )
        }


        // -----------------------------------------------------
        // STOPPED
        // -----------------------------------------------------

        TimerState.Stopped -> Unit
    }
}


// -------------------------------------------------------------
// WORKOUT BODY
// -------------------------------------------------------------

@Composable
private fun WorkoutBody(
    config: TimerConfig,
    phase: TimerPhase,
    remaining: Int,
    phaseEndsAtElapsedRealtime: Long,
    round: Int,
    total: Int,
    accent: Color,
    paused: Boolean,
    soundEnabled: Boolean,
    globalSoundEnabled: Boolean,
    language: AppLanguage,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onStop: () -> Unit
) {

    /*
     * Used only for smooth visual progress.
     */
    var nowMillis by remember {

        mutableLongStateOf(
            SystemClock.elapsedRealtime()
        )
    }


    LaunchedEffect(
        phase,
        phaseEndsAtElapsedRealtime,
        paused
    ) {

        if (
            !paused
        ) {

            while (
                isActive
            ) {

                nowMillis =
                    SystemClock.elapsedRealtime()


                delay(
                    16L
                )
            }
        }
    }


    val context =
        LocalContext.current


    val localizedContext =
        remember(
            context,
            language
        ) {

            createLocalizedContext(

                context =
                    context,

                language =
                    language
            )
        }


    val resources =
        localizedContext.resources


    // ---------------------------------------------------------
    // PHASE DURATION
    // ---------------------------------------------------------

    val phaseDurationSeconds =
        when (
            phase
        ) {

            TimerPhase.GET_READY ->
                config.warmUpSeconds

            TimerPhase.WORK ->
                config.workSeconds

            TimerPhase.REST ->
                config.restSeconds

            TimerPhase.COOLDOWN ->
                config.cooldownSeconds
        }


    // ---------------------------------------------------------
    // PROGRESS
    // ---------------------------------------------------------

    val progress =

        if (
            paused
        ) {

            if (
                phaseDurationSeconds >
                0
            ) {

                (
                        (
                                phaseDurationSeconds -
                                        remaining
                                ).toFloat() /
                                phaseDurationSeconds
                                    .toFloat()
                        )

            } else {

                1f
            }

        } else if (

            phaseDurationSeconds >
            0 &&

            phaseEndsAtElapsedRealtime >
            0L

        ) {

            val durationMillis =
                phaseDurationSeconds *
                        1000L


            val phaseStartMillis =
                phaseEndsAtElapsedRealtime -
                        durationMillis


            (
                    (
                            nowMillis -
                                    phaseStartMillis
                            ).toFloat() /
                            durationMillis
                                .toFloat()
                    )

        } else {

            0f
        }


    val safeProgress =
        progress.coerceIn(
            0f,
            1f
        )


    // ---------------------------------------------------------
    // PHASE TITLE
    // ---------------------------------------------------------

    val phaseTitle =
        when (
            phase
        ) {

            TimerPhase.GET_READY ->

                resources.getString(
                    R.string.get_ready
                )


            TimerPhase.WORK ->

                resources.getString(
                    R.string.work
                )


            TimerPhase.REST ->

                resources.getString(
                    R.string.rest
                )


            TimerPhase.COOLDOWN ->

                resources.getString(
                    R.string.cooldown
                )
        }


    val title =
        if (
            paused
        ) {

            resources.getString(
                R.string.paused
            )

        } else {

            phaseTitle
        }


    // ---------------------------------------------------------
    // UI
    // ---------------------------------------------------------

    Scaffold(

        containerColor =
            Color.Transparent

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal =
                            Dimens
                                .ScreenHorizontalPadding
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            // -------------------------------------------------
            // SOUND
            // -------------------------------------------------

            Box(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp
                        )
            ) {

                IconButton(

                    enabled =
                        globalSoundEnabled,

                    onClick = {
                        onSoundChange(
                            !soundEnabled
                        )
                    },

                    modifier =
                        Modifier
                            .align(
                                Alignment.TopEnd
                            )
                            .size(
                                48.dp
                            )
                ) {

                    Icon(

                        imageVector =
                            if (
                                globalSoundEnabled && soundEnabled
                            ) {

                                Icons.Default
                                    .VolumeUp

                            } else {

                                Icons.Default
                                    .VolumeOff
                            },

                        contentDescription =
                            resources.getString(
                                R.string.sound
                            ),

                        modifier =
                            Modifier.size(
                                27.dp
                            ),

                        tint =
                            if (globalSoundEnabled) {
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            }
                    )
                }
            }


            // -------------------------------------------------
            // MAIN CONTENT
            // -------------------------------------------------

            Box(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        )
            ) {

                Column(

                    modifier =
                        Modifier.fillMaxSize(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    // -----------------------------------------
                    // PHASE TITLE
                    // -----------------------------------------

                    Text(

                        text =
                            title,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                                .copy(
                                    fontSize =
                                        20.sp
                                ),

                        color =
                            accent,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(

                        modifier =
                            Modifier.height(
                                30.dp
                            )
                    )


                    // -----------------------------------------
                    // TIMER RING
                    // -----------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    1f
                                )
                                .padding(
                                    horizontal =
                                        4.dp
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        TimerRing(

                            progress =
                                safeProgress,

                            color =
                                accent,

                            modifier =
                                Modifier.fillMaxSize()
                        )


                        Text(

                            text =
                                "%02d:%02d".format(

                                    remaining / 60,

                                    remaining % 60
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .displayLarge
                                    .copy(
                                        fontSize =
                                            88.sp
                                    ),

                            maxLines =
                                1,

                            softWrap =
                                false,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }


                    // -----------------------------------------
                    // ROUND
                    // -----------------------------------------

                    Box(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(
                                    1f
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(

                            text =

                                if (
                                    phase ==
                                    TimerPhase.GET_READY
                                ) {

                                    resources.getString(
                                        R.string.get_ready
                                    )

                                } else {

                                    resources.getString(
                                        R.string.round_format,
                                        round,
                                        total
                                    )
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }


                    // -----------------------------------------
                    // CONTROLS
                    // -----------------------------------------

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(

                            modifier =
                                Modifier.weight(1f),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            SecondaryWorkoutControl(

                                icon =
                                    Icons.Default
                                        .SkipNext,

                                label =
                                    resources.getString(
                                        R.string.skip
                                    ),

                                onClick =
                                    onSkip
                            )
                        }


                        Box(

                            modifier =
                                Modifier.weight(1f),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Surface(

                                onClick = {

                                    if (
                                        paused
                                    ) {

                                        onResume()

                                    } else {

                                        onPause()
                                    }
                                },

                                modifier =
                                    Modifier
                                        .size(
                                            96.dp
                                        )
                                        .offset(
                                            y =
                                                (-16).dp
                                        ),

                                shape =
                                    CircleShape,

                                color =
                                    accent
                            ) {

                                Box(

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(

                                        imageVector =

                                            if (
                                                paused
                                            ) {

                                                Icons.Default
                                                    .PlayArrow

                                            } else {

                                                Icons.Default
                                                    .Pause
                                            },

                                        contentDescription =

                                            if (
                                                paused
                                            ) {

                                                resources.getString(
                                                    R.string.resume
                                                )

                                            } else {

                                                resources.getString(
                                                    R.string.pause
                                                )
                                            },

                                        modifier =
                                            Modifier.size(
                                                40.dp
                                            ),

                                        tint =
                                            Color.White
                                    )
                                }
                            }
                        }


                        Box(

                            modifier =
                                Modifier.weight(1f),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            SecondaryWorkoutControl(

                                icon =
                                    Icons.Default
                                        .Stop,

                                label =
                                    resources.getString(
                                        R.string.stop
                                    ),

                                onClick =
                                    onStop
                            )
                        }
                    }


                    Spacer(

                        modifier =
                            Modifier.height(
                                28.dp
                            )
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.height(
                        18.dp
                    )
            )
        }
    }
}


// -------------------------------------------------------------
// SECONDARY WORKOUT CONTROL
// -------------------------------------------------------------

@Composable
private fun SecondaryWorkoutControl(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Surface(

            onClick =
                onClick,

            modifier =
                Modifier.size(
                    64.dp
                ),

            shape =
                CircleShape,

            color =
                MaterialTheme
                    .colorScheme
                    .outline
        ) {

            Box(

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(

                    imageVector =
                        icon,

                    contentDescription =
                        label,

                    modifier =
                        Modifier.size(
                            27.dp
                        ),

                    tint =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )
            }
        }


        Spacer(

            modifier =
                Modifier.height(
                    5.dp
                )
        )


        Text(

            text =
                label,

            style =
                MaterialTheme
                    .typography
                    .bodySmall
                    .copy(

                        fontSize =
                            14.sp,

                        fontWeight =
                            FontWeight.Medium
                    ),

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


// -------------------------------------------------------------
// FINISHED SCREEN
// -------------------------------------------------------------

@Composable
private fun FinishedScreen(
    completedRounds: Int,
    totalRounds: Int,
    totalTimeSeconds: Int,
    language: AppLanguage,
    onDone: () -> Unit
) {

    val context =
        LocalContext.current


    val localizedContext =
        remember(
            context,
            language
        ) {

            createLocalizedContext(

                context =
                    context,

                language =
                    language
            )
        }


    val resources =
        localizedContext.resources


    Scaffold(

        containerColor =
            Color.Transparent

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .padding(
                        24.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            Spacer(

                modifier =
                    Modifier.height(
                        64.dp
                    )
            )


            // -------------------------------------------------
            // CHECK
            // -------------------------------------------------

            Surface(

                modifier =
                    Modifier.size(
                        96.dp
                    ),

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(
                            alpha =
                                0.14f
                        )
            ) {

                Box(

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Check,

                        contentDescription =
                            resources.getString(
                                R.string
                                    .workout_complete_content_description
                            ),

                        modifier =
                            Modifier.size(
                                52.dp
                            ),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.height(
                        28.dp
                    )
            )


            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            Text(

                text =
                    resources.getString(
                        R.string.workout_complete
                    ),

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )


            Spacer(

                modifier =
                    Modifier.height(
                        36.dp
                    )
            )


            // -------------------------------------------------
            // RESULTS
            // -------------------------------------------------

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                ResultValue(

                    value =
                        "$completedRounds / $totalRounds",

                    label =
                        resources.getString(
                            R.string.rounds_result
                        )
                )


                ResultValue(

                    value =
                        formatTotalTime(
                            totalTimeSeconds
                        ),

                    label =
                        resources.getString(
                            R.string.total_time
                        )
                )
            }


            Spacer(

                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            // -------------------------------------------------
            // DONE
            // -------------------------------------------------

            Button(

                onClick =
                    onDone,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        )
            ) {

                Text(

                    text =
                        resources.getString(
                            R.string.done
                        )
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        24.dp
                    )
            )
        }
    }
}


// -------------------------------------------------------------
// RESULT VALUE
// -------------------------------------------------------------

@Composable
private fun ResultValue(
    value: String,
    label: String
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(

            text =
                value,

            style =
                MaterialTheme
                    .typography
                    .headlineMedium,

            fontWeight =
                FontWeight.SemiBold,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )


        Spacer(

            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Text(

            text =
                label,

            style =
                MaterialTheme
                    .typography
                    .labelMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


// -------------------------------------------------------------
// LOCALIZED CONTEXT
// -------------------------------------------------------------

private fun createLocalizedContext(
    context: Context,
    language: AppLanguage
): Context {

    val configuration =
        Configuration(
            context.resources.configuration
        )


    val locale =
        when (
            language
        ) {

            AppLanguage.ENGLISH ->
                Locale.US

            AppLanguage.RUSSIAN ->
                Locale(
                    "ru",
                    "RU"
                )
        }


    configuration.setLocale(
        locale
    )


    return context.createConfigurationContext(
        configuration
    )
}


// -------------------------------------------------------------
// FIND ACTIVITY
// -------------------------------------------------------------

private fun Context.findActivity():
        Activity? {

    return when (this) {

        is Activity ->
            this

        is ContextWrapper ->
            baseContext.findActivity()

        else ->
            null
    }
}


// -------------------------------------------------------------
// FORMAT TOTAL TIME
// -------------------------------------------------------------

private fun formatTotalTime(
    seconds: Int
): String {

    val minutes =
        seconds / 60


    val remainingSeconds =
        seconds % 60


    return "%02d:%02d".format(

        minutes,

        remainingSeconds
    )
}