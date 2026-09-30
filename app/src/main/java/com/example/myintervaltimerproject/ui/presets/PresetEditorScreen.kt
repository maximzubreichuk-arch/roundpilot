package com.example.myintervaltimerproject.ui.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.data.presets.Preset
import com.example.myintervaltimerproject.timer.TimerConfig
import com.example.myintervaltimerproject.ui.components.HoldableControl


private const val MAX_PRESETS =
    5

private const val MIN_WORK_SECONDS =
    5

private const val MIN_REST_SECONDS =
    0

private const val MIN_ROUNDS =
    1

private const val MAX_ROUNDS =
    20

private const val MIN_WARM_UP_SECONDS =
    0

private const val MIN_COOLDOWN_SECONDS =
    0


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorScreen(
    presetId: Long?,
    onBack: () -> Unit,
) {

    val app =
        LocalContext.current.applicationContext
                as IntervalTimerApp


    /*
     * null = create new preset
     * value = edit existing preset
     */
    val editingPreset =
        remember(
            presetId
        ) {

            presetId?.let { id ->

                app.presets
                    .load()
                    .find {
                        it.id == id
                    }
            }
        }


    val isEditing =
        editingPreset != null


    // ---------------------------------------------------------
    // NAME
    // ---------------------------------------------------------

    var name by remember(
        presetId
    ) {

        mutableStateOf(
            editingPreset?.name
                ?: ""
        )
    }


    // ---------------------------------------------------------
    // WORK
    // ---------------------------------------------------------

    var work by remember(
        presetId
    ) {

        mutableIntStateOf(

            (
                    editingPreset
                        ?.config
                        ?.workSeconds
                        ?: 30
                    ).coerceAtLeast(
                    MIN_WORK_SECONDS
                )
        )
    }


    // ---------------------------------------------------------
    // REST
    // ---------------------------------------------------------

    var rest by remember(
        presetId
    ) {

        mutableIntStateOf(

            (
                    editingPreset
                        ?.config
                        ?.restSeconds
                        ?: 15
                    ).coerceAtLeast(
                    MIN_REST_SECONDS
                )
        )
    }


    // ---------------------------------------------------------
    // ROUNDS
    // ---------------------------------------------------------

    var rounds by remember(
        presetId
    ) {

        mutableIntStateOf(

            (
                    editingPreset
                        ?.config
                        ?.rounds
                        ?: 8
                    ).coerceIn(
                    MIN_ROUNDS,
                    MAX_ROUNDS
                )
        )
    }


    // ---------------------------------------------------------
    // GET READY
    // ---------------------------------------------------------

    var warmUp by remember(
        presetId
    ) {

        mutableIntStateOf(

            (
                    editingPreset
                        ?.config
                        ?.warmUpSeconds
                        ?: 0
                    ).coerceAtLeast(
                    MIN_WARM_UP_SECONDS
                )
        )
    }


    // ---------------------------------------------------------
    // COOLDOWN
    // ---------------------------------------------------------

    var cooldown by remember(
        presetId
    ) {

        mutableIntStateOf(

            (
                    editingPreset
                        ?.config
                        ?.cooldownSeconds
                        ?: 0
                    ).coerceAtLeast(
                    MIN_COOLDOWN_SECONDS
                )
        )
    }


    /*
     * When editing an existing preset, editing does not
     * consume another preset slot.
     */
    val canCreate =
        isEditing ||
                app.presets.load().size <
                MAX_PRESETS


    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(

                        text =
                            if (
                                isEditing
                            ) {

                                stringResource(
                                    R.string
                                        .edit_preset_title
                                )

                            } else {

                                stringResource(
                                    R.string
                                        .new_preset
                                )
                            }
                    )
                },

                navigationIcon = {

                    TextButton(

                        onClick =
                            onBack
                    ) {

                        Text(

                            stringResource(
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
                    )
                    .padding(
                        24.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            if (
                !canCreate
            ) {

                Text(

                    text =
                        stringResource(
                            R.string
                                .maximum_presets_reached
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )


                Text(

                    text =
                        stringResource(
                            R.string
                                .delete_preset_before_creating
                        ),

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Spacer(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                TextButton(

                    onClick =
                        onBack,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(

                        stringResource(
                            R.string.back_to_presets
                        )
                    )
                }

            } else {

                // -------------------------------------------------
                // NAME
                // -------------------------------------------------

                OutlinedTextField(

                    value =
                        name,

                    onValueChange = {

                        name =
                            it
                    },

                    label = {

                        Text(

                            stringResource(
                                R.string.name
                            )
                        )
                    },

                    singleLine =
                        true,

                    modifier =
                        Modifier.fillMaxWidth()
                )


                // -------------------------------------------------
                // WORK
                // -------------------------------------------------

                SettingNumber(

                    label =
                        stringResource(
                            R.string.work_setting
                        ),

                    value =
                        work,

                    minimum =
                        MIN_WORK_SECONDS,

                    step =
                        5,

                    showTime =
                        true,

                    onChange = {

                        work =
                            it
                    }
                )


                // -------------------------------------------------
                // REST
                // -------------------------------------------------

                SettingNumber(

                    label =
                        stringResource(
                            R.string.rest_setting
                        ),

                    value =
                        rest,

                    minimum =
                        MIN_REST_SECONDS,

                    step =
                        5,

                    showTime =
                        true,

                    onChange = {

                        rest =
                            it
                    }
                )


                // -------------------------------------------------
                // ROUNDS
                // -------------------------------------------------

                SettingNumber(

                    label =
                        stringResource(
                            R.string.rounds_setting
                        ),

                    value =
                        rounds,

                    minimum =
                        MIN_ROUNDS,

                    maximum =
                        MAX_ROUNDS,

                    step =
                        1,

                    showTime =
                        false,

                    onChange = {

                        rounds =
                            it
                    }
                )


                // -------------------------------------------------
                // GET READY
                // -------------------------------------------------

                SettingNumber(

                    label =
                        stringResource(
                            R.string.get_ready_setting
                        ),

                    value =
                        warmUp,

                    minimum =
                        MIN_WARM_UP_SECONDS,

                    step =
                        5,

                    showTime =
                        true,

                    onChange = {

                        warmUp =
                            it
                    }
                )


                // -------------------------------------------------
                // COOLDOWN
                // -------------------------------------------------

                SettingNumber(

                    label =
                        stringResource(
                            R.string.cooldown_setting
                        ),

                    value =
                        cooldown,

                    minimum =
                        MIN_COOLDOWN_SECONDS,

                    step =
                        5,

                    showTime =
                        true,

                    onChange = {

                        cooldown =
                            it
                    }
                )


                Spacer(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                val defaultPresetName =
                    stringResource(
                        R.string.my_preset
                    )


                // -------------------------------------------------
                // SAVE
                // -------------------------------------------------

                Button(

                    onClick = {

                        val presetName =
                            name
                                .trim()
                                .ifBlank {

                                    defaultPresetName
                                }


                        val config =
                            TimerConfig(

                                workSeconds =
                                    work.coerceAtLeast(
                                        MIN_WORK_SECONDS
                                    ),

                                restSeconds =
                                    rest.coerceAtLeast(
                                        MIN_REST_SECONDS
                                    ),

                                rounds =
                                    rounds.coerceIn(
                                        MIN_ROUNDS,
                                        MAX_ROUNDS
                                    ),

                                warmUpSeconds =
                                    warmUp.coerceAtLeast(
                                        MIN_WARM_UP_SECONDS
                                    ),

                                cooldownSeconds =
                                    cooldown.coerceAtLeast(
                                        MIN_COOLDOWN_SECONDS
                                    )
                            )


                        if (
                            isEditing
                        ) {

                            val updatedPreset =
                                editingPreset!!.copy(

                                    name =
                                        presetName,

                                    config =
                                        config
                                )


                            if (app.presets.update(updatedPreset)) {
                                app.analytics.logPresetUpdated()
                            }

                        } else {

                            /*
                             * Check the limit again at save time.
                             */
                            if (
                                app.presets
                                    .load()
                                    .size >=
                                MAX_PRESETS
                            ) {

                                return@Button
                            }


                            val newPreset =
                                Preset(

                                    id =
                                        System
                                            .currentTimeMillis(),

                                    name =
                                        presetName,

                                    config =
                                        config
                                )


                            if (app.presets.add(newPreset)) {
                                app.analytics.logPresetCreated()
                            }
                        }


                        onBack()
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                52.dp
                            )
                ) {

                    Text(

                        text =
                            if (
                                isEditing
                            ) {

                                stringResource(
                                    R.string
                                        .save_changes
                                )

                            } else {

                                stringResource(
                                    R.string.save
                                )
                            }
                    )
                }
            }
        }
    }
}


// -------------------------------------------------------------
// NUMBER SETTING
// -------------------------------------------------------------

@Composable
private fun SettingNumber(

    label: String,

    value: Int,

    minimum: Int,

    maximum: Int =
        Int.MAX_VALUE,

    step: Int,

    showTime: Boolean =
        true,

    onChange: (
        Int
    ) -> Unit,

    ) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(

            text =
                label,

            modifier =
                Modifier.weight(
                    1f
                )
        )


        // -----------------------------------------------------
        // MINUS
        // -----------------------------------------------------

        HoldableControl(

            text =
                "−",

            onStep = {

                onChange(

                    (
                            value - step
                            ).coerceIn(
                            minimum,
                            maximum
                        )
                )
            }
        )


        // -----------------------------------------------------
        // VALUE
        // -----------------------------------------------------

        Text(

            text =
                if (
                    showTime
                ) {

                    formatDuration(
                        value
                    )

                } else {

                    value.toString()
                },

            modifier =
                Modifier.width(
                    56.dp
                ),

            textAlign =
                TextAlign.Center
        )


        // -----------------------------------------------------
        // PLUS
        // -----------------------------------------------------

        HoldableControl(

            text =
                "+",

            onStep = {

                val nextValue =

                    if (
                        value >
                        Int.MAX_VALUE - step
                    ) {

                        Int.MAX_VALUE

                    } else {

                        value + step
                    }


                onChange(

                    nextValue.coerceIn(
                        minimum,
                        maximum
                    )
                )
            }
        )
    }
}


// -------------------------------------------------------------
// FORMAT DURATION
// -------------------------------------------------------------

private fun formatDuration(
    seconds: Int
): String {

    return "%02d:%02d".format(

        seconds / 60,

        seconds % 60
    )
}