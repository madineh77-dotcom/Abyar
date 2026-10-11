package com.example.abyar.data

import java.util.UUID

object Scheduler {
    /**
     * تولید خودکار نوبت‌بندی بر اساس سهم ساعتی مالکان
     *
     * قانون: هر مالک دقیقاً به اندازه سهم ساعتی خود در برنامه، زمان آبیاری می‌گیرد.
     * مثلاً: کسی که سهمش 5 ساعت است، مجموع زمان نوبت‌هایش در برنامه = 5 ساعت.
     */
    fun generateSchedule(
        well: Well,
        owners: List<User>,
        startTime: Long
    ): List<IrrigationTurn> {
        val turns = mutableListOf<IrrigationTurn>()
        var currentTime = startTime

        // فقط کسانی که سهم آب دارند
        val withWater = owners.filter { it.shareHours > 0.0 }
        if (withWater.isEmpty()) return turns

        // هر مالک دقیقاً به اندازه سهم ساعتی خود نوبت می‌گیرد
        for (owner in withWater) {
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
