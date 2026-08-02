package com.faiqbaig.eaw.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrightYellow = Color(0xFFFFD700)

@Composable
fun MainMenuScreen(
    onStartNewClick: () -> Unit,
    onLoadGameClick: () -> Unit,
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    SlideshowBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Placeholder Logo / Title (Upper Left)
            Text(
                text = "EAW",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrightYellow
                ),
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Settings Icon (Upper Right) - Changed tint to Grey
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Gray
                )
            }

            // Compact Buttons with Solid Black Fill (Lower Right Corner)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val buttonModifier = Modifier.width(130.dp)

                // Solid black background with yellow text
                val buttonColors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Black,
                    contentColor = BrightYellow
                )
                val buttonBorder = BorderStroke(1.5.dp, BrightYellow)

                OutlinedButton(
                    onClick = onStartNewClick,
                    modifier = buttonModifier,
                    border = buttonBorder,
                    colors = buttonColors,
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Start New", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLoadGameClick,
                    modifier = buttonModifier,
                    border = buttonBorder,
                    colors = buttonColors,
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Load Game", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExitClick,
                    modifier = buttonModifier,
                    border = buttonBorder,
                    colors = buttonColors,
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Exit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}