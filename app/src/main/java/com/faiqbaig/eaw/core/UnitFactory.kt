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

        // Only generate a name if the unit being created is a Commander
        val cName: String? = if (unitClass == UnitClass.COMMANDER) {
            CommanderNames.getRandomName(faction, existingNames)
        } else {
            null
        }

        // Return the unit, which will automatically fetch its baseStats,
        // currentHp, currentAmmo, etc., internally based on class and subtype.
        return GameUnit(
            faction = faction,
            unitClass = unitClass,
            subtype = subtype,
            x = spawnX,
            y = spawnY,
            rotation = rotation,
            commanderName = cName
        )
    }
}