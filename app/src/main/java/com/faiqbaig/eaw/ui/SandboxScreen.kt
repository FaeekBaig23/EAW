package com.faiqbaig.eaw.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.faiqbaig.eaw.core.Faction
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitSubtype
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onExitToMenu: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showDeploymentPanel by remember { mutableStateOf(false) }

    // Roster Definition
    val deployableRoster = listOf(
        Pair(UnitClass.COMMANDER, UnitSubtype.NONE),
        Pair(UnitClass.INFANTRY, UnitSubtype.LIGHT),
        Pair(UnitClass.INFANTRY, UnitSubtype.LINE),
        Pair(UnitClass.INFANTRY, UnitSubtype.GRENADIER),
        Pair(UnitClass.CAVALRY, UnitSubtype.LIGHT),
        Pair(UnitClass.CAVALRY, UnitSubtype.HEAVY),
        Pair(UnitClass.ARTILLERY, UnitSubtype.NONE)
    )

    // Track which player's deployment tab is active
    var deployingForPlayer1 by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. The Game Map (Bottom Layer)
        SandboxMapCanvas(
            units = viewModel.units,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top UI Overlay (Settings & Title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings Icon Button
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .border(1.dp, BrightYellow, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = BrightYellow
                )
            }

            Text(
                text = "SANDBOX MODE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        blurRadius = 4f
                    )
                )
            )

            // Spacer to balance the top row centering
            Spacer(modifier = Modifier.width(48.dp))
        }

        // 3. Deployment Toggle Button & Panel (Bottom Overlay)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Deployment Panel (Expands upwards)
            if (showDeploymentPanel) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = Color.Black.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, BrightYellow.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Tab Row to switch between Player 1 and Player 2 rosters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { deployingForPlayer1 = true }) {
                                Text(
                                    text = "P1: ${viewModel.player1Faction.name}",
                                    color = if (deployingForPlayer1) BrightYellow else Color.Gray,
                                    fontWeight = if (deployingForPlayer1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            TextButton(onClick = { deployingForPlayer1 = false }) {
                                Text(
                                    text = "P2: ${viewModel.player2Faction.name}",
                                    color = if (!deployingForPlayer1) BrightYellow else Color.Gray,
                                    fontWeight = if (!deployingForPlayer1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        val activeFaction = if (deployingForPlayer1) viewModel.player1Faction else viewModel.player2Faction
                        val context = LocalContext.current
                        val flagBitmap = ImageBitmap.imageResource(context.resources, activeFaction.flagResId)

                        // Scrollable Row of Deployable Units
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(deployableRoster) { (unitClass, subtype) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        // TODO: Implement Drag-to-deploy or Click-to-spawn logic
                                        println("Clicked to deploy: $unitClass - $subtype")
                                    }
                                ) {
                                    // 80x80 container for the sprite
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.size(60.dp)) {
                                            // Fixed: Use withTransform to wrap translation properly inside DrawScope
                                            withTransform({
                                                translate(left = size.width / 2, top = size.height / 2)
                                            }) {
                                                drawTacticalSprite(
                                                    unitClass = unitClass,
                                                    subtype = subtype,
                                                    factionColor = activeFaction.color,
                                                    flagBitmap = flagBitmap
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (subtype == UnitSubtype.NONE) unitClass.name else subtype.name,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Square Deployment Toggle Button
            IconButton(
                onClick = { showDeploymentPanel = !showDeploymentPanel },
                modifier = Modifier
                    .padding(bottom = 16.dp, top = 8.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp))
                    .border(1.dp, BrightYellow, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    imageVector = if (showDeploymentPanel) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Toggle Deployment",
                    tint = BrightYellow
                )
            }
        }
    }

    // Settings Dialog Overlay
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E221E), // Dark tactical background
                border = BorderStroke(1.5.dp, BrightYellow)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SANDBOX SETTINGS",
                        color = BrightYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Player 1 Faction Select
                    FactionSelector(
                        label = "Side 1 (Left)",
                        selectedFaction = viewModel.player1Faction,
                        opponentFaction = viewModel.player2Faction,
                        onFactionSelected = { viewModel.updatePlayer1Faction(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Player 2 Faction Select
                    FactionSelector(
                        label = "Side 2 (Right)",
                        selectedFaction = viewModel.player2Faction,
                        opponentFaction = viewModel.player1Faction,
                        onFactionSelected = { viewModel.updatePlayer2Faction(it) }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { onExitToMenu() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Text("Exit to Menu")
                        }

                        Button(
                            onClick = { showSettings = false },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightYellow, contentColor = Color.Black)
                        ) {
                            Text("Resume", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper Composable for Faction Selection
@Composable
fun FactionSelector(
    label: String,
    selectedFaction: Faction,
    opponentFaction: Faction,
    onFactionSelected: (Faction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Faction.entries.forEach { faction ->
                val isSelected = faction == selectedFaction
                val isOpponent = faction == opponentFaction

                // Highlight selected, hide/dim opponent's choice to prevent duplicate selection
                val borderColor = if (isSelected) BrightYellow else Color.Transparent
                val opacity = if (isOpponent) 0.2f else 1.0f

                Image(
                    painter = painterResource(id = faction.flagResId),
                    contentDescription = faction.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp, 32.dp)
                        .alpha(opacity)
                        .border(2.dp, borderColor, RoundedCornerShape(2.dp))
                        .clickable(enabled = !isOpponent) { // Prevents clicking the opponent's faction
                            onFactionSelected(faction)
                        }
                )
            }
        }
    }
}