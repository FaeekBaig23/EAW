package com.faiqbaig.eaw.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.faiqbaig.eaw.core.Faction
import com.faiqbaig.eaw.core.GameUnit
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitSubtype
import com.faiqbaig.eaw.core.UnitState
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SandboxMapCanvas(
    units: List<GameUnit>,
    selectedUnitId: String?,
    frame: Int = 0,
    modifier: Modifier = Modifier
) {
    val flagBitmaps = mapOf(
        Faction.FRANCE to ImageBitmap.imageResource(id = Faction.FRANCE.flagResId),
        Faction.GREAT_BRITAIN to ImageBitmap.imageResource(id = Faction.GREAT_BRITAIN.flagResId),
        Faction.RUSSIA to ImageBitmap.imageResource(id = Faction.RUSSIA.flagResId),
        Faction.AUSTRIA to ImageBitmap.imageResource(id = Faction.AUSTRIA.flagResId),
        Faction.PRUSSIA to ImageBitmap.imageResource(id = Faction.PRUSSIA.flagResId)
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = Color(0xFF556B2F)) // Base terrain color

        for (unit in units) {
            // 1. ROTATED SPRITE LAYER (Unit sprite, faction ring, attack flash)
            withTransform({
                translate(left = unit.x, top = unit.y)

                if (unit.unitClass != UnitClass.COMMANDER) {
                    rotate(degrees = unit.rotation, pivot = Offset.Zero)
                }
            }) {
                // Selection ring in faction color
                if (unit.id == selectedUnitId) {
                    drawCircle(
                        color = unit.faction.color.copy(alpha = 0.5f),
                        radius = 50f,
                        center = Offset.Zero
                    )
                }

                // Brief white flash during attack actions
                val isAttacking = unit.state == UnitState.FIRING || unit.state == UnitState.IN_MELEE
                if (isAttacking) {
                    val flashAlpha = if (frame % 30 < 15) 0.4f else 0.1f
                    drawCircle(
                        color = Color.White.copy(alpha = flashAlpha),
                        radius = 45f,
                        center = Offset.Zero
                    )
                }

                drawTacticalSprite(
                    unitClass = unit.unitClass,
                    subtype = unit.subtype,
                    factionColor = unit.faction.color,
                    flagBitmap = flagBitmaps[unit.faction],
                    commanderName = unit.commanderName
                )
            }

            // 2. UNROTATED HUD LAYER (HP & Morale status bars)
            if (unit.id == selectedUnitId) {
                withTransform({
                    translate(left = unit.x, top = unit.y)
                }) {
                    val barWidth = 60f
                    val barHeight = 6f
                    val yOffset = -60f // Offset above the unit sprite

                    // Fetch max HP from baseStats
                    val maxUnitHp = unit.baseStats.maxHp.toFloat()
                    val hpRatio = (unit.currentHp / maxUnitHp).coerceIn(0f, 1f)

                    // HP Bar (Top - Green)
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(-barWidth / 2, yOffset),
                        size = Size(barWidth, barHeight)
                    )
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(-barWidth / 2, yOffset),
                        size = Size(barWidth * hpRatio, barHeight)
                    )

                    // Only draw the Morale bar if the unit actually utilizes morale
                    if (unit.baseStats.maxMorale != null) {
                        // Safely unwrap nullable morale values, defaulting to 1f to avoid division by zero
                        val maxUnitMorale = unit.baseStats.maxMorale.toFloat()
                        val safeCurrentMorale = unit.currentMorale ?: 0f
                        val moraleRatio = (safeCurrentMorale / maxUnitMorale).coerceIn(0f, 1f)

                        // Morale Bar (Bottom - Blue)
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(-barWidth / 2, yOffset + barHeight + 2f),
                            size = Size(barWidth, barHeight)
                        )
                        drawRect(
                            color = Color.Blue,
                            topLeft = Offset(-barWidth / 2, yOffset + barHeight + 2f),
                            size = Size(barWidth * moraleRatio, barHeight)
                        )
                    }
                }
            }
        }
    }
}

