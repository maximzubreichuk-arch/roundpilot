package com.example.myintervaltimerproject.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myintervaltimerproject.ui.theme.Dimens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier =
            modifier.height(
                Dimens.PrimaryButtonHeight
            ),
        shape =
            RoundedCornerShape(
                26.dp
            )
    ) {
        Text(text)
    }
}

@Composable
fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Surface(
            onClick = onClick,
            shape =
                RoundedCornerShape(
                    Dimens.CardCornerRadius
                ),
            tonalElevation = 0.dp,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = value,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall
                )

                Text(
                    text =
                        "Tap to change",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimerRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor =
        MaterialTheme
            .colorScheme
            .outline
            .copy(
                alpha = 0.35f
            )

    Canvas(
        modifier = modifier
    ) {

        /*
         * Thick workout progress ring.
         */
        val strokeWidth =
            16.dp.toPx()

        // ---------------------------------------------------------
        // BACKGROUND TRACK
        // ---------------------------------------------------------

        drawArc(
            color =
                trackColor,

            startAngle =
                -90f,

            sweepAngle =
                360f,

            useCenter =
                false,

            style =
                Stroke(
                    width =
                        strokeWidth,

                    cap =
                        StrokeCap.Round
                )
        )

        // ---------------------------------------------------------
        // ACTIVE PROGRESS
        // ---------------------------------------------------------

        drawArc(
            color =
                color,

            startAngle =
                -90f,

            sweepAngle =
                360f *
                        progress.coerceIn(
                            0f,
                            1f
                        ),

            useCenter =
                false,

            style =
                Stroke(
                    width =
                        strokeWidth,

                    cap =
                        StrokeCap.Round
                )
        )
    }
}