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

@Composable
fun SandboxMapCanvas(units: List<GameUnit>, modifier: Modifier = Modifier) {
    val flagBitmaps = mapOf(
        Faction.FRANCE to ImageBitmap.imageResource(id = Faction.FRANCE.flagResId),
        Faction.GREAT_BRITAIN to ImageBitmap.imageResource(id = Faction.GREAT_BRITAIN.flagResId),
        Faction.RUSSIA to ImageBitmap.imageResource(id = Faction.RUSSIA.flagResId),
        Faction.AUSTRIA to ImageBitmap.imageResource(id = Faction.AUSTRIA.flagResId),
        Faction.PRUSSIA to ImageBitmap.imageResource(id = Faction.PRUSSIA.flagResId)
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = Color(0xFF556B2F))

        for (unit in units) {
            withTransform({
                translate(left = unit.x, top = unit.y)
                rotate(degrees = unit.rotation)
            }) {
                drawTacticalSprite(
                    unitClass = unit.unitClass,
                    subtype = unit.subtype,
                    factionColor = unit.faction.color,
                    flagBitmap = flagBitmaps[unit.faction]
                )
            }
        }
    }
}

// Extracted so we can call it from both the Map and the Deployment UI
fun DrawScope.drawTacticalSprite(
    unitClass: UnitClass,
    subtype: UnitSubtype,
    factionColor: Color,
    flagBitmap: ImageBitmap? = null
) {
    val rectWidth = 60f
    val rectHeight = 20f
    val topLeft = Offset(-rectWidth / 2, -rectHeight / 2)
    val size = Size(rectWidth, rectHeight)

    when (unitClass) {
        UnitClass.INFANTRY -> {
            // Base Rectangle
            drawRect(color = factionColor, size = size, topLeft = topLeft)

            // Front facing edge colored in white
            drawLine(
                color = Color.White,
                start = Offset(-rectWidth / 2, -rectHeight / 2),
                end = Offset(rectWidth / 2, -rectHeight / 2),
                strokeWidth = 4f
            )

            // Line & Grenadier Diagonals
            if (subtype == UnitSubtype.LINE || subtype == UnitSubtype.GRENADIER) {
                drawLine(Color.White, Offset(-rectWidth / 2, -rectHeight / 2), Offset(rectWidth / 2, rectHeight / 2), strokeWidth = 2f)
                drawLine(Color.White, Offset(-rectWidth / 2, rectHeight / 2), Offset(rectWidth / 2, -rectHeight / 2), strokeWidth = 2f)
            }

            // Grenadier Side Quadrants
            if (subtype == UnitSubtype.GRENADIER) {
                val leftPath = Path().apply {
                    moveTo(-rectWidth / 2, -rectHeight / 2)
                    lineTo(0f, 0f)
                    lineTo(-rectWidth / 2, rectHeight / 2)
                    close()
                }
                drawPath(leftPath, Color.White)

                val rightPath = Path().apply {
                    moveTo(rectWidth / 2, -rectHeight / 2)
                    lineTo(0f, 0f)
                    lineTo(rectWidth / 2, rectHeight / 2)
                    close()
                }
                drawPath(rightPath, Color.White)
            }

            // Black outline to contain it all
            drawRect(color = Color.Black, size = size, topLeft = topLeft, style = Stroke(1.5f))
        }

        UnitClass.CAVALRY -> {
            // Base Rectangle
            drawRect(color = factionColor, size = size, topLeft = topLeft)

            // Filled Diagonal
            val diagColor = if (subtype == UnitSubtype.HEAVY) Color.Gray else Color.White
            val fillPath = Path().apply {
                moveTo(-rectWidth / 2, rectHeight / 2) // Bottom left
                lineTo(rectWidth / 2, -rectHeight / 2)  // Top right
                lineTo(rectWidth / 2, rectHeight / 2)   // Bottom right
                close()
            }
            drawPath(fillPath, diagColor)

            drawRect(color = Color.Black, size = size, topLeft = topLeft, style = Stroke(1.5f))
        }

        UnitClass.ARTILLERY -> {
            // Top-down cannon composed of basic shapes
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
            // Grey Pole
            drawLine(Color.Gray, start = Offset(0f, -20f), end = Offset(0f, 20f), strokeWidth = 3f)

            if (flagBitmap != null) {
                val flagWidth = 36
                val flagHeight = 24
                drawImage(
                    image = flagBitmap,
                    dstOffset = IntOffset(0, -20),
                    dstSize = IntSize(flagWidth, flagHeight)
                )
                // Grey Outline
                drawRect(
                    color = Color.Gray,
                    topLeft = Offset(0f, -20f),
                    size = Size(flagWidth.toFloat(), flagHeight.toFloat()),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}