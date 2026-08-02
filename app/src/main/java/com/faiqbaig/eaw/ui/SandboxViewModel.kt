package com.faiqbaig.eaw.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.faiqbaig.eaw.core.Faction
import com.faiqbaig.eaw.core.GameUnit

class SandboxViewModel : ViewModel() {

    // Track the selected factions for both sides
    var player1Faction by mutableStateOf(Faction.FRANCE)
        private set
    var player2Faction by mutableStateOf(Faction.GREAT_BRITAIN)
        private set

    // Keep the empty unit list ready for when we build the deployment logic
    var units by mutableStateOf<List<GameUnit>>(emptyList())
        private set

    // Renamed to 'update' to avoid JVM signature clash with the auto-generated setters
    fun updatePlayer1Faction(faction: Faction) {
        player1Faction = faction
    }

    fun updatePlayer2Faction(faction: Faction) {
        player2Faction = faction
    }

    // Function to clear units if factions are changed
    fun clearUnits() {
        units = emptyList()
    }
}