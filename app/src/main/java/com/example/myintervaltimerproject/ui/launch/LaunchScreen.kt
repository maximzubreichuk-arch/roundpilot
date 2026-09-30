package com.example.myintervaltimerproject.ui.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.myintervaltimerproject.ui.theme.Dimens
import kotlinx.coroutines.delay

@Composable
fun LaunchScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(850)
        onFinished()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(Modifier.size(96.dp)) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            8.dp.toPx()
                        )
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(com.example.myintervaltimerproject.R.string.app_name), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
