package com.example.abyar.data

import java.util.UUID

object Scheduler {
    /**
     * تولید خودکار نوبت‌بندی بر اساس سهم ساعتی مالکان
     * هر مالک دقیقاً به اندازه سهم ساعتی خودش زمان می‌گیرد.
     */
    fun generateSchedule(
        well: Well,
        owners: List<User>,
        startTime: Long
    ): List<IrrigationTurn> {
        val turns = mutableListOf<IrrigationTurn>()
        var currentTime = startTime

        val withWater = owners.filter { it.shareHours > 0.0 }
        if (withWater.isEmpty()) return turns

        // مرتب‌سازی بر اساس سهم نزولی (مالکان عمده اول)
        val sorted = withWater.sortedByDescending { it.shareHours }

        for (owner in sorted) {
            val shareMs = (owner.shareHours * 60.0 * 60.0 * 1000.0).toLong()
            turns.add(
                IrrigationTurn(
                    id = UUID.randomUUID().toString(),
                    wellId = well.id,
                    userId = owner.id,
                    userName = owner.fullName,
                    startTime = currentTime,
                    endTime = currentTime + shareMs,
                    status = "PENDING"
                )
            )
            currentTime += shareMs
        }
        return turns
    }
}
