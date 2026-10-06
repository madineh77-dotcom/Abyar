package com.example.abyar.data

import java.util.UUID

object Scheduler {
    fun generateSchedule(
        well: Well,
        owners: List<User>,
        startTime: Long
    ): List<IrrigationTurn> {
        val turns = mutableListOf<IrrigationTurn>()
        var currentTime = startTime
        val totalHours = owners.sumOf { it.shareHours ?: 0.0 }
        if (totalHours <= 0.0) return turns
        val cycleDurationMs = well.cycleDurationDays * 24 * 60 * 60 * 1000L

        for (owner in owners) {
            val ownerHours = owner.shareHours ?: 0.0
            if (ownerHours <= 0.0) continue
            val shareRatio = ownerHours / totalHours
            val durationMs = (cycleDurationMs * shareRatio).toLong()

            turns.add(
                IrrigationTurn(
                    id = UUID.randomUUID().toString(),
                    wellId = well.id,
                    userId = owner.id,
                    userName = owner.fullName,
                    startTime = currentTime,
                    endTime = currentTime + durationMs,
                    status = "PENDING"
                )
            )
            currentTime += durationMs
        }
        return turns
    }
}
