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
import com.faiqbaig.eaw.core.getBaseStatsForUnit

enum class GamePhase { SETUP, DEPLOYMENT, BATTLE, POST_BATTLE }

data class GameConfig(
    val unitsPerSide: Int = 10,
    val commandersPerSide: Int = 2,
    val fundsPerPlayer: Int = 3000, // New budget variable
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
    fun startSetup() {
        units.clear()
        currentPhase = GamePhase.SETUP
    }

    fun startDeployment() {
        units.clear()
        activeDeploymentPlayer = 1
        currentPhase = GamePhase.DEPLOYMENT
    }

    fun finishP1Deployment() {
        activeDeploymentPlayer = 2
    }

    fun startBattle() {
        // Updated to pull maxHp from baseStats
        p1InitialHp = units.filter { it.faction == player1Faction }.sumOf { it.baseStats.maxHp }
        p2InitialHp = units.filter { it.faction == player2Faction }.sumOf { it.baseStats.maxHp }
        currentPhase = GamePhase.BATTLE
    }

    fun endBattle(verdict: String) {
        matchVerdict = verdict
        currentPhase = GamePhase.POST_BATTLE
    }

    // Deployment & Budget Logic
    fun getDeployedUnitCount(faction: Faction): Int =
        units.count { it.faction == faction && it.unitClass != UnitClass.COMMANDER }

    fun getDeployedCommanderCount(faction: Faction): Int =
        units.count { it.faction == faction && it.unitClass == UnitClass.COMMANDER }

    fun getPlayer1RemainingFunds(): Int {
        val spent = units.filter { it.faction == player1Faction }.sumOf { it.baseStats.cost }
        return config.fundsPerPlayer - spent
    }

    fun getPlayer2RemainingFunds(): Int {
        val spent = units.filter { it.faction == player2Faction }.sumOf { it.baseStats.cost }
        return config.fundsPerPlayer - spent
    }

    fun deployUnit(faction: Faction, unitClass: UnitClass, subtype: UnitSubtype, x: Float, y: Float, rotation: Float) {
        if (currentPhase != GamePhase.DEPLOYMENT) return

        // 1. Enforce Numeric Limits
        if (unitClass == UnitClass.COMMANDER) {
            if (getDeployedCommanderCount(faction) >= config.commandersPerSide) return
        } else {
            if (getDeployedUnitCount(faction) >= config.unitsPerSide) return
        }

        // 2. Enforce Budget Limits
        val stats = getBaseStatsForUnit(unitClass, subtype)
        val currentFunds = if (faction == player1Faction) getPlayer1RemainingFunds() else getPlayer2RemainingFunds()

        if (currentFunds < stats.cost) return // Block deployment if insufficient funds

        // 3. Create Unit
        val usedNames = units.mapNotNull { it.commanderName }.toSet()
        val newUnit = UnitFactory.createDeployedUnit(faction, unitClass, subtype, x, y, rotation, usedNames)

        // 4. Apply Modifiers (Updated to use new state variables)
        if (config.infiniteAmmo && newUnit.currentAmmo != null) {
            newUnit.currentAmmo = 9999
        }
        if (config.infiniteMorale && newUnit.currentMorale != null) {
            newUnit.currentMorale = 9999
        }

        units.add(newUnit)
    }

    // Helper methods for stats (Updated to use currentHp)
    fun getP1CurrentHp() = units.filter { it.faction == player1Faction }.sumOf { it.currentHp }
    fun getP2CurrentHp() = units.filter { it.faction == player2Faction }.sumOf { it.currentHp }
}