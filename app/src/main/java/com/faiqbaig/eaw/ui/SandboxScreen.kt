package com.faiqbaig.eaw.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.faiqbaig.eaw.core.Faction
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitSubtype
import com.faiqbaig.eaw.core.getBaseStatsForUnit
import kotlin.math.atan2

@Composable
fun SandboxScreen(
    viewModel: SandboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onExitToMenu: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // FOG OF WAR LOGIC: Hide enemy units during deployment
        val visibleUnits = when (viewModel.currentPhase) {
            GamePhase.DEPLOYMENT -> {
                if (viewModel.activeDeploymentPlayer == 1) {
                    viewModel.units.filter { it.faction == viewModel.player1Faction }
                } else {
                    viewModel.units.filter { it.faction == viewModel.player2Faction }
                }
            }
            else -> viewModel.units // Show all units during Battle and Post-Battle
        }

        // 1. Map Layer
        SandboxMapCanvas(
            units = visibleUnits,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Overlay Layer
        when (viewModel.currentPhase) {
            GamePhase.SETUP -> {
                SetupPhaseOverlay(viewModel, onExitToMenu)
            }
            GamePhase.DEPLOYMENT -> {
                DeploymentPhaseOverlay(viewModel)
            }
            GamePhase.BATTLE -> {
                BattlePhaseOverlay(viewModel)
            }
            GamePhase.POST_BATTLE -> {
                PostBattleOverlay(viewModel, onExitToMenu)
            }
        }
    }
}

