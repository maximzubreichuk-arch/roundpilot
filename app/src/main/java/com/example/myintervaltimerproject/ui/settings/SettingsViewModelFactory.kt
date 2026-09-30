package com.example.myintervaltimerproject.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myintervaltimerproject.data.preferences.PreferencesRepository
class SettingsViewModelFactory(private val repo:PreferencesRepository):ViewModelProvider.Factory{override fun <T:ViewModel> create(modelClass:Class<T>):T{if(modelClass.isAssignableFrom(SettingsViewModel::class.java)) @Suppress("UNCHECKED_CAST") return SettingsViewModel(repo) as T;throw IllegalArgumentException("Unknown ViewModel")}}
