package com.example.myintervaltimerproject.ui.components

import android.util.Log

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun HoldableControl(
    text: String,
    onStep: () -> Unit,
    modifier: Modifier = Modifier
) {

    /*
     * IMPORTANT:
     *
     * pointerInput(Unit) must always use the latest
     * onStep callback.
     *
     * Without rememberUpdatedState(), pointerInput may
     * keep an old lambda after recomposition.
     *
     * This was the reason why holding + / - could change
     * the value only once or keep applying the old value.
     */
    val currentOnStep by
    rememberUpdatedState(
        newValue = onStep
    )


    Box(

        modifier =
            modifier

                .size(
                    width = 42.dp,
                    height = 42.dp
                )

                .onGloballyPositioned {

                    Log.d(
                        "HoldableControl",
                        "$text -> " +
                                "width=${it.size.width}, " +
                                "height=${it.size.height}"
                    )
                }

                .clip(
                    CircleShape
                )

                .background(
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(
                            alpha = 0.10f
                        )
                )

                .pointerInput(Unit) {

                    detectTapGestures(

                        onPress = {

                            coroutineScope {

                                var longPressStarted =
                                    false


                                val repeatJob =
                                    launch {

                                        delay(
                                            350L
                                        )


                                        longPressStarted =
                                            true


                                        while (true) {

                                            currentOnStep()

                                            delay(
                                                80L
                                            )
                                        }
                                    }


                                val released =
                                    tryAwaitRelease()


                                repeatJob.cancel()


                                /*
                                 * A normal short tap performs
                                 * exactly one step.
                                 *
                                 * A long press has already been
                                 * processed by repeatJob.
                                 */
                                if (
                                    released &&
                                    !longPressStarted
                                ) {

                                    currentOnStep()
                                }
                            }
                        }
                    )
                },

        contentAlignment =
            Alignment.Center
    ) {

        Text(

            text =
                text,

            fontSize =
                20.sp,

            lineHeight =
                20.sp,

            fontWeight =
                FontWeight.Medium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}