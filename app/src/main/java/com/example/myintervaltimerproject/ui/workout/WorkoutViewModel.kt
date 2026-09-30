package com.example.myintervaltimerproject.ui.workout

import androidx.lifecycle.ViewModel

import com.example.myintervaltimerproject.timer.TimerConfig
import com.example.myintervaltimerproject.timer.TimerSessionController
import com.example.myintervaltimerproject.timer.TimerState

import kotlinx.coroutines.flow.StateFlow


class WorkoutViewModel(
    private val controller: TimerSessionController
) : ViewModel() {

    val state:
            StateFlow<TimerState> =
        controller.state


    fun start(
        config: TimerConfig
    ) =
        controller.start(
            config
        )


    fun pause() =
        controller.pause()


    fun resume() =
        controller.resume()


    fun skip() =
        controller.skip()


    fun stop() =
        controller.stop()


    /*
     * IMPORTANT:
     *
     * TimerSessionController is owned by IntervalTimerApp
     * and is shared with the Foreground Service.
     *
     * Therefore ViewModel must NOT call controller.close()
     * from onCleared().
     *
     * Destroying WorkoutScreen must not stop the global
     * timer controller or the background workout.
     */
    override fun onCleared() {

        super.onCleared()
    }
}