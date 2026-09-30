package com.example.myintervaltimerproject.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.myintervaltimerproject.IntervalTimerApp


class WorkoutViewModelFactory(
    private val app: IntervalTimerApp
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                WorkoutViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(
                controller =
                    app.timerSessionController
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}