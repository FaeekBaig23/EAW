package com.faiqbaig.eaw.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.faiqbaig.eaw.core.Faction
import com.faiqbaig.eaw.core.GameUnit
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitFactory
import com.faiqbaig.eaw.core.UnitSubtype

class SandboxViewModel : ViewModel() {
    val units = mutableStateListOf<GameUnit>()

    var player1Faction by mutableStateOf(Faction.FRANCE)
        private set
    var player2Faction by mutableStateOf(Faction.AUSTRIA)
        private set

    fun updatePlayer1Faction(faction: Faction) { player1Faction = faction }
    fun updatePlayer2Faction(faction: Faction) { player2Faction = faction }

    // New deployment function
    fun deployUnit(faction: Faction, unitClass: UnitClass, subtype: UnitSubtype, x: Float, y: Float, rotation: Float) {
        // Extract a set of every commander name currently on the field
        val usedNames = units.mapNotNull { it.commanderName }.toSet()

        val newUnit = UnitFactory.createDeployedUnit(
            faction = faction,
            unitClass = unitClass,
            subtype = subtype,
            spawnX = x,
            spawnY = y,
            rotation = rotation,
            existingNames = usedNames // <-- Pass the set to the factory
        )
        units.add(newUnit)
    }
}