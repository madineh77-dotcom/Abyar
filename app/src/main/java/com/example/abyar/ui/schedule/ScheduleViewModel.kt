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

data class OwnerScheduleSummary(
    val userId: String,
    val userName: String,
    val shareHours: Double,
    val scheduledHours: Double,
    val turnsCount: Int
) {
    val difference: Double get() = scheduledHours - shareHours
    val isBalanced: Boolean get() = kotlin.math.abs(difference) < 0.05
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val turnDao = db.turnDao()
    private val userDao = db.userDao()
    private val wellDao = db.wellDao()

    private val _turns = MutableStateFlow<List<IrrigationTurn>>(emptyList())
    val turns: StateFlow<List<IrrigationTurn>> = _turns

    private val _owners = MutableStateFlow<List<User>>(emptyList())
    val owners: StateFlow<List<User>> = _owners

    private val _ownerSummaries = MutableStateFlow<List<OwnerScheduleSummary>>(emptyList())
    val ownerSummaries: StateFlow<List<OwnerScheduleSummary>> = _ownerSummaries

    private val _stats = MutableStateFlow(ScheduleStats(0, 0, null, null, 0, 0, 0))
    val stats: StateFlow<ScheduleStats> = _stats

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _info = MutableStateFlow("")
    val info: StateFlow<String> = _info

    fun clearMessages() { _error.value = ""; _info.value = "" }

    fun loadTurns(wellId: String) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: return@launch
            val allOwners = userDao.getUsersByWellSync(wellId).filter { it.shareHours > 0.0 }
            _owners.value = allOwners

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

                val summaries = allOwners.map { owner ->
                    val userTurns = list.filter { it.userId == owner.id }
                    val totalMs = userTurns.sumOf { it.endTime - it.startTime }
                    val totalHours = totalMs / (1000.0 * 60.0 * 60.0)
                    OwnerScheduleSummary(
                        userId = owner.id,
                        userName = owner.fullName,
                        shareHours = owner.shareHours,
                        scheduledHours = totalHours,
                        turnsCount = userTurns.size
                    )
                }
                _ownerSummaries.value = summaries
            }
        }
    }

    fun generateSchedule(wellId: String, startJalaliYear: Int, startJalaliMonth: Int, startJalaliDay: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: run { _error.value = "چاه یافت نشد"; return@launch }
            val owners = userDao.getUsersByWellSync(wellId).filter { it.shareHours > 0.0 }
            if (owners.isEmpty()) { _error.value = "هیچ مالکی با سهم ثبت نشده است"; return@launch }

            val startTime = jalaliToTimestamp(startJalaliYear, startJalaliMonth, startJalaliDay, 6, 0)
            turnDao.deleteTurnsByWell(wellId)
            val newTurns = Scheduler.generateSchedule(well, owners, startTime)
            turnDao.insertTurns(newTurns)

            _error.value = ""
            _info.value = "${newTurns.size} نوبت تولید شد"
            onSuccess()
        }
    }

    fun toggleTurnStatus(turn: IrrigationTurn, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val newStatus = if (turn.status == "PENDING") "COMPLETED" else "PENDING"
            turnDao.insertTurns(listOf(turn.copy(status = newStatus)))
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

    // ==========================================
    // منطق جدید: جابجایی هوشمند و حذف با شیفت
    // ==========================================

    /**
     * جابجایی دو نوبت با انتخاب زمان مقصد
     * پس از جابجایی، کل برنامه از ابتدا بازسازی می‌شود تا شیفت‌ها اعمال شوند.
     */
    fun swapTurns(turnA: IrrigationTurn, turnB: IrrigationTurn, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (turnA.id == turnB.id) {
                _error.value = "نمی‌توانید یک نوبت را با خودش جابجا کنید"
                return@launch
            }

            val allTurns = turnDao.getTurnsByWellSync(turnA.wellId).sortedBy { it.startTime }.toMutableList()
            val indexA = allTurns.indexOfFirst { it.id == turnA.id }
            val indexB = allTurns.indexOfFirst { it.id == turnB.id }

            if (indexA == -1 || indexB == -1) {
                _error.value = "نوبت‌ها یافت نشدند"
                return@launch
            }

            // جابجایی جایگاه در لیست
            val temp = allTurns[indexA]
            allTurns[indexA] = allTurns[indexB]
            allTurns[indexB] = temp

            // بازسازی زمان‌ها (شیفت خودکار)
            var currentTime = allTurns.first().startTime
            val updatedTurns = allTurns.map { t ->
                val duration = t.endTime - t.startTime
                val newTurn = t.copy(
                    startTime = currentTime,
                    endTime = currentTime + duration
                )
                currentTime += duration
                newTurn
            }

            turnDao.insertTurns(updatedTurns)
            _error.value = ""
            _info.value = "نوبت‌ها جابجا و برنامه شیفت داده شد"
            onSuccess()
        }
    }

    /**
     * حذف نوبت با شیفت دادن خودکار نوبت‌های بعدی به عقب
     */
    fun deleteTurnAndShift(turn: IrrigationTurn, onSuccess: () -> Unit) {
        viewModelScope.launch {
            turnDao.deleteTurn(turn.id)
            val remaining = turnDao.getTurnsByWellSync(turn.wellId).sortedBy { it.startTime }
            
            if (remaining.isNotEmpty()) {
                var currentTime = remaining.first().startTime
                val updatedTurns = remaining.map { t ->
                    val duration = t.endTime - t.startTime
                    val newTurn = t.copy(
                        startTime = currentTime,
                        endTime = currentTime + duration
                    )
                    currentTime += duration
                    newTurn
                }
                turnDao.insertTurns(updatedTurns)
            }
            _info.value = "نوبت حذف و برنامه شیفت داده شد"
            onSuccess()
        }
    }
}
