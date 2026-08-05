package com.faiqbaig.eaw.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.faiqbaig.eaw.core.Faction
import com.faiqbaig.eaw.core.GameUnit
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitFactory
import com.faiqbaig.eaw.core.UnitSubtype

enum class GamePhase { SETUP, DEPLOYMENT, BATTLE, POST_BATTLE }

data class GameConfig(
    val unitsPerSide: Int = 10,
    val commandersPerSide: Int = 2,
    val infiniteAmmo: Boolean = false,
    val infiniteMorale: Boolean = false
)

class SandboxViewModel : ViewModel() {
    val units = mutableStateListOf<GameUnit>()

    // Core States
    var currentPhase by mutableStateOf(GamePhase.SETUP)
        private set
    var config by mutableStateOf(GameConfig())
        private set
    var activeDeploymentPlayer by mutableIntStateOf(1)
        private set

    var player1Faction by mutableStateOf(Faction.FRANCE)
        private set
    var player2Faction by mutableStateOf(Faction.AUSTRIA)
        private set

    // Post-Battle Statistics Tracking
    var p1InitialHp by mutableStateOf(0)
    var p2InitialHp by mutableStateOf(0)
    var matchVerdict by mutableStateOf("")
        private set


    // Configuration Modifiers
    fun updateConfig(newConfig: GameConfig) { config = newConfig }
    fun updatePlayer1Faction(faction: Faction) { player1Faction = faction }
    fun updatePlayer2Faction(faction: Faction) { player2Faction = faction }

    // Phase Transitions
    fun startDeployment() {
        units.clear()
        activeDeploymentPlayer = 1
        currentPhase = GamePhase.DEPLOYMENT
    }

    // New function to swap from P1 to P2
    fun finishP1Deployment() {
        activeDeploymentPlayer = 2
    }

    fun startBattle() {
        p1InitialHp = units.filter { it.faction == player1Faction }.sumOf { it.maxHp }
        p2InitialHp = units.filter { it.faction == player2Faction }.sumOf { it.maxHp }
        currentPhase = GamePhase.BATTLE
    }

    fun endBattle(verdict: String) {
        matchVerdict = verdict
        currentPhase = GamePhase.POST_BATTLE
    }

    // Deployment Logic
    fun getDeployedUnitCount(faction: Faction): Int =
        units.count { it.faction == faction && it.unitClass != UnitClass.COMMANDER }

    fun getDeployedCommanderCount(faction: Faction): Int =
        units.count { it.faction == faction && it.unitClass == UnitClass.COMMANDER }

    fun deployUnit(faction: Faction, unitClass: UnitClass, subtype: UnitSubtype, x: Float, y: Float, rotation: Float) {
        if (currentPhase != GamePhase.DEPLOYMENT) return

        // Enforce Limits
        if (unitClass == UnitClass.COMMANDER) {
            if (getDeployedCommanderCount(faction) >= config.commandersPerSide) return
        } else {
            if (getDeployedUnitCount(faction) >= config.unitsPerSide) return
        }

        val usedNames = units.mapNotNull { it.commanderName }.toSet()
        val newUnit = UnitFactory.createDeployedUnit(faction, unitClass, subtype, x, y, rotation, usedNames)

        // Apply Modifiers
        if (config.infiniteAmmo) newUnit.ammo = 9999
        if (config.infiniteMorale) newUnit.morale = 9999f

        units.add(newUnit)
    }

    // Helper methods for stats
    fun getP1CurrentHp() = units.filter { it.faction == player1Faction }.sumOf { it.hp }
    fun getP2CurrentHp() = units.filter { it.faction == player2Faction }.sumOf { it.hp }
}