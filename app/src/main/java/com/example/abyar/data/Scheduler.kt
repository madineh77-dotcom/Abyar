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

        // فقط کسانی که سهم آب دارند
        val withWater = owners.filter { it.shareHours > 0.0 }
        val totalHours = withWater.sumOf { it.shareHours }
        if (totalHours <= 0.0) return turns

        val cycleDurationMs = well.cycleDurationDays * 24L * 60L * 60L * 1000L

        for (owner in withWater) {
            val shareRatio = owner.shareHours / totalHours
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
