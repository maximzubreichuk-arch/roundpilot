package com.example.myintervaltimerproject.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myintervaltimerproject.IntervalTimerApp
import com.example.myintervaltimerproject.R
import com.example.myintervaltimerproject.data.presets.Preset
import com.example.myintervaltimerproject.timer.TimerConfig
import com.example.myintervaltimerproject.ui.components.HoldableControl

private const val MAX_PRESETS = 5
private const val MAX_TIME_SECONDS = Int.MAX_VALUE

@Composable
fun HomeScreen(
    onStart: (TimerConfig, String) -> Unit,
    onAddPreset: () -> Unit,
    onEditPreset: (Long) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val config by vm.config.collectAsState()

    val app =
        LocalContext.current.applicationContext
                as IntervalTimerApp

    val presets by app.presets.items.collectAsState()

    var presetToDelete by remember {
        mutableStateOf<Preset?>(null)
    }

    var showLimitDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 16.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(0.dp)
        ) {

            // ---------------------------------------------------------
            // HEADER
            // ---------------------------------------------------------

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.Top,

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                stringResource(
                                    R.string.app_name
                                ),

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick =
                            onSettings,

                        modifier =
                            Modifier.size(46.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Settings,

                            contentDescription =
                                stringResource(
                                    R.string
                                        .settings_content_description
                                ),

                            modifier =
                                Modifier.size(27.dp)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )
            }

            // ---------------------------------------------------------
            // TIMER SETTINGS
            // ---------------------------------------------------------

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly,

                    verticalAlignment =
                        Alignment.Top
                ) {

                    TimerSetting(
                        modifier =
                            Modifier.weight(1f),

                        label =
                            stringResource(
                                R.string.work
                            ),

                        value =
                            config.workSeconds,

                        onMinus = {

                            vm.updateWork(
                                (
                                        config.workSeconds - 5
                                        ).coerceAtLeast(5)
                            )
                        },

                        onPlus = {

                            vm.updateWork(
                                (
                                        config.workSeconds + 5
                                        ).coerceAtMost(
                                        MAX_TIME_SECONDS
                                    )
                            )
                        }
                    )

                    TimerSetting(
                        modifier =
                            Modifier.weight(1f),

                        label =
                            stringResource(
                                R.string.rest
                            ),

                        value =
                            config.restSeconds,

                        onMinus = {

                            vm.updateRest(
                                (
                                        config.restSeconds - 5
                                        ).coerceAtLeast(0)
                            )
                        },

                        onPlus = {

                            vm.updateRest(
                                (
                                        config.restSeconds + 5
                                        ).coerceAtMost(
                                        MAX_TIME_SECONDS
                                    )
                            )
                        }
                    )

                    TimerSetting(
                        modifier =
                            Modifier.weight(1f),

                        label =
                            stringResource(
                                R.string.rounds
                            ),

                        value =
                            config.rounds,

                        showTime = false,

                        onMinus = {

                            vm.updateRounds(
                                (
                                        config.rounds - 1
                                        ).coerceAtLeast(1)
                            )
                        },

                        onPlus = {

                            vm.updateRounds(
                                (
                                        config.rounds + 1
                                        ).coerceAtMost(20)
                            )
                        }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(32.dp)
                )
            }

            // ---------------------------------------------------------
            // START
            // ---------------------------------------------------------

            item {

                Button(
                    onClick = {
                        onStart(config, "quick_start")
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),

                    shape =
                        RoundedCornerShape(20.dp)
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string.start
                            ),

                        fontSize = 18.sp,

                        fontWeight =
                            FontWeight.Medium
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )
            }

            // ---------------------------------------------------------
            // QUICK PRESETS HEADER
            // ---------------------------------------------------------

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text =
                            stringResource(
                                R.string.quick_presets
                            ),

                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = {

                            if (
                                presets.size <
                                MAX_PRESETS
                            ) {
                                onAddPreset()
                            } else {
                                showLimitDialog = true
                            }
                        },

                        modifier =
                            Modifier.size(44.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Add,

                            contentDescription =
                                stringResource(
                                    R.string
                                        .add_preset_content_description
                                ),

                            modifier =
                                Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }

            // ---------------------------------------------------------
            // PRESETS
            // ---------------------------------------------------------

            if (presets.isEmpty()) {

                item {

                    Text(
                        text =
                            stringResource(
                                R.string.no_presets_yet
                            ),

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        modifier =
                            Modifier.padding(
                                vertical = 12.dp
                            )
                    )
                }

            } else {

                items(
                    items = presets,
                    key = { it.id }
                ) { preset ->

                    PresetCard(
                        preset = preset,

                        onPlay = {
                            onStart(preset.config, "preset")
                            app.analytics.logPresetStarted()
                        },

                        onEdit = {
                            onEditPreset(
                                preset.id
                            )
                        },

                        onDelete = {
                            presetToDelete =
                                preset
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DELETE DIALOG
    // -------------------------------------------------------------

    presetToDelete?.let { preset ->

        AlertDialog(
            onDismissRequest = {
                presetToDelete = null
            },

            title = {

                Text(
                    stringResource(
                        R.string.delete_preset_title
                    )
                )
            },

            text = {

                Text(
                    stringResource(
                        R.string.delete_preset_message,
                        preset.name
                    )
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        app.presets.delete(preset.id)
                        app.analytics.logPresetDeleted()

                        presetToDelete = null
                    }
                ) {

                    Text(
                        stringResource(
                            R.string.delete
                        )
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        presetToDelete = null
                    }
                ) {

                    Text(
                        stringResource(
                            R.string.cancel
                        )
                    )
                }
            }
        )
    }

    // -------------------------------------------------------------
    // LIMIT DIALOG
    // -------------------------------------------------------------

    if (showLimitDialog) {

        AlertDialog(
            onDismissRequest = {
                showLimitDialog = false
            },

            title = {

                Text(
                    stringResource(
                        R.string.preset_limit_title
                    )
                )
            },

            text = {

                Text(
                    stringResource(
                        R.string.preset_limit_message
                    )
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showLimitDialog = false
                    }
                ) {

                    Text(
                        stringResource(
                            R.string.ok
                        )
                    )
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TIMER SETTING
// -------------------------------------------------------------

@Composable
private fun TimerSetting(
    modifier: Modifier,
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    showTime: Boolean = true,
) {
    Surface(
        modifier =
            modifier
                .padding(
                    horizontal = 4.dp
                )
                .height(154.dp),

        shape =
            RoundedCornerShape(16.dp),

        color =
            MaterialTheme
                .colorScheme
                .surface,

        tonalElevation =
            0.dp,

        shadowElevation =
            0.dp,

        border =
            androidx.compose.foundation
                .BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .outline
                            .copy(
                                alpha = 0.55f
                            )
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 12.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = label,

                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

                fontWeight =
                    FontWeight.Medium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    if (showTime) {
                        formatDuration(value)
                    } else {
                        value.toString()
                    },

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                HoldableControl(
                    text = "−",

                    modifier =
                        Modifier.requiredSize(
                            42.dp
                        ),

                    onStep =
                        onMinus
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                HoldableControl(
                    text = "+",

                    modifier =
                        Modifier.requiredSize(
                            42.dp
                        ),

                    onStep =
                        onPlus
                )
            }
        }
    }
}
// -------------------------------------------------------------
// PRESET CARD
// -------------------------------------------------------------

@Composable
private fun PresetCard(
    preset: Preset,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(66.dp),

        shape =
            RoundedCornerShape(14.dp),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant,

        tonalElevation = 0.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = 12.dp,
                        end = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                modifier =
                    Modifier.size(40.dp),

                shape =
                    RoundedCornerShape(10.dp),

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(
                            alpha = 0.14f
                        )
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            presetSymbol(
                                preset.name
                            ),

                        fontSize = 22.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(10.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        preset.name,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text =
                        buildPresetDescription(
                            preset
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier =
                    Modifier.size(6.dp)
            )

            // ---------------------------------------------------------
            // PLAY
            // ---------------------------------------------------------

            Surface(
                onClick =
                    onPlay,

                modifier =
                    Modifier.size(52.dp),

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(
                            alpha = 0.14f
                        )
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.PlayArrow,

                        contentDescription =
                            stringResource(
                                R.string
                                    .start_preset_content_description,
                                preset.name
                            ),

                        modifier =
                            Modifier.size(30.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(6.dp)
            )

            // ---------------------------------------------------------
            // EDIT
            // ---------------------------------------------------------

            IconButton(
                onClick =
                    onEdit,

                modifier =
                    Modifier.size(40.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Edit,

                    contentDescription =
                        stringResource(
                            R.string
                                .edit_preset_content_description,
                            preset.name
                        ),

                    modifier =
                        Modifier.size(20.dp)
                )
            }

            // ---------------------------------------------------------
            // DELETE
            // ---------------------------------------------------------

            IconButton(
                onClick =
                    onDelete,

                modifier =
                    Modifier.size(40.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Delete,

                    contentDescription =
                        stringResource(
                            R.string
                                .delete_preset_content_description,
                            preset.name
                        ),

                    modifier =
                        Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// PRESET SYMBOL
// -------------------------------------------------------------

private fun presetSymbol(
    name: String
): String {

    val normalized =
        name
            .trim()
            .lowercase()
            .replace('ё', 'е')
            .replace(
                Regex("[^\\p{L}\\p{N}\\s-]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )

    val words =
        normalized
            .split(" ")
            .filter {
                it.isNotBlank()
            }
            .toSet()

    fun hasWord(
        vararg values: String
    ): Boolean {
        return values.any {
            it in words
        }
    }

    fun contains(
        vararg values: String
    ): Boolean {
        return values.any {
            normalized.contains(it)
        }
    }

    return when {

        // -----------------------------------------------------
        // HIIT / INTERVAL
        // -----------------------------------------------------

        hasWord(
            "hiit",
            "хит"
        ) ||
                contains(
                    "interval",
                    "интервальн"
                ) ->
            "⚡"

        // -----------------------------------------------------
        // TABATA
        // -----------------------------------------------------

        hasWord(
            "tabata",
            "табата"
        ) ->
            "⏱"

        // -----------------------------------------------------
        // BOXING
        // -----------------------------------------------------

        hasWord(
            "boxing",
            "box",
            "бокс",
            "боксинг",
            "sparring",
            "спаринг",
            "спарринг",
            "punch",
            "punches"
        ) ||
                contains(
                    "удар"
                ) ->
            "🥊"

        // -----------------------------------------------------
        // RUNNING
        // -----------------------------------------------------

        hasWord(
            "running",
            "run",
            "jog",
            "jogging"
        ) ||
                contains(
                    "бег",
                    "пробеж",
                    "джог"
                ) ->
            "🏃"

        // -----------------------------------------------------
        // WALKING
        // -----------------------------------------------------

        hasWord(
            "walking",
            "walk"
        ) ||
                contains(
                    "ходьб",
                    "прогул"
                ) ->
            "🚶"

        // -----------------------------------------------------
        // SQUATS
        // -----------------------------------------------------

        hasWord(
            "squat",
            "squats"
        ) ||
                contains(
                    "присед"
                ) ->
            "🏋️"

        // -----------------------------------------------------
        // LUNGES
        // -----------------------------------------------------

        hasWord(
            "lunge",
            "lunges"
        ) ||
                contains(
                    "выпад"
                ) ->
            "🦵"

        // -----------------------------------------------------
        // PUSH-UPS
        // -----------------------------------------------------

        hasWord(
            "push-up",
            "pushups",
            "pushup",
            "push"
        ) ||
                contains(
                    "отжим"
                ) ->
            "🤸"

        // -----------------------------------------------------
        // PULL-UPS
        // -----------------------------------------------------

        hasWord(
            "pull-up",
            "pullups",
            "pullup",
            "pull"
        ) ||
                contains(
                    "подтяг",
                    "турник"
                ) ->
            "🙆"

        // -----------------------------------------------------
        // PLANK
        // -----------------------------------------------------

        hasWord(
            "plank",
            "планка",
            "планк"
        ) ->
            "🧘"

        // -----------------------------------------------------
        // BURPEES
        // -----------------------------------------------------

        hasWord(
            "burpee",
            "burpees"
        ) ||
                contains(
                    "берпи",
                    "бурпи"
                ) ->
            "🔥"

        // -----------------------------------------------------
        // JUMP ROPE
        // -----------------------------------------------------

        hasWord(
            "jumprope",
            "skipping",
            "rope"
        ) ||
                contains(
                    "jump rope",
                    "скакал"
                ) ->
            "🪢"

        // -----------------------------------------------------
        // JUMPING
        // -----------------------------------------------------

        contains(
            "jumping jack",
            "jump jack"
        ) ||
                hasWord(
                    "jumping",
                    "jumps"
                ) ||
                contains(
                    "прыжк",
                    "джамп"
                ) ->
            "⬆️"

        // -----------------------------------------------------
        // CYCLING
        // -----------------------------------------------------

        hasWord(
            "cycling",
            "cycle",
            "bike"
        ) ||
                contains(
                    "велосипед",
                    "велотренаж"
                ) ->
            "🚴"

        // -----------------------------------------------------
        // SWIMMING
        // -----------------------------------------------------

        hasWord(
            "swimming",
            "swim"
        ) ||
                contains(
                    "плавани",
                    "бассейн"
                ) ->
            "🏊"

        // -----------------------------------------------------
        // ROWING
        // -----------------------------------------------------

        hasWord(
            "rowing",
            "rower"
        ) ||
                contains(
                    "гребл",
                    "гребной"
                ) ->
            "🚣"

        // -----------------------------------------------------
        // DEADLIFT
        // -----------------------------------------------------

        hasWord(
            "deadlift"
        ) ||
                contains(
                    "становая"
                ) ->
            "🏋️"

        // -----------------------------------------------------
        // BENCH PRESS
        // -----------------------------------------------------

        contains(
            "bench press",
            "жим лежа"
        ) ->
            "🏋️"

        // -----------------------------------------------------
        // GLUTES / BOOTY
        // -----------------------------------------------------

        hasWord(
            "glute",
            "glutes",
            "booty",
            "butt",
            "ass"
        ) ||
                contains(
                    "ягодиц",
                    "ягодичн",
                    "жоп",
                    "поп",
                    "зад",
                    "hip thrust",
                    "мостик"
                ) ->
            "🍑"

        // -----------------------------------------------------
        // ARMS
        // -----------------------------------------------------

        hasWord(
            "arm",
            "arms",
            "guns",
            "bis",
            "tris"
        ) ||
                contains(
                    "рук",
                    "бицепс",
                    "бицух",
                    "трицепс",
                    "трицух"
                ) ->
            "💪"

        // -----------------------------------------------------
        // LEGS
        // -----------------------------------------------------

        hasWord(
            "leg",
            "legs"
        ) ||
                contains(
                    "ног",
                    "ляжк",
                    "бедр",
                    "квадрицепс",
                    "квадр",
                    "thigh"
                ) ->
            "🦵"

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        hasWord(
            "back",
            "lats",
            "lat"
        ) ||
                contains(
                    "спин",
                    "широчайш",
                    "крыль",
                    "крылыш"
                ) ->
            "🧍"

        // -----------------------------------------------------
        // SHOULDERS
        // -----------------------------------------------------

        hasWord(
            "shoulder",
            "shoulders",
            "delts",
            "deltoid"
        ) ||
                contains(
                    "плеч",
                    "дельт"
                ) ->
            "💪"

        // -----------------------------------------------------
        // CHEST
        // -----------------------------------------------------

        hasWord(
            "chest",
            "pec",
            "pecs"
        ) ||
                contains(
                    "груд"
                ) ->
            "🫁"

        // -----------------------------------------------------
        // ABS / CORE
        // -----------------------------------------------------

        hasWord(
            "abs",
            "core",
            "crunch",
            "situp"
        ) ||
                contains(
                    "пресс",
                    "живот",
                    "кубик",
                    "скручива",
                    "six-pack",
                    "six pack"
                ) ->
            "💪"

        // -----------------------------------------------------
        // STRETCHING
        // -----------------------------------------------------

        hasWord(
            "stretch",
            "stretching"
        ) ||
                contains(
                    "растяж"
                ) ->
            "🧘"

        // -----------------------------------------------------
        // YOGA / MOBILITY
        // -----------------------------------------------------

        hasWord(
            "yoga",
            "mobility"
        ) ||
                contains(
                    "йог",
                    "мобильност"
                ) ->
            "🧘"

        // -----------------------------------------------------
        // WARM-UP / CHARGING
        // -----------------------------------------------------

        hasWord(
            "warmup"
        ) ||
                contains(
                    "warm up",
                    "warm-up",
                    "размин",
                    "зарядк",
                    "разогрев",
                    "раскачк"
                ) ->
            "🔥"

        // -----------------------------------------------------
        // COOL-DOWN
        // -----------------------------------------------------

        hasWord(
            "cooldown"
        ) ||
                contains(
                    "cool down",
                    "cool-down",
                    "замин",
                    "остыв"
                ) ->
            "🧘"

        // -----------------------------------------------------
        // CARDIO
        // -----------------------------------------------------

        hasWord(
            "cardio"
        ) ||
                contains(
                    "кардио"
                ) ->
            "❤️"

        // -----------------------------------------------------
        // STRENGTH / WEIGHTS
        // -----------------------------------------------------

        hasWord(
            "strength",
            "weights",
            "weight",
            "dumbbell",
            "dumbbells",
            "kettlebell"
        ) ||
                contains(
                    "силов",
                    "гантел",
                    "гир",
                    "желез"
                ) ->
            "🏋️"

        // -----------------------------------------------------
        // FULL BODY
        // -----------------------------------------------------

        contains(
            "full body",
            "full-body",
            "fullbody",
            "фулбоди",
            "фуллбоди",
            "все тело",
            "тело целиком"
        ) ->
            "🏋️"

        // -----------------------------------------------------
        // UPPER BODY
        // -----------------------------------------------------

        contains(
            "upper body",
            "верх тела",
            "верхняя часть тела"
        ) ->
            "💪"

        // -----------------------------------------------------
        // LOWER BODY
        // -----------------------------------------------------

        contains(
            "lower body",
            "низ тела",
            "нижняя часть тела"
        ) ->
            "🦵"

        // -----------------------------------------------------
        // GENERAL WORKOUT / GYM
        // -----------------------------------------------------

        hasWord(
            "workout",
            "training",
            "gym",
            "session",
            "зал"
        ) ||
                contains(
                    "трениров",
                    "треня",
                    "тренюх",
                    "качалк",
                    "кач"
                ) ->
            "🏋️"

        // -----------------------------------------------------
        // DEFAULT
        // -----------------------------------------------------

        else ->
            "◷"
    }
}

// -------------------------------------------------------------
// PRESET DESCRIPTION
// -------------------------------------------------------------

private fun buildPresetDescription(
    preset: Preset
): String {

    val config =
        preset.config

    return buildString {

        append(
            formatDuration(
                config.workSeconds
            )
        )

        append(" / ")

        append(
            formatDuration(
                config.restSeconds
            )
        )

        append(" × ")

        append(
            config.rounds
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