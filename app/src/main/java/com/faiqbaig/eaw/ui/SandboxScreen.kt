package com.faiqbaig.eaw.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SandboxScreen(
    onExitToMenu: () -> Unit
) {
    // A temporary dark background for your game UI shell
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        Text(
            text = "SANDBOX ENVIRONMENT INITIALIZED",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        Button(
            onClick = onExitToMenu,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text("Surrender (Menu)")
        }
    }
}