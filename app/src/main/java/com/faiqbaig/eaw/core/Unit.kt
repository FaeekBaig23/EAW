package com.faiqbaig.eaw.core

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.faiqbaig.eaw.R
import java.util.UUID

enum class Faction(val color: Color, @DrawableRes val flagResId: Int) {
    FRANCE(Color(0xFF0D47A1), R.drawable.france),
    GREAT_BRITAIN(Color(0xFFE53935), R.drawable.britain),
    RUSSIA(Color(0xFF43A047), R.drawable.russia),
    AUSTRIA(Color(0xFFFFD700), R.drawable.austria),
    PRUSSIA(Color(0xFF212121), R.drawable.prussia)
}

enum class UnitClass { INFANTRY, CAVALRY, ARTILLERY, COMMANDER }

enum class UnitSubtype { LIGHT, LINE, GRENADIER, HEAVY, LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, LEVEL_5, NONE }

data class GameUnit(
    val id: String = UUID.randomUUID().toString(),
    val faction: Faction,
    val unitClass: UnitClass,
    val subtype: UnitSubtype,
    var x: Float,
    var y: Float,
    var rotation: Float = 0f,
    val commanderName: String? = null
) {
    // 1. Fetch the locked v1 stats for this specific unit type
    val baseStats: BaseUnitStats = getBaseStatsForUnit(unitClass, subtype)

    // 2. Mutable state variables initialized from base stats
    var currentHp: Int = baseStats.maxHp
    var currentMorale: Int? = baseStats.maxMorale
    var currentAmmo: Int? = baseStats.maxAmmo

    // 3. Combat tracking timers and state
    var reloadTimer: Float = 0f
    var meleeTickTimer: Float = 0f
    var isFirstChargeTick: Boolean = true
    var isMoving: Boolean = false
}