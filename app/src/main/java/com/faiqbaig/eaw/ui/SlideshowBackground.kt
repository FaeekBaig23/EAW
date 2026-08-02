package com.faiqbaig.eaw.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.faiqbaig.eaw.R

@Composable
fun SlideshowBackground(
    content: @Composable () -> Unit
) {
    val images = listOf(
        R.drawable.ss1, R.drawable.ss2, R.drawable.ss3,
        R.drawable.ss4, R.drawable.ss5, R.drawable.ss6,
        R.drawable.ss7, R.drawable.ss8, R.drawable.ss9
    )

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(12000)
            currentIndex = (currentIndex + 1) % images.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = images[currentIndex],
            animationSpec = tween(durationMillis = 2000),
            label = "Slideshow Fade"
        ) { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Historical Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Display screens directly over full-brightness background
        content()
    }
}