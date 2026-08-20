package com.faiqbaig.eaw.combat

import com.faiqbaig.eaw.core.GameUnit
import com.faiqbaig.eaw.core.UnitState
import com.faiqbaig.eaw.core.UnitClass
import com.faiqbaig.eaw.core.UnitSubtype
import kotlin.math.*
import kotlin.random.Random

object BattleEngine {

    // --- SOUND CALLBACKS ---
    var onVolleyFired: (() -> Unit)? = null
    var onArtilleryFired: (() -> Unit)? = null

    fun updateTick(units: MutableList<GameUnit>, rawDeltaTime: Float) {
        val deltaTime = rawDeltaTime.coerceAtMost(0.05f)
        val fadeDuration = 1.5f // 1.5 seconds fade-out effect

        val iterator = units.iterator()
        while (iterator.hasNext()) {
            val unit = iterator.next()

            // Handle dead units: Fade out and remove when fully transparent
            if (unit.currentHp <= 0) {
                unit.deathTimer += deltaTime
                unit.alpha = (1.0f - (unit.deathTimer / fadeDuration)).coerceAtLeast(0f)

                if (unit.alpha <= 0f) {
                    iterator.remove() // Cleanly remove from map list
                }
                continue
            }

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
                UnitState.SURRENDERING -> { }
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

                if (u1.currentHp <= 0 || u2.currentHp <= 0) continue

                val dx = u2.x - u1.x
                val dy = u2.y - u1.y
                val distSq = dx * dx + dy * dy

                if (distSq in 0.0001f..minDistanceSq) {

                    // HOSTILE CONTACT
                    if (u1.faction != u2.faction) {
                        // DISENGAGE CHECK: If either unit was given a explicit MOVING order, allow them to pull away
                        val u1IsWithdrawing = u1.state == UnitState.MOVING
                        val u2IsWithdrawing = u2.state == UnitState.MOVING

                        if (!u1IsWithdrawing && !u2IsWithdrawing) {
                            if (u1.state != UnitState.ROUTING && u2.state != UnitState.ROUTING) {
                                if (u1.unitClass == UnitClass.CAVALRY || u2.unitClass == UnitClass.CAVALRY ||
                                    u1.state == UnitState.CHASING || u2.state == UnitState.CHASING) {

                                    if (u1.state != UnitState.IN_MELEE) {
                                        u1.targetUnitId = u2.id
                                        u1.state = UnitState.IN_MELEE
                                        u1.isFirstChargeTick = (u1.unitClass == UnitClass.CAVALRY && u1.state == UnitState.CHASING)
                                        u1.meleeTickTimer = 1.0f // Strike immediately on contact
                                    }

                                    if (u2.state != UnitState.IN_MELEE) {
                                        u2.targetUnitId = u1.id
                                        u2.state = UnitState.IN_MELEE
                                        u2.isFirstChargeTick = (u2.unitClass == UnitClass.CAVALRY && u2.state == UnitState.CHASING)
                                        u2.meleeTickTimer = 1.0f // Strike immediately on contact
                                    }
                                }
                            }
                        }

                        // Apply a minor physical push so withdrawing units can break physical overlap
                        val dist = sqrt(distSq)
                        val overlap = (minDistance - dist) / 2f
                        val nx = dx / dist
                        val ny = dy / dist

                        u1.x -= nx * overlap * 0.3f
                        u1.y -= ny * overlap * 0.3f
                        u2.x += nx * overlap * 0.3f
                        u2.y += ny * overlap * 0.3f

                        continue
                    }

                    // FRIENDLY SEPARATION
                    val dist = sqrt(distSq)
                    val overlap = (minDistance - dist) / 2f
                    val nx = dx / dist
                    val ny = dy / dist
                    val pushFactor = if (u1.state == UnitState.MOVING || u2.state == UnitState.MOVING) 0.2f else 0.5f

                    u1.x -= nx * overlap * pushFactor
                    u1.y -= ny * overlap * pushFactor
                    u2.x += nx * overlap * pushFactor
                    u2.y += ny * overlap * pushFactor
                }
            }
        }
    }

    private fun moveTowards(unit: GameUnit, targetX: Float, targetY: Float, deltaTime: Float) {
        val dx = targetX - unit.x
        val dy = targetY - unit.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 1f) {
            val speed = unit.baseStats.moveSpeed
            val moveDist = speed * deltaTime
            val actualMove = min(moveDist, dist)

            val nx = dx / dist
            val ny = dy / dist

            unit.x += nx * actualMove
            unit.y += ny * actualMove

            // SMOOTH ROTATION: Interpolate toward movement vector instead of snap-assigning
            val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val targetAngle = rawAngle + 90f
            val angleDiff = normalizeAngle(targetAngle - unit.rotation)
            val step = unit.baseStats.rotationSpeed * deltaTime

            unit.rotation += angleDiff.coerceIn(-step, step)
            unit.isMoving = true
        } else {
            unit.isMoving = false
        }
    }

    private fun processIdle(unit: GameUnit, allUnits: List<GameUnit>) {
        // Auto-detect enemies entering range
        val target = findNearestEnemyInRange(unit, allUnits, unit.baseStats.detectionRadius)
        if (target != null) {
            unit.targetUnitId = target.id

            val dx = target.x - unit.x
            val dy = target.y - unit.y
            val distSq = dx * dx + dy * dy
            val meleeContactRangeSq = 35f * 35f // 1225f

            // CAVALRY: If enemy is already in physical contact range, enter melee immediately
            if (unit.unitClass == UnitClass.CAVALRY && distSq <= meleeContactRangeSq) {
                unit.state = UnitState.IN_MELEE
                unit.isFirstChargeTick = false // Idle counter-attack gets no charge bonus
                unit.meleeTickTimer = 0f
            } else {
                // Pivot toward enemy first
                unit.state = UnitState.ROTATING
            }
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

        // Align target angle calculation with the north sprite edge
        val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val targetAngle = rawAngle + 90f

        val angleDiff = normalizeAngle(targetAngle - unit.rotation)

        if (abs(angleDiff) <= 15f) { // Within 15-degree tolerance
            unit.rotation = targetAngle
            unit.isMoving = false

            val distance = getDistance(unit, target)
            val meleeContactRange = 60f // Aligned with collision minDistance (30f radius * 2)

            // --- CAVALRY / MELEE TRANSITION ---
            if (unit.unitClass == UnitClass.CAVALRY || unit.isMelee) {
                if (distance <= meleeContactRange) {
                    unit.state = UnitState.IN_MELEE
                    unit.isFirstChargeTick = (unit.unitClass == UnitClass.CAVALRY)
                    unit.meleeTickTimer = 1.0f // Strike immediately on contact
                } else {
                    unit.state = UnitState.CHASING // Close distance first
                }
            }
            // --- RANGED TRANSITION ---
            else {
                if (distance <= unit.baseStats.effectiveRange) {
                    if (unit.state != UnitState.FIRING) {
                        unit.state = UnitState.FIRING
                        unit.reloadTimer = 0f // Fire initial volley immediately once aimed
                    }
                } else {
                    unit.state = UnitState.CHASING
                }
            }
        } else {
            val step = unit.baseStats.rotationSpeed * deltaTime
            unit.rotation += if (angleDiff > 0) min(step, angleDiff) else max(-step, angleDiff)
        }
    }

    private fun processFiring(attacker: GameUnit, allUnits: List<GameUnit>, deltaTime: Float): Boolean {
        // HARD LOCK: Prevent Cavalry from ever firing ranged attacks
        if (attacker.unitClass == UnitClass.CAVALRY) {
            attacker.state = UnitState.CHASING
            return false
        }

        val target = allUnits.find { it.id == attacker.targetUnitId }
        if (target == null || target.state == UnitState.ROUTING || target.currentHp <= 0) {
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

            // --- TRIGGER SOUND EFFECTS BASED ON UNIT TYPE ---
            when (attacker.unitClass) {
                UnitClass.INFANTRY -> onVolleyFired?.invoke()
                UnitClass.ARTILLERY -> onArtilleryFired?.invoke()
                else -> {}
            }

            return true
        }
        return false
    }

    private fun processMelee(attacker: GameUnit, allUnits: List<GameUnit>, deltaTime: Float): Boolean {
        val target = allUnits.find { it.id == attacker.targetUnitId }

        if (target == null || target.currentHp <= 0 || target.state == UnitState.ROUTING) {
            attacker.clearCurrentOrders()
            attacker.state = UnitState.IDLE
            return false
        }

        // 1. DISTANCE GUARD (Must be larger than collision minDistance of 60f)
        val distance = getDistance(attacker, target)
        val maxMeleeRange = 70f // Increased to allow combat within 60f collision bubble

        if (distance > maxMeleeRange) {
            if (attacker.unitClass == UnitClass.CAVALRY) {
                attacker.state = UnitState.CHASING
            } else {
                attacker.clearCurrentOrders()
                attacker.state = UnitState.IDLE
            }
            return false
        }

        // 2. MELEE TICK TIMER
        attacker.meleeTickTimer += deltaTime
        val meleeInterval = 1.0f

        if (attacker.meleeTickTimer >= meleeInterval) {
            attacker.meleeTickTimer = 0f

            var damage = attacker.baseStats.meleeDamage.toFloat()

            if (attacker.isFirstChargeTick) {
                val chargeMultiplier = 1.0f + (attacker.baseStats.chargeBonus ?: 0f)
                damage *= chargeMultiplier
                attacker.isFirstChargeTick = false
            }

            target.currentHp -= damage.toInt()
            attacker.lastAttackTimestamp = System.currentTimeMillis()

            return true
        }

        return false
    }

    private fun processChasing(attacker: GameUnit, allUnits: List<GameUnit>, deltaTime: Float) {
        val target = allUnits.find { it.id == attacker.targetUnitId }
        if (target == null || target.state == UnitState.ROUTING || target.currentHp <= 0) {
            attacker.clearCurrentOrders()
            return
        }

        val distance = getDistance(attacker, target)
        val meleeContactRange = 35f // Collision distance for melee combat

        // CAVALRY: Always forces close-quarters engagement
        if (attacker.unitClass == UnitClass.CAVALRY) {
            if (distance <= meleeContactRange) {
                attacker.state = UnitState.IN_MELEE
                attacker.isFirstChargeTick = true // Apply cavalry charge bonus damage
                attacker.meleeTickTimer = 0f     // Strike immediately on impact
            } else {
                // Move directly toward target
                moveTowards(attacker, target.x, target.y, deltaTime)
            }
            return
        }

        // INFANTRY & ARTILLERY: Engage at effective range
        if (distance <= attacker.baseStats.effectiveRange) {
            attacker.state = UnitState.FIRING
        } else {
            moveTowards(attacker, target.x, target.y, deltaTime)
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