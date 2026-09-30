package com.example.myintervaltimerproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.LocalContext

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.platform.timer.TimerForegroundService
import com.example.myintervaltimerproject.timer.TimerConfig
import com.example.myintervaltimerproject.ui.home.HomeScreen
import com.example.myintervaltimerproject.ui.presets.PresetEditorScreen
import com.example.myintervaltimerproject.ui.settings.SettingsScreen
import com.example.myintervaltimerproject.ui.workout.WorkoutScreen


@Composable
fun AppNavigation() {

    val navController =
        rememberNavController()


    val context =
        LocalContext.current


    val app =
        context.applicationContext
                as IntervalTimerApp

    val preferences by app.preferences.flow.collectAsState(
        initial = com.example.myintervaltimerproject.data.preferences.AppPreferences()
    )


    val persistedSession = remember {
        app.sessions.load()
    }

    val hasPersistedSession = persistedSession != null

    var pendingConfig by remember {
        mutableStateOf(
            persistedSession?.config ?: TimerConfig()
        )
    }

    LaunchedEffect(hasPersistedSession) {
        if (hasPersistedSession) {
            TimerForegroundService.alarm(context)
        }
    }

    NavHost(

        navController =
            navController,

        startDestination =
            if (hasPersistedSession) AppRoute.Workout.route
            else AppRoute.Home.route

    ) {


        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        composable(
            AppRoute.Home.route
        ) {

            HomeScreen(

                onStart = { config, source ->

                    /*
                     * Remember the configuration so WorkoutScreen
                     * can display the same workout.
                     */
                    pendingConfig =
                        config


                    /*
                     * IMPORTANT:
                     *
                     * The Foreground Service is now the owner
                     * of the active timer.
                     *
                     * WorkoutScreen will only display the state
                     * of the shared TimerSessionController.
                     */
                    // Start the engine synchronously so WorkoutScreen never renders
                    // an empty Idle state while the service is being created.
                    app.timerSessionController.configurePreferences(preferences)
                    app.analytics.logWorkoutStarted(
                        source = source,
                        workSeconds = config.workSeconds,
                        restSeconds = config.restSeconds,
                        rounds = config.rounds,
                    )
                    app.timerSessionController.start(config)

                    // The service owns continuous background execution.
                    TimerForegroundService.start(context)


                    /*
                     * Open the workout UI after the timer state already exists.
                     */
                    navController.navigate(
                        AppRoute.Workout.route
                    )
                },


                onAddPreset = {

                    navController.navigate(
                        AppRoute.PresetEditor.route
                    )
                },


                onEditPreset = { presetId ->

                    navController.navigate(

                        "${AppRoute.PresetEditor.route}/$presetId"

                    )
                },


                onSettings = {

                    navController.navigate(
                        AppRoute.Settings.route
                    )
                }
            )
        }


        // -----------------------------------------------------
        // WORKOUT
        // -----------------------------------------------------

        composable(
            AppRoute.Workout.route
        ) {

            WorkoutScreen(

                config =
                    pendingConfig,

                onBack = {

                    navController.popBackStack()
                },

                app =
                    app
            )
        }


        // -----------------------------------------------------
        // NEW PRESET
        // -----------------------------------------------------

        composable(
            AppRoute.PresetEditor.route
        ) {

            PresetEditorScreen(

                presetId =
                    null,

                onBack = {

                    navController.popBackStack()
                }
            )
        }


        // -----------------------------------------------------
        // EDIT PRESET
        // -----------------------------------------------------

        composable(

            route =
                "${AppRoute.PresetEditor.route}/{presetId}"

        ) { backStackEntry ->


            val presetId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "presetId"
                    )
                    ?.toLongOrNull()


            PresetEditorScreen(

                presetId =
                    presetId,

                onBack = {

                    navController.popBackStack()
                }
            )
        }


        // -----------------------------------------------------
        // SETTINGS
        // -----------------------------------------------------

        composable(
            AppRoute.Settings.route
        ) {

            SettingsScreen(

                onBack = {

                    navController.popBackStack()
                }
            )
        }
    }
}