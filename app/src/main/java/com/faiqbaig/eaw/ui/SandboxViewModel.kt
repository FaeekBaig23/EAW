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
import com.faiqbaig.eaw.core.getCorpCapacity
import com.faiqbaig.eaw.core.UnitState
import kotlin.math.pow
import kotlin.math.sqrt

enum class GamePhase { SETUP, DEPLOYMENT, BATTLE, POST_BATTLE }

data class GameConfig(
    val unitsPerSide: Int = 10,
    val commandersPerSide: Int = 2,
    val fundsPerPlayer: Int = 3000,
    val infiniteAmmo: Boolean = false,
    val infiniteMorale: Boolean = false
)

class SandboxViewModel : ViewModel() {
    val units = mutableStateListOf<GameUnit>()

    // Core States
    var currentPhase by mutableStateOf(GamePhase.SETUP)
    var battleFrame by mutableIntStateOf(0)
    var selectedBattleUnitId by mutableStateOf<String?>(null)
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

    // --- NEW: Corp & Selection States ---
    var activeCommanderId by mutableStateOf<String?>(null)
        private set

    var selectedUnitId by mutableStateOf<String?>(null)
        private set
    var selectedCorpId by mutableStateOf<String?>(null)
        private set

    // Pending placement state
    var pendingPlacementUnit by mutableStateOf<GameUnit?>(null)
        private set

    fun updateConfig(newConfig: GameConfig) { config = newConfig }
    fun updatePlayer1Faction(faction: Faction) { player1Faction = faction }
    fun updatePlayer2Faction(faction: Faction) { player2Faction = faction }

    fun startSetup() {
        units.clear()
        currentPhase = GamePhase.SETUP
    }

    fun startDeployment() {
        units.clear()
        activeDeploymentPlayer = 1
        resetSelectionStates()
        currentPhase = GamePhase.DEPLOYMENT
    }

    fun finishP1Deployment() {
        activeDeploymentPlayer = 2
        resetSelectionStates()
    }

    fun startBattle() {
        p1InitialHp = units.filter { it.faction == player1Faction }.sumOf { it.baseStats.maxHp }
        p2InitialHp = units.filter { it.faction == player2Faction }.sumOf { it.baseStats.maxHp }
        resetSelectionStates()
        currentPhase = GamePhase.BATTLE
    }

    fun endBattle(verdict: String) {
        matchVerdict = verdict
        currentPhase = GamePhase.POST_BATTLE
    }

    private fun resetSelectionStates() {
        activeCommanderId = null
        selectedUnitId = null
        selectedCorpId = null
        pendingPlacementUnit = null
    }

    // --- Selection Logic ---
    fun selectUnit(unit: GameUnit?) {
        selectedUnitId = unit?.id
        selectedCorpId = if (unit?.unitClass == UnitClass.COMMANDER) unit.id else null
    }

    fun setActiveCommander(commanderId: String?) {
        activeCommanderId = commanderId
    }

    // --- Budget Logic ---
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

    // --- Staged Deployment Logic ---
    fun stageUnitPlacement(faction: Faction, unitClass: UnitClass, subtype: UnitSubtype, x: Float, y: Float): Boolean {
        // 1. Prevent Overlap
        val minOverlapSquared = 3600f
        val hasOverlap = units.any {
            val dx = it.x - x
            val dy = it.y - y
            (dx * dx + dy * dy) < minOverlapSquared
        }
        if (hasOverlap) return false

        // 2. Enforce Numeric Limits
        if (unitClass == UnitClass.COMMANDER) {
            if (getDeployedCommanderCount(faction) >= config.commandersPerSide) return false
        } else {
            if (getDeployedUnitCount(faction) >= config.unitsPerSide) return false
        }

        // 3. Enforce Corp Capacity Limit
        if (unitClass != UnitClass.COMMANDER && activeCommanderId != null) {
            val activeCmdr = units.find { it.id == activeCommanderId }
            if (activeCmdr != null) {
                val currentCorpSize = units.count { it.corpId == activeCommanderId }
                if (currentCorpSize >= activeCmdr.subtype.getCorpCapacity()) {
                    return false // Block placement if the selected Corp is full
                }
            }
        }

        // 4. Enforce Budget Limits
        val stats = getBaseStatsForUnit(unitClass, subtype)
        val currentFunds = if (faction == player1Faction) getPlayer1RemainingFunds() else getPlayer2RemainingFunds()
        if (currentFunds < stats.cost) return false

        val usedNames = units.mapNotNull { it.commanderName }.toSet()
        val newUnit = UnitFactory.createDeployedUnit(faction, unitClass, subtype, x, y, 0f, usedNames)

        if (unitClass != UnitClass.COMMANDER) {
            newUnit.corpId = activeCommanderId
        }

        pendingPlacementUnit = newUnit
        return true
    }

    fun rotatePendingUnit() {
        // Only allow rotation if the unit is NOT a commander
        pendingPlacementUnit?.let { unit ->
            if (unit.unitClass != UnitClass.COMMANDER) {
                // Use .copy() to force the mutable state to recompose the UI
                pendingPlacementUnit = unit.copy(rotation = (unit.rotation + 90f) % 360f)
            }
        }
    }

    // --- NEW: Delete Confirmed Unit ---
    fun deleteSelectedUnit() {
        val unitToRemove = units.find { it.id == selectedUnitId } ?: return
        units.remove(unitToRemove)

        // Clear active references if we deleted the currently active commander
        if (activeCommanderId == unitToRemove.id) activeCommanderId = null
        selectedUnitId = null
        selectedCorpId = null
    }

    fun cancelPendingPlacement() {
        pendingPlacementUnit = null
    }

    fun confirmPendingPlacement() {
        val unit = pendingPlacementUnit ?: return

        if (config.infiniteAmmo && unit.currentAmmo != null) unit.currentAmmo = 9999
        if (config.infiniteMorale && unit.currentMorale != null) unit.currentMorale = 9999f

        units.add(unit)

        // Auto-select newly placed commanders to speed up flow
        if (unit.unitClass == UnitClass.COMMANDER) {
            activeCommanderId = unit.id
        }

        pendingPlacementUnit = null
    }

    fun getP1CurrentHp() = units.filter { it.faction == player1Faction }.sumOf { it.currentHp }
    fun getP2CurrentHp() = units.filter { it.faction == player2Faction }.sumOf { it.currentHp }

    fun handleBattleMapTap(x: Float, y: Float) {
        val tappedUnit = units.find { unit ->
            val dx = unit.x - x
            val dy = unit.y - y
            kotlin.math.sqrt(dx * dx + dy * dy) < 150f
        }

        val currentlySelectedUnit = units.find { it.id == selectedBattleUnitId }

        if (tappedUnit != null) {
            if (tappedUnit.id == selectedBattleUnitId) {
                // --- NEW: Tapping the active unit deselects it ---
                selectedBattleUnitId = null
            } else if (currentlySelectedUnit == null || tappedUnit.faction == currentlySelectedUnit.faction) {
                // Select a new friendly unit
                selectedBattleUnitId = tappedUnit.id
            } else {
                // Chase/Attack enemy unit
                currentlySelectedUnit.targetUnitId = tappedUnit.id
                currentlySelectedUnit.state = UnitState.CHASING
            }
        } else {
            if (currentlySelectedUnit != null) {
                // Move Order
                currentlySelectedUnit.targetUnitId = null
                currentlySelectedUnit.destinationX = x
                currentlySelectedUnit.destinationY = y
                currentlySelectedUnit.state = UnitState.MOVING
            } else {
                selectedBattleUnitId = null
            }
        }
    }
}