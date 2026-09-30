package com.example.myintervaltimerproject.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myintervaltimerproject.data.preferences.AppLanguage
import com.example.myintervaltimerproject.data.preferences.AppPreferences
import com.example.myintervaltimerproject.data.preferences.PreferencesRepository
import com.example.myintervaltimerproject.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: PreferencesRepository
) : ViewModel() {

    val prefs =
        repo.flow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppPreferences()
        )

    fun sound(
        value: Boolean
    ) = viewModelScope.launch {
        repo.setSound(value)
    }

    fun vibration(
        value: Boolean
    ) = viewModelScope.launch {
        repo.setVibration(value)
    }

    fun countdown(
        value: Boolean
    ) = viewModelScope.launch {
        repo.setCountdown(value)
    }

    fun screen(
        value: Boolean
    ) = viewModelScope.launch {
        repo.setKeepScreenOn(value)
    }

    fun theme(
        value: ThemeMode
    ) = viewModelScope.launch {
        repo.setThemeMode(value)
    }

    fun language(
        value: AppLanguage
    ) = viewModelScope.launch {
        repo.setLanguage(value)
    }
}