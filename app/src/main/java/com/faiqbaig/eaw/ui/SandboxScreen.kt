package com.faiqbaig.eaw.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.atan2

@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onExitToMenu: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showDeploymentPanel by remember { mutableStateOf(false) }

    // Select and Place State
    var pendingDeployment by remember { mutableStateOf<Pair<UnitClass, UnitSubtype>?>(null) }
    var proxyPosition by remember { mutableStateOf<Offset?>(null) }
    var proxyRotation by remember { mutableStateOf(0f) }

    val deployableRoster = listOf(
        Pair(UnitClass.COMMANDER, UnitSubtype.LEVEL_5),
        Pair(UnitClass.COMMANDER, UnitSubtype.LEVEL_4),
        Pair(UnitClass.COMMANDER, UnitSubtype.LEVEL_3),
        Pair(UnitClass.COMMANDER, UnitSubtype.LEVEL_2),
        Pair(UnitClass.COMMANDER, UnitSubtype.LEVEL_1),
        Pair(UnitClass.INFANTRY, UnitSubtype.LIGHT),
        Pair(UnitClass.INFANTRY, UnitSubtype.LINE),
        Pair(UnitClass.INFANTRY, UnitSubtype.GRENADIER),
        Pair(UnitClass.CAVALRY, UnitSubtype.LIGHT),
        Pair(UnitClass.CAVALRY, UnitSubtype.HEAVY),
        Pair(UnitClass.ARTILLERY, UnitSubtype.NONE)
    )

    var deployingForPlayer1 by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. The Game Map
        SandboxMapCanvas(
            units = viewModel.units,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Settings Overlay (Removed Text Heading)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        }

        // 3. Deployment Panel & Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showDeploymentPanel) {
                // Compact Deployment Roster
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, BrightYellow.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
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
                            // Close Button inside panel
                            IconButton(onClick = { showDeploymentPanel = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        val activeFaction = if (deployingForPlayer1) viewModel.player1Faction else viewModel.player2Faction
                        val context = LocalContext.current
                        val flagBitmap = ImageBitmap.imageResource(context.resources, activeFaction.flagResId)

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 8.dp, start = 8.dp, end = 8.dp)
                        ) {
                            items(deployableRoster) { (unitClass, subtype) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            pendingDeployment = Pair(unitClass, subtype)
                                            showDeploymentPanel = false
                                        }
                                        .padding(4.dp)
                                ) {
                                    Canvas(modifier = Modifier.size(50.dp)) {
                                        withTransform({
                                            translate(left = size.width / 2, top = size.height / 2)
                                        }) {
                                            // No name is passed here, so it renders without text in the UI
                                            drawTacticalSprite(unitClass, subtype, activeFaction.color, flagBitmap, null)
                                        }
                                    }

                                    val displayName = when (subtype) {
                                        UnitSubtype.LEVEL_1 -> "Lv 1"
                                        UnitSubtype.LEVEL_2 -> "Lv 2"
                                        UnitSubtype.LEVEL_3 -> "Lv 3"
                                        UnitSubtype.LEVEL_4 -> "Lv 4"
                                        UnitSubtype.LEVEL_5 -> "Lv 5"
                                        UnitSubtype.NONE -> unitClass.name
                                        else -> subtype.name
                                    }

                                    Text(
                                        text = displayName,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Show Button ONLY when Roster is closed
                IconButton(
                    onClick = { showDeploymentPanel = true },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp))
                        .border(1.dp, BrightYellow, RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Open Deployment",
                        tint = BrightYellow
                    )
                }
            }
        }

        // 4. Select and Place Overlay (Tap to place, Drag to rotate)
        if (pendingDeployment != null) {
            val activeFaction = if (deployingForPlayer1) viewModel.player1Faction else viewModel.player2Faction

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            proxyPosition = down.position
                            proxyRotation = 0f

                            var dragEnded = false
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()

                                if (change != null && change.pressed) {
                                    // Calculate rotation based on drag vector
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y

                                    // Small deadzone so normal taps don't cause wild spinning
                                    if (dx * dx + dy * dy > 50) {
                                        proxyRotation = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    }
                                    change.consume()
                                } else if (change != null && !change.pressed) {
                                    dragEnded = true
                                    change.consume()
                                }
                            } while (!dragEnded)

                            // Commit the unit to the ViewModel on finger lift
                            viewModel.deployUnit(
                                faction = activeFaction,
                                unitClass = pendingDeployment!!.first,
                                subtype = pendingDeployment!!.second,
                                x = proxyPosition!!.x,
                                y = proxyPosition!!.y,
                                rotation = proxyRotation
                            )

                            // Clean up state
                            pendingDeployment = null
                            proxyPosition = null
                        }
                    }
            ) {
                // Cancel Button and Instructions
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap to place, Drag to rotate", color = Color.White, modifier = Modifier.padding(end = 16.dp))
                    Button(
                        onClick = { pendingDeployment = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }

                // Draw proxy on screen while dragging
                if (proxyPosition != null) {
                    val context = LocalContext.current
                    val flagBitmap = ImageBitmap.imageResource(context.resources, activeFaction.flagResId)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        withTransform({
                            translate(proxyPosition!!.x, proxyPosition!!.y)
                            rotate(proxyRotation)
                        }) {
                            drawTacticalSprite(
                                unitClass = pendingDeployment!!.first,
                                subtype = pendingDeployment!!.second,
                                factionColor = activeFaction.color,
                                flagBitmap = flagBitmap,
                                commanderName = null // Keep proxy unnamed until it hits the ViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog... (Keep your existing Dialog code here)
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