package com.faiqbaig.eaw.combat

import com.faiqbaig.eaw.core.GameUnit
import com.faiqbaig.eaw.core.UnitState
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitSubtype
import kotlin.math.*
import kotlin.random.Random

object BattleEngine {

    var onVolleyFired: (() -> Unit)? = null

    fun updateTick(units: List<GameUnit>, rawDeltaTime: Float) {
        // Clamp deltaTime to prevent teleportation during UI recomposition frame hitches
        val deltaTime = rawDeltaTime.coerceAtMost(0.05f)

        units.forEach { unit ->
            if (unit.currentHp <= 0) return@forEach

            var tookDamageThisTick = false

            when (unit.state) {
                UnitState.IDLE -> processIdle(unit, units)
                UnitState.MOVING -> processMovement(unit, deltaTime)
                UnitState.ROTATING -> processRotation(unit, units, deltaTime)
                UnitState.FIRING -> tookDamageThisTick = processFiring(unit, units, deltaTime)
                UnitState.IN_MELEE -> tookDamageThisTick = processMelee(unit, units, deltaTime)
                UnitState.CHASING -> processChasing(unit, units, deltaTime)
                UnitState.RETURNING -> processMovement(unit, deltaTime)
                UnitState.ROUTING -> processRouting(unit, deltaTime)
                UnitState.SURRENDERING -> { /* Wait for capture logic */ }
            }

            updateMorale(unit, deltaTime, tookDamageThisTick)
        }

        resolveUnitCollisions(units)
    }

    // Call this inside BattleEngine.updateTick() after processing unit movement
    private fun resolveUnitCollisions(units: List<GameUnit>, unitRadius: Float = 30f) {
        val minDistance = unitRadius * 2f
        val minDistanceSq = minDistance * minDistance

        for (i in units.indices) {
            for (j in i + 1 until units.size) {
                val u1 = units[i]
                val u2 = units[j]

                // Ignore routing or dead units if necessary
                if (u1.currentHp <= 0 || u2.currentHp <= 0) continue

                val dx = u2.x - u1.x
                val dy = u2.y - u1.y
                val distSq = dx * dx + dy * dy

                if (distSq in 0.0001f..minDistanceSq) {
                    val dist = sqrt(distSq)
                    val overlap = (minDistance - dist) / 2f
                    val nx = dx / dist
                    val ny = dy / dist

                    // Push units apart along collision vector
                    u1.x -= nx * overlap
                    u1.y -= ny * overlap
                    u2.x += nx * overlap
                    u2.y += ny * overlap
                }
            }
        }
    }

    private fun processIdle(unit: GameUnit, allUnits: List<GameUnit>) {
        // Auto-detect enemies entering range
        val target = findNearestEnemyInRange(unit, allUnits, unit.baseStats.detectionRadius)
        if (target != null) {
            unit.targetUnitId = target.id
            unit.state = UnitState.ROTATING
        }
    }

    private fun processMovement(unit: GameUnit, deltaTime: Float) {
        val destX = unit.destinationX ?: return
        val destY = unit.destinationY ?: return

        val dx = destX - unit.x
        val dy = destY - unit.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < 5f) { // Arrived
            unit.x = destX
            unit.y = destY
            unit.state = UnitState.IDLE
            unit.clearCurrentOrders()
            return
        }

        // 1. Calculate target facing angle
        val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val targetAngle = rawAngle + 90f

        // 2. Smoothly rotate toward destination angle using rotationSpeed
        val angleDiff = normalizeAngle(targetAngle - unit.rotation)
        val step = unit.baseStats.rotationSpeed * deltaTime

        if (abs(angleDiff) > 15f) {
            // Pivot on the spot until within 15 degrees of target heading
            unit.rotation += if (angleDiff > 0) min(step, angleDiff) else max(-step, angleDiff)
            unit.isMoving = false
            return
        } else {
            // Continue fine-tuning rotation while marching
            unit.rotation += if (angleDiff > 0) min(step, angleDiff) else max(-step, angleDiff)
        }

