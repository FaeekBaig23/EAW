package com.faiqbaig.eaw.core

/**
 * Holds the static base attributes for a unit class/subtype.
 */
data class BaseUnitStats(
    // Original properties (Order must match the factory calls)
    val maxHp: Int,
    val maxMorale: Int?,
    val maxAmmo: Int?,
    val moveSpeed: Float,
    val reloadTime: Float?,
    val volleyDamage: Int,
    val meleeDamage: Int,
    val chargeBonus: Float? = null,
    val pointBlankVolleyDamage: Int? = null,
    val cost: Int,

    // New Combat & Morale Simulation Modifiers (Assigned defaults so the factory doesn't break)
    val rotationSpeed: Float = 60f, // degrees per second
    val detectionRadius: Float = 400f,
    val effectiveRange: Float = 250f,
    val canisterRange: Float = 100f,
    val moraleDecayPerHitPct: Float = 0.05f, // 5% morale lost per hit
    val moraleDecayPerSecMoving: Float = 1.0f,
    val moraleRegenPerSecIdle: Float = 2.0f,
    val supplyCutMoraleDrainPerSec: Float = 1.0f,
    val moveSpeedMultiplier: Float = 1.0f
)

/**
 * Retrieves the locked v1 Sandbox stats for a given unit type and subtype.
 */
fun getBaseStatsForUnit(unitClass: UnitClass, subtype: UnitSubtype): BaseUnitStats {
    return when (unitClass) {
        UnitClass.INFANTRY -> when (subtype) {
            UnitSubtype.LIGHT -> BaseUnitStats(
                maxHp = 350, maxMorale = 70, maxAmmo = 40,
                moveSpeed = 42f, reloadTime = 3.5f,
                volleyDamage = 12, meleeDamage = 8, cost = 80
            )
            UnitSubtype.LINE -> BaseUnitStats(
                maxHp = 500, maxMorale = 80, maxAmmo = 40,
                moveSpeed = 34f, reloadTime = 4.5f,
                volleyDamage = 18, meleeDamage = 10, cost = 120
            )
            UnitSubtype.GRENADIER -> BaseUnitStats(
                maxHp = 650, maxMorale = 100, maxAmmo = 35,
                moveSpeed = 29f, reloadTime = 5.0f,
                volleyDamage = 16, meleeDamage = 18, cost = 180
            )
            else -> BaseUnitStats(500, 80, 40, 34f, 4.5f, 18, 10, cost = 120) // Fallback
        }

        UnitClass.CAVALRY -> when (subtype) {
            UnitSubtype.LIGHT -> BaseUnitStats(
                maxHp = 300, maxMorale = 75, maxAmmo = null,
                moveSpeed = 72f, reloadTime = null,
                volleyDamage = 0, meleeDamage = 14, chargeBonus = 0.40f, cost = 140
            )
            UnitSubtype.HEAVY -> BaseUnitStats(
                maxHp = 450, maxMorale = 90, maxAmmo = null,
                moveSpeed = 59f, reloadTime = null,
                volleyDamage = 0, meleeDamage = 22, chargeBonus = 0.70f, cost = 220
            )
            else -> BaseUnitStats(300, 75, null, 72f, null, 0, 14, 0.40f, cost = 140)
        }

        UnitClass.ARTILLERY -> {
            BaseUnitStats(
                maxHp = 200, maxMorale = 60, maxAmmo = 20,
                moveSpeed = 20f, reloadTime = 7.0f,
                volleyDamage = 40, meleeDamage = 4, pointBlankVolleyDamage = 60, cost = 260
            )
        }

        UnitClass.COMMANDER -> {
            BaseUnitStats(
                maxHp = 100, maxMorale = null, maxAmmo = null,
                moveSpeed = 37f, reloadTime = null,
                volleyDamage = 0, meleeDamage = 3, cost = 400
            )
        }
    }
}