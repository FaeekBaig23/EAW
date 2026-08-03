package com.faiqbaig.eaw.core

object UnitFactory {

    fun createDeployedUnit(
        faction: Faction,
        unitClass: UnitClass,
        subtype: UnitSubtype,
        spawnX: Float,
        spawnY: Float,
        rotation: Float,
        existingNames: Set<String> = emptySet()
    ): GameUnit {
        var maxHp = 100
        var speed = 10f
        var range = 50f
        var power = 10
        var ammo = 0
        var cName: String? = null

        when (unitClass) {
            UnitClass.INFANTRY -> {
                range = 150f
                ammo = 20
                when (subtype) {
                    UnitSubtype.LIGHT -> { maxHp = 80; speed = 15f; power = 8 }
                    UnitSubtype.LINE -> { maxHp = 100; speed = 10f; power = 12 }
                    UnitSubtype.GRENADIER -> { maxHp = 120; speed = 8f; power = 18; ammo = 15 }
                    else -> {}
                }
            }
            UnitClass.CAVALRY -> {
                range = 25f
                when (subtype) {
                    UnitSubtype.LIGHT -> { maxHp = 90; speed = 25f; power = 15 }
                    UnitSubtype.HEAVY -> { maxHp = 150; speed = 18f; power = 25 }
                    else -> {}
                }
            }
            UnitClass.ARTILLERY -> {
                maxHp = 60
                speed = 5f
                range = 400f
                power = 40
                ammo = 30
            }
            UnitClass.COMMANDER -> {
                // Call the new file here
                cName = CommanderNames.getRandomName(faction, existingNames)

                speed = 12f
                range = 0f
                ammo = 0
                val levelMultiplier = when (subtype) {
                    UnitSubtype.LEVEL_1 -> 1
                    UnitSubtype.LEVEL_2 -> 2
                    UnitSubtype.LEVEL_3 -> 3
                    UnitSubtype.LEVEL_4 -> 4
                    UnitSubtype.LEVEL_5 -> 5
                    else -> 1
                }
                maxHp = 100 + (20 * levelMultiplier)
                power = 5 * levelMultiplier
            }
        }

        return GameUnit(
            faction = faction,
            unitClass = unitClass,
            subtype = subtype,
            hp = maxHp,
            maxHp = maxHp,
            morale = 100f,
            ammo = ammo,
            speed = speed,
            attackRange = range,
            attackPower = power,
            x = spawnX,
            y = spawnY,
            rotation = rotation,
            commanderName = cName
        )
    }
}