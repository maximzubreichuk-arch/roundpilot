package com.example.myintervaltimerproject.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import com.example.myintervaltimerproject.R

@Composable
fun AppBackground(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    if (darkTheme) {
                        DarkBackground
                    } else {
                        LightBackground
                    }
                )
    ) {

        if (darkTheme) {

            // -----------------------------------------------------
            // DARK
            // -----------------------------------------------------

            Image(
                painter =
                    painterResource(
                        R.drawable.aurora_dark_background
                    ),

                contentDescription =
                    null,

                modifier =
                    Modifier.fillMaxSize(),

                contentScale =
                    ContentScale.Crop
            )

            /*
             * Very subtle dark overlay.
             *
             * Keeps the image slightly deeper.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = 0.08f
                            )
                        )
            )

        } else {

            // -----------------------------------------------------
            // LIGHT
            // -----------------------------------------------------

            /*
             * Deliberately no image and no gradient.
             *
             * Clean premium off-white background.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            LightBackground
                        )
            )
        }

        // ---------------------------------------------------------
        // CONTENT
        // ---------------------------------------------------------

        content()
    }
}