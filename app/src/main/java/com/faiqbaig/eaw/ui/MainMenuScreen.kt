package com.faiqbaig.eaw.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onStartNewClick: () -> Unit,
    onLoadGameClick: () -> Unit,
    onExitClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // A Box allows us to overlap elements, perfect for pinning the Settings icon to the corner
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Settings Icon (Top Right)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }

        // Main Content (Centered Title and Buttons)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Corps: Europe at War",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStartNewClick,
                modifier = Modifier.width(200.dp)
            ) {
                Text("Start New")
            }

            Button(
                onClick = onLoadGameClick,
                modifier = Modifier.width(200.dp)
            ) {
                Text("Load Game")
            }

            OutlinedButton(
                onClick = onExitClick,
                modifier = Modifier.width(200.dp)
            ) {
                Text("Exit")
            }
        }
    }
}