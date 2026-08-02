package com.faiqbaig.eaw.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModeSelectScreen(
    onSandboxClick: () -> Unit,
    onConquestClick: () -> Unit,
    onBackClick: () -> Unit
) {
    SlideshowBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Screen Title
            Text(
                text = "SELECT CAMPAIGN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BrightYellow,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Mode Selection Cards (Centered Side-by-Side or Stacked)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.855f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sandbox Mode Card
                ModeCard(
                    modifier = Modifier.weight(1f),
                    title = "Sandbox Mode",
                    description = "Play freely without strict campaign constraints. Test strategies, armies, and maps at your own pace.",
                    onClick = onSandboxClick
                )

                // Conquest Mode Card
                ModeCard(
                    modifier = Modifier.weight(1f),
                    title = "Conquest",
                    description = "Lead your empire through historical Napoleonic campaigns, managing resources and territory on a grand scale.",
                    onClick = onConquestClick
                )
            }

            // Back Button (Bottom Left)
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(130.dp),
                border = BorderStroke(1.5.dp, BrightYellow),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Black,
                    contentColor = BrightYellow
                )
            ) {
                Text("Back", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModeCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, BrightYellow),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black.copy(alpha = 0.8f),
            contentColor = BrightYellow
        ),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrightYellow
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.LightGray,
                lineHeight = 16.sp
            )
        }
    }
}