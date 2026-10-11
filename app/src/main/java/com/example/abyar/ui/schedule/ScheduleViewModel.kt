package com.example.abyar.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.*
import com.example.abyar.util.jalaliToTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScheduleStats(
    val totalTurns: Int,
    val cycleDays: Int,
    val startDate: Long?,
    val endDate: Long?,
    val participantsCount: Int,
    val completedCount: Int,
    val pendingCount: Int
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val turnDao = db.turnDao()
    private val userDao = db.userDao()
    private val wellDao = db.wellDao()

    private val _turns = MutableStateFlow<List<IrrigationTurn>>(emptyList())
    val turns: StateFlow<List<IrrigationTurn>> = _turns

    private val _stats = MutableStateFlow(
        ScheduleStats(0, 0, null, null, 0, 0, 0)
    )
    val stats: StateFlow<ScheduleStats> = _stats

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _info = MutableStateFlow("")
    val info: StateFlow<String> = _info

    fun clearMessages() { _error.value = ""; _info.value = "" }

    fun loadTurns(wellId: String) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: return@launch
            turnDao.getTurnsByWell(wellId).collect { list ->
                _turns.value = list
                val completed = list.count { it.status == "COMPLETED" }
                val pending = list.count { it.status == "PENDING" }
                val participants = list.map { it.userId }.distinct().size
                _stats.value = ScheduleStats(
                    totalTurns = list.size,
                    cycleDays = well.cycleDurationDays,
                    startDate = list.firstOrNull()?.startTime,
                    endDate = list.lastOrNull()?.endTime,
                    participantsCount = participants,
                    completedCount = completed,
                    pendingCount = pending
                )
            }
        }
    }

    fun generateSchedule(
        wellId: String,
        startJalaliYear: Int,
        startJalaliMonth: Int,
        startJalaliDay: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: run {
                _error.value = "چاه یافت نشد"
                return@launch
            }
            val owners = userDao.getUsersByWellSync(wellId)
                .filter { it.shareHours > 0.0 }

            if (owners.isEmpty()) {
                _error.value = "هیچ مالکی با سهم آب ثبت نشده است"
                return@launch
            }

            val totalShare = owners.sumOf { it.shareHours }
            if (totalShare <= 0.0) {
                _error.value = "مجموع سهم مالکان صفر است"
                return@launch
            }

            // تبدیل تاریخ شروع شمسی به timestamp
            val startTime = jalaliToTimestamp(
                startJalaliYear, startJalaliMonth, startJalaliDay, 6, 0
            )

            // حذف برنامه قبلی
            turnDao.deleteTurnsByWell(wellId)

            // تولید برنامه جدید
            val newTurns = Scheduler.generateSchedule(well, owners, startTime)
            turnDao.insertTurns(newTurns)

            _error.value = ""
            _info.value = "${newTurns.size} نوبت با موفقیت تولید شد"
            onSuccess()
        }
    }

    fun toggleTurnStatus(turn: IrrigationTurn, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val newStatus = when (turn.status) {
                "PENDING" -> "COMPLETED"
                "COMPLETED" -> "PENDING"
                else -> "PENDING"
            }
            turnDao.insertTurns(listOf(turn.copy(status = newStatus)))
            onSuccess()
        }
    }

    fun deleteTurn(turn: IrrigationTurn, onSuccess: () -> Unit) {
        viewModelScope.launch {
            turnDao.deleteTurn(turn.id)
            onSuccess()
        }
    }

    fun clearAllTurns(wellId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            turnDao.deleteTurnsByWell(wellId)
            _info.value = "کل برنامه پاک شد"
            onSuccess()
        }
    }
}
