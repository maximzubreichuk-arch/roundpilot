package com.example.myintervaltimerproject

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

import com.example.myintervaltimerproject.data.preferences.AppLanguage
import com.example.myintervaltimerproject.data.preferences.AppPreferences
import com.example.myintervaltimerproject.data.preferences.ThemeMode
import com.example.myintervaltimerproject.ui.launch.LaunchScreen
import com.example.myintervaltimerproject.ui.navigation.AppNavigation
import com.example.myintervaltimerproject.ui.theme.IntervalTimerTheme

import java.util.Locale


class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        val app =
            application
                    as IntervalTimerApp


        setContent {

            val prefs by
            app.preferences.flow.collectAsState(
                initial =
                    AppPreferences()
            )


            LaunchedEffect(prefs.language, prefs.themeMode) {
                app.analytics.setAppContext(
                    language = prefs.language.name,
                    theme = prefs.themeMode.name,
                )
            }


            val systemDark =
                isSystemInDarkTheme()


            val darkTheme =
                when (
                    prefs.themeMode
                ) {

                    ThemeMode.SYSTEM ->
                        systemDark

                    ThemeMode.LIGHT ->
                        false

                    ThemeMode.DARK ->
                        true
                }


            /*
             * -----------------------------------------------------
             * SELECTED APPLICATION LANGUAGE
             * -----------------------------------------------------
             */

            val languageTag =
                when (
                    prefs.language
                ) {

                    AppLanguage.ENGLISH ->
                        "en"

                    AppLanguage.RUSSIAN ->
                        "ru"
                }


            /*
             * Create a configuration using
             * the language selected inside the app.
             */
            val baseConfiguration =
                LocalConfiguration.current


            val localizedConfiguration =
                Configuration(
                    baseConfiguration
                ).apply {

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.N
                    ) {

                        setLocales(

                            LocaleList(
                                Locale.forLanguageTag(
                                    languageTag
                                )
                            )
                        )
                    }
                }


            /*
             * Context configured with the selected language.
             *
             * stringResource() inside the app will now
             * use values/strings.xml or values-ru/strings.xml.
             */
            val localizedContext =
                LocalContext.current
                    .createConfigurationContext(
                        localizedConfiguration
                    )


            CompositionLocalProvider(

                LocalContext provides
                        localizedContext,

                LocalConfiguration provides
                        localizedConfiguration

            ) {

                IntervalTimerTheme(

                    darkTheme =
                        darkTheme

                ) {

                    var launchFinished
                            by rememberSaveable {

                                mutableStateOf(
                                    false
                                )
                            }


                    if (
                        launchFinished
                    ) {

                        /*
                         * Request notification permission
                         * on Android 13+.
                         */
                        LaunchedEffect(Unit) {

                            if (
                                Build.VERSION.SDK_INT >=
                                33
                            ) {

                                notificationPermission.launch(

                                    Manifest.permission
                                        .POST_NOTIFICATIONS
                                )
                            }
                        }


                        AppNavigation()

                    } else {

                        LaunchScreen {

                            launchFinished =
                                true
                        }
                    }
                }
            }
        }
    }
}