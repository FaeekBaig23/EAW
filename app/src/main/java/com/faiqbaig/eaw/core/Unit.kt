package com.faiqbaig.eaw.core

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.faiqbaig.eaw.R
import java.util.UUID

// Added the flagResId to tie the drawable directly to the faction
enum class Faction(val color: Color, @DrawableRes val flagResId: Int) {
    FRANCE(Color(0xFF1E88E5), R.drawable.france),
    GREAT_BRITAIN(Color(0xFFE53935), R.drawable.britain),
    RUSSIA(Color(0xFF43A047), R.drawable.russia),
    AUSTRIA(Color(0xFFFDD835), R.drawable.austria),
    PRUSSIA(Color(0xFF212121), R.drawable.prussia)
}

enum class UnitClass { INFANTRY, CAVALRY, ARTILLERY, COMMANDER }
enum class UnitSubtype { LIGHT, LINE, GRENADIER, HEAVY, NONE }

data class GameUnit(
    val id: String = UUID.randomUUID().toString(),
    val faction: Faction,
    val unitClass: UnitClass,
    val subtype: UnitSubtype,
    var hp: Int,
    val maxHp: Int,
    var morale: Float,
    var ammo: Int,
    var x: Float,
    var y: Float,
    var rotation: Float
)