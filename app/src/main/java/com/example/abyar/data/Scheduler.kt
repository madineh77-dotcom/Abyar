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

        // ۱. بررسی دستمزد موتوربان از کل چاه
        val operator = owners.find { it.role == UserRole.OPERATOR }
        if (operator != null && 
            operator.operatorWageType == WageType.FROM_TOTAL && 
            operator.operatorWageAmount > 0 && 
            operator.includeWageInSchedule) {
            
            val wageMs = (operator.operatorWageAmount * 60.0 * 60.0 * 1000.0).toLong()
            turns.add(
                IrrigationTurn(
                    id = UUID.randomUUID().toString(),
                    wellId = well.id,
                    userId = operator.id,
                    userName = "دستمزد موتوربان (${operator.fullName})",
                    startTime = currentTime,
                    endTime = currentTime + wageMs,
                    status = "PENDING"
                )
            )
            currentTime += wageMs
        }

        // ۲. نوبت مالکان (به جز کسانی که سهم ۰ دارند)
        val withWater = owners.filter { it.shareHours > 0.0 }
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