// Extracted so we can call it from both the Map and the Deployment UI
fun DrawScope.drawTacticalSprite(
    unitClass: UnitClass,
    subtype: UnitSubtype,
    factionColor: Color,
    flagBitmap: ImageBitmap? = null,
    commanderName: String? = null // New parameter
) {
    val rectWidth = 60f
    val rectHeight = 20f
    val edgeThickness = 4f // The thickened edge we set earlier

    val baseTopLeft = Offset(-rectWidth / 2, -rectHeight / 2)
    val baseSize = Size(rectWidth, rectHeight)
    val totalTopLeft = Offset(-rectWidth / 2, -rectHeight / 2 - edgeThickness)
    val totalSize = Size(rectWidth, rectHeight + edgeThickness)

    when (unitClass) {
        UnitClass.INFANTRY -> {
            // Base Rectangle (Faction Color)
            drawRect(color = factionColor, size = baseSize, topLeft = baseTopLeft)

            // Line Diagonals (Only drawn for LINE infantry, omitted for GRENADIER and LIGHT)
            if (subtype == UnitSubtype.LINE) {
                drawLine(Color.White, Offset(-rectWidth / 2, -rectHeight / 2), Offset(rectWidth / 2, rectHeight / 2), strokeWidth = edgeThickness)
                drawLine(Color.White, Offset(-rectWidth / 2, rectHeight / 2), Offset(rectWidth / 2, -rectHeight / 2), strokeWidth = edgeThickness)
            }

            // Grenadier Front-Back Quadrants (Rendered alone without intersecting lines)
            if (subtype == UnitSubtype.GRENADIER) {
                // Front quadrant
                val frontPath = Path().apply {
                    moveTo(-rectWidth / 2, -rectHeight / 2)
                    lineTo(rectWidth / 2, -rectHeight / 2)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(frontPath, Color.White)

                // Back quadrant
                val backPath = Path().apply {
                    moveTo(-rectWidth / 2, rectHeight / 2)
                    lineTo(rectWidth / 2, rectHeight / 2)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(backPath, Color.White)
            }

            // Front facing edge colored in white (Attached to the OUTSIDE front)
            drawRect(
                color = Color.White,
                topLeft = totalTopLeft,
                size = Size(rectWidth, edgeThickness)
            )

            // Black outline spanning the combined shape (base + front edge)
            drawRect(color = Color.Black, size = totalSize, topLeft = totalTopLeft, style = Stroke(1.5f))

            // A crisp separator line between the white edge and the colored base
            drawLine(
                color = Color.Black,
                start = Offset(-rectWidth / 2, -rectHeight / 2),
                end = Offset(rectWidth / 2, -rectHeight / 2),
                strokeWidth = 1f
            )
        }

        UnitClass.CAVALRY -> {
            // Base Rectangle
            drawRect(color = factionColor, size = baseSize, topLeft = baseTopLeft)

            // Filled Diagonal (Upper-left to lower-right)
            val diagColor = if (subtype == UnitSubtype.HEAVY) Color.Gray else Color.White
            val fillPath = Path().apply {
                moveTo(-rectWidth / 2, -rectHeight / 2) // Upper left
                lineTo(rectWidth / 2, rectHeight / 2)   // Lower right
                lineTo(-rectWidth / 2, rectHeight / 2)  // Lower left
                close()
            }
            drawPath(fillPath, diagColor)

            // Front facing edge colored in white (Attached to the OUTSIDE front)
            drawRect(
                color = Color.White,
                topLeft = totalTopLeft,
                size = Size(rectWidth, edgeThickness)
            )

            // Black outline spanning the combined shape
            drawRect(color = Color.Black, size = totalSize, topLeft = totalTopLeft, style = Stroke(1.5f))

            // A crisp separator line
            drawLine(
                color = Color.Black,
                start = Offset(-rectWidth / 2, -rectHeight / 2),
                end = Offset(rectWidth / 2, -rectHeight / 2),
                strokeWidth = 1f
            )
        }

        UnitClass.ARTILLERY -> {
            // Wheels
            drawRect(Color.Black, topLeft = Offset(-16f, -12f), size = Size(8f, 24f))
            drawRect(Color.Black, topLeft = Offset(8f, -12f), size = Size(8f, 24f))

            // Axle
            drawLine(Color.DarkGray, start = Offset(-8f, 0f), end = Offset(8f, 0f), strokeWidth = 5f)

            // Barrel (Base + Outline)
            drawRect(factionColor, topLeft = Offset(-5f, -28f), size = Size(10f, 34f))
            drawRect(Color.Black, topLeft = Offset(-5f, -28f), size = Size(10f, 34f), style = Stroke(1.5f))
        }

        UnitClass.COMMANDER -> {
            if (flagBitmap != null) {
                val flagWidth = 72
                val flagHeight = 48
                val flagTopLeftX = -flagWidth / 2
                val flagTopLeftY = -40

                // The bottom of the flag is at y = 8f.
                // The old pole went to 24f (length of 16).
                // Extending it by 50% gives a length of 24. (8f + 24 = 32f)
                val flagBottomY = (flagTopLeftY + flagHeight).toFloat()
                val poleBaseY = 32f

                // Draw extended pole (Silver)
                drawLine(
                    color = Color(0xFFC0C0C0),
                    start = Offset(0f, flagBottomY),
                    end = Offset(0f, poleBaseY),
                    strokeWidth = 4f
                )

                drawImage(
                    image = flagBitmap,
                    dstOffset = IntOffset(flagTopLeftX, flagTopLeftY),
                    dstSize = IntSize(flagWidth, flagHeight)
                )

                // Uniform Silver Outline
                drawRect(
                    color = Color(0xFFC0C0C0),
                    topLeft = Offset(flagTopLeftX.toFloat(), flagTopLeftY.toFloat()),
                    size = Size(flagWidth.toFloat(), flagHeight.toFloat()),
                    style = Stroke(width = 2.5f)
                )

                // Resolve Level to Roman Numeral
                val numeral = when (subtype) {
                    UnitSubtype.LEVEL_1 -> "I"
                    UnitSubtype.LEVEL_2 -> "II"
                    UnitSubtype.LEVEL_3 -> "III"
                    UnitSubtype.LEVEL_4 -> "IV"
                    UnitSubtype.LEVEL_5 -> "V"
                    else -> ""
                }

                drawContext.canvas.nativeCanvas.apply {
                    // Commander Name Text
                    if (commanderName != null) {
                        val namePaint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 22f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        drawText(commanderName, 0f, flagTopLeftY - 12f, namePaint)
                    }

                    // Level Numeral Text at the pole base
                    if (numeral.isNotEmpty()) {
                        val numeralPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#FFD700") // Yellow
                            textSize = 18f
                            textAlign = Paint.Align.LEFT
                            typeface = Typeface.DEFAULT_BOLD
                            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                        // x = 6f pushes it slightly right of the 4f thick pole, y = poleBaseY anchors it to the bottom
                        drawText(numeral, 6f, poleBaseY, numeralPaint)
                    }
                }
            }
        }
    }
}