package com.example.myintervaltimerproject.ui.settings

import android.content.Context
import android.content.res.Configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.data.preferences.AppLanguage
import com.example.myintervaltimerproject.data.preferences.ThemeMode
import com.example.myintervaltimerproject.platform.vibration.AndroidVibrationController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {

    val app =
        LocalContext.current.applicationContext
                as IntervalTimerApp


    val vm: SettingsViewModel =
        viewModel(
            factory =
                SettingsViewModelFactory(
                    app.preferences
                )
        )


    val prefs by vm.prefs.collectAsState()


    /*
     * ---------------------------------------------------------
     * LOCALIZED CONTEXT
     * ---------------------------------------------------------
     *
     * The selected application language is stored in DataStore.
     *
     * We create a Context with that locale so that Settings
     * immediately displays the selected language even when the
     * device itself uses another language.
     */
    val context =
        LocalContext.current

    val localizedContext =
        remember(
            context,
            prefs.language
        ) {

            createLocalizedContext(
                context =
                    context,

                language =
                    prefs.language
            )
        }


    val resources =
        localizedContext.resources


    // ---------------------------------------------------------
    // VIBRATION
    // ---------------------------------------------------------

    val vibrationController =
        remember {

            AndroidVibrationController(
                app
            )
        }


    DisposableEffect(Unit) {

        onDispose {

            vibrationController.cancel()
        }
    }


    // ---------------------------------------------------------
    // SCREEN
    // ---------------------------------------------------------

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        resources.getString(
                            R.string.settings
                        )
                    )
                },

                navigationIcon = {

                    TextButton(
                        onClick = onBack
                    ) {

                        Text(
                            resources.getString(
                                R.string.back
                            )
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    ),

            verticalArrangement =
                Arrangement.Top
        ) {


            // -------------------------------------------------
            // SOUND
            // -------------------------------------------------

            SwitchRow(

                title =
                    resources.getString(
                        R.string.sound
                    ),

                checked =
                    prefs.soundEnabled,

                onChange = { enabled ->
                    app.analytics.logSettingChanged("sound", enabled)
                    vm.sound(enabled)
                }
            )


            // -------------------------------------------------
            // VIBRATION
            // -------------------------------------------------

            SwitchRow(

                title =
                    resources.getString(
                        R.string.vibration
                    ),

                checked =
                    prefs.vibrationEnabled,

                onChange = { enabled ->

                    app.analytics.logSettingChanged("vibration", enabled)

                    vm.vibration(
                        enabled
                    )


                    if (enabled) {

                        vibrationController
                            .phaseChange()

                    } else {

                        vibrationController
                            .cancel()
                    }
                }
            )


            /*
             * Test vibration explanation.
             */
            if (
                prefs.vibrationEnabled
            ) {

                Text(

                    text =
                        resources.getString(
                            R.string
                                .vibration_test_message
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.padding(

                            start =
                                16.dp,

                            end =
                                16.dp,

                            bottom =
                                8.dp
                        )
                )
            }


            // -------------------------------------------------
            // COUNTDOWN
            // -------------------------------------------------

            SwitchRow(

                title =
                    resources.getString(
                        R.string.countdown
                    ),

                checked =
                    prefs.countdownEnabled,

                onChange = { enabled ->
                    app.analytics.logSettingChanged("countdown_sound", enabled)
                    vm.countdown(enabled)
                }
            )


            // -------------------------------------------------
            // SCREEN
            // -------------------------------------------------

            SwitchRow(

                title =
                    resources.getString(
                        R.string
                            .keep_screen_awake
                    ),

                checked =
                    prefs.keepScreenOn,

                onChange = { enabled ->
                    app.analytics.logSettingChanged("keep_screen_on", enabled)
                    vm.screen(enabled)
                }
            )


            // -------------------------------------------------
            // APPEARANCE
            // -------------------------------------------------

            ListItem(

                headlineContent = {

                    Text(

                        resources.getString(
                            R.string.appearance
                        )
                    )
                },

                supportingContent = {

                    Text(

                        when (
                            prefs.themeMode
                        ) {

                            ThemeMode.SYSTEM ->

                                resources.getString(
                                    R.string.system
                                )


                            ThemeMode.LIGHT ->

                                resources.getString(
                                    R.string.light
                                )


                            ThemeMode.DARK ->

                                resources.getString(
                                    R.string.dark
                                )
                        }
                    )
                }
            )


            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                16.dp
                        ),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {


                FilterChip(

                    selected =
                        prefs.themeMode ==
                                ThemeMode.SYSTEM,

                    onClick = {

                        app.analytics.logThemeChanged("system")
                        vm.theme(ThemeMode.SYSTEM)
                    },

                    label = {

                        Text(

                            resources.getString(
                                R.string.system
                            )
                        )
                    }
                )


                FilterChip(

                    selected =
                        prefs.themeMode ==
                                ThemeMode.LIGHT,

                    onClick = {

                        app.analytics.logThemeChanged("light")
                        vm.theme(ThemeMode.LIGHT)
                    },

                    label = {

                        Text(

                            resources.getString(
                                R.string.light
                            )
                        )
                    }
                )


                FilterChip(

                    selected =
                        prefs.themeMode ==
                                ThemeMode.DARK,

                    onClick = {

                        app.analytics.logThemeChanged("dark")
                        vm.theme(ThemeMode.DARK)
                    },

                    label = {

                        Text(

                            resources.getString(
                                R.string.dark
                            )
                        )
                    }
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            // -------------------------------------------------
            // LANGUAGE
            // -------------------------------------------------

            ListItem(

                headlineContent = {

                    Text(

                        resources.getString(
                            R.string.language
                        )
                    )
                },

                supportingContent = {

                    Text(

                        when (
                            prefs.language
                        ) {

                            AppLanguage.ENGLISH ->

                                resources.getString(
                                    R.string.english
                                )


                            AppLanguage.RUSSIAN ->

                                resources.getString(
                                    R.string.russian
                                )
                        }
                    )
                }
            )


            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                16.dp
                        ),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {


                FilterChip(

                    selected =
                        prefs.language ==
                                AppLanguage.ENGLISH,

                    onClick = {

                        app.analytics.logLanguageChanged("english")
                        vm.language(AppLanguage.ENGLISH)
                    },

                    label = {

                        Text(

                            resources.getString(
                                R.string.english
                            )
                        )
                    }
                )


                FilterChip(

                    selected =
                        prefs.language ==
                                AppLanguage.RUSSIAN,

                    onClick = {

                        app.analytics.logLanguageChanged("russian")
                        vm.language(AppLanguage.RUSSIAN)
                    },

                    label = {

                        Text(

                            resources.getString(
                                R.string.russian
                            )
                        )
                    }
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            // -------------------------------------------------
            // ABOUT
            // -------------------------------------------------

            ListItem(

                headlineContent = {

                    Text(

                        resources.getString(
                            R.string.about
                        )
                    )
                },

                supportingContent = {

                    Text(
                        "RoundPilot • v1.0"
                    )
                }
            )
        }
    }
}


// -------------------------------------------------------------
// SWITCH ROW
// -------------------------------------------------------------

@Composable
private fun SwitchRow(

    title: String,

    checked: Boolean,

    onChange: (Boolean) -> Unit

) {

    ListItem(

        headlineContent = {

            Text(
                title
            )
        },

        trailingContent = {

            Switch(

                checked =
                    checked,

                onCheckedChange =
                    onChange
            )
        }
    )
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
            context
                .resources
                .configuration
        )


    val locale =
        when (language) {

            AppLanguage.ENGLISH ->

                java.util.Locale.US


            AppLanguage.RUSSIAN ->

                java.util.Locale(
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