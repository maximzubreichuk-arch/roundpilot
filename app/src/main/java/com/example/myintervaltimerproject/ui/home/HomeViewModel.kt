package com.example.myintervaltimerproject.ui.home

import androidx.lifecycle.ViewModel

import com.example.myintervaltimerproject.timer.TimerConfig

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class HomeViewModel : ViewModel() {

    private val _config =
        MutableStateFlow(
            TimerConfig()
        )


    val config: StateFlow<TimerConfig> =
        _config.asStateFlow()


    // ---------------------------------------------------------
    // WORK
    // ---------------------------------------------------------

    fun updateWork(
        seconds: Int
    ) {

        _config.value =
            _config.value.copy(

                workSeconds =
                    seconds.coerceAtLeast(
                        MIN_WORK_SECONDS
                    )
            )
    }


    // ---------------------------------------------------------
    // REST
    // ---------------------------------------------------------

    fun updateRest(
        seconds: Int
    ) {

        _config.value =
            _config.value.copy(

                restSeconds =
                    seconds.coerceAtLeast(
                        MIN_REST_SECONDS
                    )
            )
    }


    // ---------------------------------------------------------
    // ROUNDS
    // ---------------------------------------------------------

    fun updateRounds(
        rounds: Int
    ) {

        _config.value =
            _config.value.copy(

                rounds =
                    rounds.coerceIn(
                        MIN_ROUNDS,
                        MAX_ROUNDS
                    )
            )
    }


    // ---------------------------------------------------------
    // WARM-UP
    // ---------------------------------------------------------

    fun updateWarmUp(
        seconds: Int
    ) {

        _config.value =
            _config.value.copy(

                warmUpSeconds =
                    seconds.coerceAtLeast(
                        MIN_WARM_UP_SECONDS
                    )
            )
    }


    // ---------------------------------------------------------
    // COOLDOWN
    // ---------------------------------------------------------

    fun updateCooldown(
        seconds: Int
    ) {

        _config.value =
            _config.value.copy(

                cooldownSeconds =
                    seconds.coerceAtLeast(
                        MIN_COOLDOWN_SECONDS
                    )
            )
    }


    private companion object {

        const val MIN_WORK_SECONDS =
            5

        const val MIN_REST_SECONDS =
            0

        const val MIN_ROUNDS =
            1

        const val MAX_ROUNDS =
            20

        const val MIN_WARM_UP_SECONDS =
            0

        const val MIN_COOLDOWN_SECONDS =
            0
    }
}