package com.example.myintervaltimerproject.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    ENGLISH,
    RUSSIAN
}

data class AppPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val countdownEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
)

private val Context.dataStore by preferencesDataStore(
    name = "settings"
)

class PreferencesRepository(
    private val context: Context
) {

    private object Keys {

        val sound =
            booleanPreferencesKey("sound")

        val vibration =
            booleanPreferencesKey("vibration")

        val countdown =
            booleanPreferencesKey("countdown")

        val keepScreenOn =
            booleanPreferencesKey("keep_screen_on")

        val theme =
            stringPreferencesKey("theme")

        val language =
            stringPreferencesKey("language")
    }

    val flow: Flow<AppPreferences> =
        context.dataStore.data.map { p ->

            AppPreferences(

                soundEnabled =
                    p[Keys.sound] ?: true,

                vibrationEnabled =
                    p[Keys.vibration] ?: true,

                countdownEnabled =
                    p[Keys.countdown] ?: true,

                keepScreenOn =
                    p[Keys.keepScreenOn] ?: true,

                themeMode =
                    runCatching {
                        ThemeMode.valueOf(
                            p[Keys.theme]
                                ?: ThemeMode.DARK.name
                        )
                    }.getOrDefault(
                        ThemeMode.DARK
                    ),

                language =
                    runCatching {
                        AppLanguage.valueOf(
                            p[Keys.language]
                                ?: AppLanguage.ENGLISH.name
                        )
                    }.getOrDefault(
                        AppLanguage.ENGLISH
                    ),
            )
        }

    suspend fun setSound(
        value: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.sound] = value
        }
    }

    suspend fun setVibration(
        value: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.vibration] = value
        }
    }

    suspend fun setCountdown(
        value: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.countdown] = value
        }
    }

    suspend fun setKeepScreenOn(
        value: Boolean
    ) {
        context.dataStore.edit {
            it[Keys.keepScreenOn] = value
        }
    }

    suspend fun setThemeMode(
        value: ThemeMode
    ) {
        context.dataStore.edit {
            it[Keys.theme] = value.name
        }
    }

    suspend fun setLanguage(
        value: AppLanguage
    ) {
        context.dataStore.edit {
            it[Keys.language] = value.name
        }
    }
}