@Composable
fun SetupPhaseOverlay(viewModel: SandboxViewModel, onExit: () -> Unit) {
    var unitsText by remember { mutableStateOf(viewModel.config.unitsPerSide.toString()) }
    var fundsText by remember { mutableStateOf(viewModel.config.fundsPerPlayer.toString()) }
    var commandersPerSide by remember { mutableFloatStateOf(viewModel.config.commandersPerSide.toFloat().coerceIn(0f, 4f)) }
    var infAmmo by remember { mutableStateOf(viewModel.config.infiniteAmmo) }
    var infMorale by remember { mutableStateOf(viewModel.config.infiniteMorale) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { /* Cannot dismiss, must start or exit */ }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E221E),
            border = BorderStroke(1.5.dp, BrightYellow)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SANDBOX SETUP", color = BrightYellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))

                FactionSelector(
                    label = "Player 1 Faction",
                    selectedFaction = viewModel.player1Faction,
                    opponentFaction = viewModel.player2Faction,
                    onFactionSelected = { viewModel.updatePlayer1Faction(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                FactionSelector(
                    label = "Player 2 Faction",
                    selectedFaction = viewModel.player2Faction,
                    opponentFaction = viewModel.player1Faction,
                    onFactionSelected = { viewModel.updatePlayer2Faction(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Unit Cap Input
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        if (filtered.isEmpty()) {
                            unitsText = ""
                        } else {
                            val num = filtered.toIntOrNull() ?: 0
                            unitsText = if (num > 30) "30" else filtered
                        }
                    },
                    label = { Text("Units per side (Max 30)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightYellow,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = BrightYellow,
                        unfocusedLabelColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Funds Budget Input
                OutlinedTextField(
                    value = fundsText,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        fundsText = filtered
                    },
                    label = { Text("Funds per player") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrightYellow,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = BrightYellow,
                        unfocusedLabelColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Commanders per side: ${commandersPerSide.toInt()}", color = Color.White)
                Slider(
                    value = commandersPerSide,
                    onValueChange = { commandersPerSide = it },
                    valueRange = 0f..4f,
                    steps = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Switch(checked = infAmmo, onCheckedChange = { infAmmo = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Infinite Ammo", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Switch(checked = infMorale, onCheckedChange = { infMorale = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Infinite Morale", color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(onClick = onExit, border = BorderStroke(1.dp, Color.Red)) {
                        Text("Back", color = Color.Red)
                    }
                    Button(
                        onClick = {
                            val finalUnits = unitsText.toIntOrNull() ?: 10
                            val finalFunds = fundsText.toIntOrNull() ?: 3000

                            viewModel.updateConfig(
                                GameConfig(
                                    unitsPerSide = finalUnits,
                                    commandersPerSide = commandersPerSide.toInt(),
                                    fundsPerPlayer = finalFunds,
                                    infiniteAmmo = infAmmo,
                                    infiniteMorale = infMorale
                                )
                            )
                            viewModel.startDeployment()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightYellow, contentColor = Color.Black)
                    ) {
                        Text("Begin Deployment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeploymentPhaseOverlay(viewModel: SandboxViewModel) {
    var showDeploymentPanel by remember { mutableStateOf(false) }
    var pendingDeployment by remember { mutableStateOf<Pair<UnitClass, UnitSubtype>?>(null) }
    var proxyPosition by remember { mutableStateOf<Offset?>(null) }
    var proxyRotation by remember { mutableStateOf(0f) }

    val isP1 = viewModel.activeDeploymentPlayer == 1
    val activeFaction = if (isP1) viewModel.player1Faction else viewModel.player2Faction
    val remainingFunds = if (isP1) viewModel.getPlayer1RemainingFunds() else viewModel.getPlayer2RemainingFunds()

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

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Draw Deployment Zones
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thirdWidth = size.width / 3f
            val twoThirdsWidth = (size.width * 2) / 3f

            drawRect(
                color = Color.Red.copy(alpha = 0.15f),
                topLeft = Offset(thirdWidth, 0f),
                size = Size(thirdWidth, size.height)
            )

            if (isP1) {
                drawRect(color = Color.Green.copy(alpha = 0.1f), size = Size(thirdWidth, size.height))
                drawLine(color = Color.Green, start = Offset(thirdWidth, 0f), end = Offset(thirdWidth, size.height), strokeWidth = 3f)
            } else {
                drawRect(color = Color.Green.copy(alpha = 0.1f), topLeft = Offset(twoThirdsWidth, 0f), size = Size(thirdWidth, size.height))
                drawLine(color = Color.Green, start = Offset(twoThirdsWidth, 0f), end = Offset(twoThirdsWidth, size.height), strokeWidth = 3f)
            }
        }

        // Top Status & Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "P${viewModel.activeDeploymentPlayer} DEPLOYMENT",
                color = BrightYellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            )

            if (isP1) {
                Button(
                    onClick = {
                        viewModel.finishP1Deployment()
                        pendingDeployment = null
                        showDeploymentPanel = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightYellow, contentColor = Color.Black)
                ) {
                    Text("End P1 Turn", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.startBattle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("Start Battle", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Roster Selection Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showDeploymentPanel) {
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
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Deploying: ${activeFaction.name.replace("_", " ")}",
                                    color = BrightYellow,
                                    fontWeight = FontWeight.Bold
                                )
                                val uCount = viewModel.getDeployedUnitCount(activeFaction)
                                val cCount = viewModel.getDeployedCommanderCount(activeFaction)
                                Text(
                                    text = "Funds: $remainingFunds | Units: $uCount/${viewModel.config.unitsPerSide} | Cmdrs: $cCount/${viewModel.config.commandersPerSide}",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showDeploymentPanel = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        val context = LocalContext.current
                        val flagBitmap = remember(activeFaction.flagResId) {
                            ImageBitmap.imageResource(context.resources, activeFaction.flagResId)
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 8.dp, start = 8.dp, end = 8.dp)
                        ) {
                            items(deployableRoster) { (unitClass, subtype) ->
                                val stats = getBaseStatsForUnit(unitClass, subtype)
                                val canAfford = remainingFunds >= stats.cost

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .alpha(if (canAfford) 1f else 0.4f)
                                        .clickable(enabled = canAfford) {
                                            pendingDeployment = Pair(unitClass, subtype)
                                            showDeploymentPanel = false
                                        }
                                        .padding(4.dp)
                                ) {
                                    Canvas(modifier = Modifier.size(50.dp)) {
                                        withTransform({
                                            translate(left = size.width / 2, top = size.height / 2)
                                        }) {
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

                                    Text(text = displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "🪙 ${stats.cost}",
                                        color = if (canAfford) BrightYellow else Color.Red,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = { showDeploymentPanel = true },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp))
                        .border(1.dp, BrightYellow, RoundedCornerShape(4.dp))
                ) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Open", tint = BrightYellow)
                }
            }
        }

        // Unit Placement Preview & Gesture Input
        if (pendingDeployment != null) {
            val context = LocalContext.current
            val flagBitmap = remember(activeFaction.flagResId) {
                ImageBitmap.imageResource(context.resources, activeFaction.flagResId)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .pointerInput(isP1) {
                        awaitEachGesture {
                            val down = awaitFirstDown()

                            val thirdWidth = size.width / 3f
                            val allowedToPlace = if (isP1) {
                                down.position.x <= thirdWidth
                            } else {
                                down.position.x >= (size.width - thirdWidth)
                            }

                            if (allowedToPlace) {
                                proxyPosition = down.position
                                proxyRotation = 0f

                                var dragEnded = false
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull()

                                    if (change != null && change.pressed) {
                                        val dx = change.position.x - down.position.x
                                        val dy = change.position.y - down.position.y

                                        if (dx * dx + dy * dy > 50) {
                                            proxyRotation = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        }
                                        change.consume()
                                    } else if (change != null && !change.pressed) {
                                        dragEnded = true
                                        change.consume()
                                    }
                                } while (!dragEnded)

                                viewModel.deployUnit(
                                    faction = activeFaction,
                                    unitClass = pendingDeployment!!.first,
                                    subtype = pendingDeployment!!.second,
                                    x = proxyPosition!!.x,
                                    y = proxyPosition!!.y,
                                    rotation = proxyRotation
                                )

                                pendingDeployment = null
                                proxyPosition = null
                                showDeploymentPanel = true // Re-open panel automatically to speed up deployment
                            } else {
                                down.consume()
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap green zone to place, drag to rotate", color = Color.White, modifier = Modifier.padding(end = 16.dp))
                    Button(
                        onClick = {
                            pendingDeployment = null
                            showDeploymentPanel = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }

                if (proxyPosition != null) {
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
                                commanderName = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattlePhaseOverlay(viewModel: SandboxViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.endBattle("${viewModel.player2Faction.name.replace("_", " ")} Wins by Surrender!") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text("P1 Surrender", color = Color.White)
            }

            Button(
                onClick = { viewModel.endBattle("${viewModel.player1Faction.name.replace("_", " ")} Wins by Surrender!") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text("P2 Surrender", color = Color.White)
            }
        }
    }
}

@Composable
fun PostBattleOverlay(
    viewModel: SandboxViewModel,
    onExit: () -> Unit
) {
    val p1Loss = viewModel.p1InitialHp - viewModel.getP1CurrentHp()
    val p2Loss = viewModel.p2InitialHp - viewModel.getP2CurrentHp()

    val p1FactionName = viewModel.player1Faction.name.replace("_", " ")
    val p2FactionName = viewModel.player2Faction.name.replace("_", " ")
    val formattedVerdict = viewModel.matchVerdict.replace("_", " ")

    val p1Commanders = viewModel.units
        .filter { it.faction == viewModel.player1Faction && it.unitClass == UnitClass.COMMANDER }
        .map { Pair(it.subtype.toRomanNumeral(), it.commanderName ?: "Field Command") }
        .ifEmpty { listOf(Pair("I", "Field Command")) }

    val p2Commanders = viewModel.units
        .filter { it.faction == viewModel.player2Faction && it.unitClass == UnitClass.COMMANDER }
        .map { Pair(it.subtype.toRomanNumeral(), it.commanderName ?: "Field Command") }
        .ifEmpty { listOf(Pair("I", "Field Command")) }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f) // Slightly narrower to avoid covering full screen edges
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E221E),
            border = BorderStroke(1.5.dp, BrightYellow)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Result Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, start = 2.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Result",
                        fontWeight = FontWeight.Bold,
                        color = BrightYellow,
                        fontSize = 13.sp,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = formattedVerdict,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // 1. Belligerents
                InfoboxHeader("Belligerents")
                InfoboxTwoColumnRow(
                    leftContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = viewModel.player1Faction.flagResId),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp, 16.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(p1FactionName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    rightContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = viewModel.player2Faction.flagResId),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp, 16.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(p2FactionName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )

                // 2. Commanders and leaders
                InfoboxHeader("Commanders and leaders")
                InfoboxTwoColumnRow(
                    leftContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            p1Commanders.forEach { (romanLevel, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = viewModel.player1Faction.flagResId),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp, 10.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$romanLevel $name", color = Color(0xFFBDC3C7), fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    rightContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            p2Commanders.forEach { (romanLevel, name) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = viewModel.player2Faction.flagResId),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp, 10.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$romanLevel $name", color = Color(0xFFBDC3C7), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                )

                // 3. Strength
                InfoboxHeader("Strength")
                InfoboxTwoColumnRow(
                    leftContent = {
                        Text("${viewModel.p1InitialHp}", color = Color.White, fontSize = 12.sp)
                    },
                    rightContent = {
                        Text("${viewModel.p2InitialHp}", color = Color.White, fontSize = 12.sp)
                    }
                )

                // 4. Casualties and losses
                InfoboxHeader("Casualties and losses")
                InfoboxTwoColumnRow(
                    leftContent = {
                        Text("$p1Loss", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    },
                    rightContent = {
                        Text("$p2Loss", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.startSetup() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(1.dp, BrightYellow),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Reset", color = BrightYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.startDeployment() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrightYellow, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Redeploy", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Menu", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun UnitSubtype.toRomanNumeral(): String = when (this) {
    UnitSubtype.LEVEL_5 -> "V"
    UnitSubtype.LEVEL_4 -> "IV"
    UnitSubtype.LEVEL_3 -> "III"
    UnitSubtype.LEVEL_2 -> "II"
    UnitSubtype.LEVEL_1 -> "I"
    else -> "I"
}

@Composable
private fun InfoboxHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
            .background(Color(0xFF2C3E50), RoundedCornerShape(2.dp))
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun InfoboxTwoColumnRow(
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            leftContent()
        }

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        ) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            rightContent()
        }
    }
}

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
                        .clickable(enabled = !isOpponent) {
                            onFactionSelected(faction)
                        }
                )
            }
        }
    }
}