        // 3. Move unit forward once aligned
        val speed = unit.baseStats.moveSpeed * unit.baseStats.moveSpeedMultiplier * deltaTime
        val ratio = speed / distance
        unit.x += dx * ratio
        unit.y += dy * ratio
        unit.isMoving = true
    }

    private fun processRotation(unit: GameUnit, allUnits: List<GameUnit>, deltaTime: Float) {
        val target = allUnits.find { it.id == unit.targetUnitId }
        if (target == null || target.currentHp <= 0 || target.state == UnitState.ROUTING) {
            unit.clearCurrentOrders()
            return
        }

        val dx = target.x - unit.x
        val dy = target.y - unit.y

        // --- TWEAKED: Align target angle calculation with the north sprite edge ---
        val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val targetAngle = rawAngle + 90f // Matches the movement offset adjustment

        // Simple rotation interpolation
        val angleDiff = normalizeAngle(targetAngle - unit.rotation)

        if (abs(angleDiff) <= 15f) { // Within 15-degree tolerance
            unit.rotation = targetAngle
            unit.state = if (unit.isMelee) UnitState.IN_MELEE else UnitState.FIRING
            unit.isFirstChargeTick = true
            unit.isMoving = false
        } else {
            val step = unit.baseStats.rotationSpeed * deltaTime
            unit.rotation += if (angleDiff > 0) min(step, angleDiff) else max(-step, angleDiff)
        }
    }

    private fun processFiring(attacker: GameUnit, allUnits: List<GameUnit>, deltaTime: Float): Boolean {
        val target = allUnits.find { it.id == attacker.targetUnitId }
        if (target == null || target.state == UnitState.ROUTING) {
            attacker.clearCurrentOrders()
            return false
        }

        attacker.reloadTimer -= deltaTime
        if (attacker.reloadTimer <= 0f) {
            attacker.reloadTimer = attacker.baseStats.reloadTime ?: 5.0f

            val distance = getDistance(attacker, target)
            if (distance > attacker.baseStats.effectiveRange) {
                attacker.clearCurrentOrders()
                return false
            }

            val damage = calculateDamage(attacker, target, distance, false)
            target.currentHp -= damage

            attacker.lastAttackTimestamp = System.currentTimeMillis()

            // --- PLAY AUDIO FOR INFANTRY VOLLEYS ---
            if (attacker.unitClass == UnitClass.INFANTRY) {
                onVolleyFired?.invoke()
            }

            return true
        }
        return false
    }

    private fun processMelee(attacker: GameUnit, allUnits: List<GameUnit>, deltaTime: Float): Boolean {
        val target = allUnits.find { it.id == attacker.targetUnitId }
        if (target == null || target.state == UnitState.ROUTING) {
            attacker.clearCurrentOrders()
            return false
        }

        attacker.meleeTickTimer -= deltaTime
        if (attacker.meleeTickTimer <= 0f) {
            attacker.meleeTickTimer = 1.0f // 1-second melee tick

            val distance = getDistance(attacker, target)
            val damage = calculateDamage(attacker, target, distance, attacker.isFirstChargeTick)
            target.currentHp -= damage

            // Stamp timestamp when melee strike connects
            attacker.lastAttackTimestamp = System.currentTimeMillis()

            attacker.isFirstChargeTick = false
            return true
        }
        return false
    }

    private fun processChasing(unit: GameUnit, allUnits: List<GameUnit>, deltaTime: Float) {
        val target = allUnits.find { it.id == unit.targetUnitId }

        if (target == null || target.state == UnitState.ROUTING) {
            breakChase(unit)
            return
        }

        val distance = getDistance(unit, target)
        val engagementRange = if (unit.isMelee) 10f else unit.baseStats.effectiveRange

        if (distance > engagementRange) {
            // Target is out of range, increment outrun timer
            unit.chaseOutrunTimer += deltaTime
            if (unit.chaseOutrunTimer >= 2.0f) {
                breakChase(unit) // Outrun grace window exceeded
            } else {
                // Move towards target
                unit.destinationX = target.x
                unit.destinationY = target.y
                processMovement(unit, deltaTime)
            }
        } else {
            // Caught up, reset timer and begin engagement
            unit.chaseOutrunTimer = 0f
            unit.state = UnitState.ROTATING
            unit.isMoving = false
        }
    }

    private fun breakChase(unit: GameUnit) {
        unit.targetUnitId = null
        unit.state = UnitState.RETURNING
        unit.isMoving = true
        // Additional logic to set destinationX/Y to corps position can go here
    }

    private fun processRouting(unit: GameUnit, deltaTime: Float) {
        val maxMorale = unit.baseStats.maxMorale?.toFloat() ?: return
        val rallyThreshold = maxMorale * 0.15f

        if (unit.isInCommanderAura) { // Simplified safety check
            val currentMorale = unit.currentMorale ?: 0f
            unit.currentMorale = currentMorale + (unit.baseStats.moraleRegenPerSecIdle * deltaTime)

            if (unit.currentMorale!! >= rallyThreshold) {
                unit.state = UnitState.IDLE
            }
        }
    }

    private fun calculateDamage(attacker: GameUnit, target: GameUnit, distance: Float, isFirstChargeTick: Boolean): Int {
        var baseDamage = if (attacker.isMelee) attacker.baseStats.meleeDamage.toFloat() else attacker.baseStats.volleyDamage.toFloat()
        var rangeMult = 1.0f

        if (!attacker.isMelee) {
            val maxRange = attacker.baseStats.effectiveRange
            if (attacker.unitClass == UnitClass.ARTILLERY && distance <= attacker.baseStats.canisterRange) {
                baseDamage = attacker.baseStats.pointBlankVolleyDamage?.toFloat() ?: baseDamage
            } else {
                rangeMult = when {
                    distance <= maxRange * 0.25f -> 1.15f
                    distance <= maxRange * 0.75f -> 1.0f
                    else -> 0.75f
                }
            }
        }

        val hpRatio = attacker.currentHp.toFloat() / attacker.baseStats.maxHp.toFloat()
        val hpRatioMult = (0.4f + 0.6f * hpRatio).coerceAtLeast(0.4f)

        val moveMult = if (target.state in listOf(UnitState.MOVING, UnitState.CHASING, UnitState.RETURNING)) 0.75f else 1.0f

        var chargeMult = 1.0f
        if (attacker.isCavalry && isFirstChargeTick) {
            chargeMult = 1.0f + (attacker.baseStats.chargeBonus ?: 0f)
        }

        val randRoll = Random.nextDouble(0.85, 1.00).toFloat()

        return (baseDamage * hpRatioMult * rangeMult * moveMult * chargeMult * randRoll).roundToInt()
    }

    private fun updateMorale(unit: GameUnit, deltaTimeSec: Float, tookDamageThisTick: Boolean) {
        if (unit.state == UnitState.ROUTING || unit.state == UnitState.SURRENDERING) return

        val maxMorale = unit.baseStats.maxMorale?.toFloat() ?: return // Skip morale checks for Commanders
        var currentMorale = unit.currentMorale ?: maxMorale

        var decayMultiplier = 1.0f
        if (unit.isEncircled) decayMultiplier *= 2.0f
        if (unit.isInCommanderAura) decayMultiplier *= 0.8f // Assuming 20% resistance bonus

        if (unit.state == UnitState.MOVING || unit.state == UnitState.CHASING) {
            currentMorale -= (unit.baseStats.moraleDecayPerSecMoving * deltaTimeSec * decayMultiplier)
        }

        if (unit.isSupplyCut) {
            currentMorale -= (unit.baseStats.supplyCutMoraleDrainPerSec * deltaTimeSec)
        }

        if (tookDamageThisTick) {
            currentMorale -= (maxMorale * unit.baseStats.moraleDecayPerHitPct * decayMultiplier)
            unit.timeSinceLastCombat = 0f
        } else {
            unit.timeSinceLastCombat += deltaTimeSec
        }

        if (unit.state == UnitState.IDLE && unit.timeSinceLastCombat >= 3.0f) {
            currentMorale += (unit.baseStats.moraleRegenPerSecIdle * deltaTimeSec)
        }

        unit.currentMorale = currentMorale.coerceIn(0f, maxMorale)

        if (unit.currentMorale!! <= 0f) {
            if (unit.isEncircled && !unit.hasFleePath) {
                unit.state = UnitState.SURRENDERING
            } else {
                unit.state = UnitState.ROUTING
                unit.clearCurrentOrders()
            }
        }
    }

    // Helper math functions
    private fun getDistance(u1: GameUnit, u2: GameUnit): Float {
        val dx = u2.x - u1.x
        val dy = u2.y - u1.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    private fun findNearestEnemyInRange(unit: GameUnit, allUnits: List<GameUnit>, range: Float): GameUnit? {
        return allUnits.filter {
            it.faction != unit.faction &&
                    it.currentHp > 0 &&
                    it.state != UnitState.ROUTING
        }.minByOrNull { getDistance(unit, it) }
            ?.takeIf { getDistance(unit, it) <= range }
    }
}