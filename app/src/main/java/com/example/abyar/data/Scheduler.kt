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

        val operator = owners.find { it.role == UserRole.OPERATOR }
        val wageHours = if (
            operator != null &&
            operator.includeWageInSchedule &&
            operator.operatorWageAmount > 0
        ) operator.operatorWageAmount else 0.0

        // ۱. دستمزد از کل چاه (قبل از نوبت مالکان)
        if (operator != null &&
            operator.operatorWageType == WageType.FROM_TOTAL &&
            wageHours > 0
        ) {
            val wageMs = (wageHours * 60.0 * 60.0 * 1000.0).toLong()
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

        // ۲. نوبت مالکان (کسر دستمزد از سهم خود موتوربان در صورت لزوم)
        val sorted = owners
            .filter { it.shareHours > 0.0 }
            .sortedByDescending { it.shareHours }

        for (owner in sorted) {
            var share = owner.shareHours

            if (operator != null &&
                owner.id == operator.id &&
                operator.operatorWageType == WageType.FROM_OWN_SHARE &&
                wageHours > 0
            ) {
                share = (share - wageHours).coerceAtLeast(0.0)
            }

            if (share <= 0.0) continue

            val shareMs = (share * 60.0 * 60.0 * 1000.0).toLong()
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
