package com.example.abyar.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.*
import com.example.abyar.util.jalaliToTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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

    private val _owners = MutableStateFlow<List<User>>(emptyList())
    val owners: StateFlow<List<User>> = _owners

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
            // بارگذاری لیست مالکان برای پنل جابجایی
            _owners.value = userDao.getUsersByWellSync(wellId)
                .filter { it.shareHours > 0.0 }

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

            val startTime = jalaliToTimestamp(
                startJalaliYear, startJalaliMonth, startJalaliDay, 6, 0
            )

            turnDao.deleteTurnsByWell(wellId)
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

    // ============ قابلیت جدید ۱: جابجایی نوبت ============

    /**
     * نوبت فعلی را به کاربر دیگری منتقل می‌کند (زمان ثابت می‌ماند)
     */
    fun moveTurnToUser(
        turn: IrrigationTurn,
        newUserId: String,
        newUserName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (turn.userId == newUserId) {
                _error.value = "نوبت قبلاً به این شخص تعلق دارد"
                return@launch
            }
            val updated = turn.copy(
                userId = newUserId,
                userName = newUserName
            )
            turnDao.insertTurns(listOf(updated))
            _info.value = "نوبت به ${newUserName} منتقل شد"
            onSuccess()
        }
    }

    // ============ قابلیت جدید ۲: شکستن نوبت ============

    /**
     * شکستن نوبت به دو بخش با نقطه دلخواه (ساعت)
     * @param splitAfterHours تعداد ساعتی که بخش اول طول می‌کشد
     */
    fun splitTurnIntoTwo(
        turn: IrrigationTurn,
        splitAfterHours: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val totalMs = turn.endTime - turn.startTime
            val totalHours = totalMs / (1000.0 * 60 * 60)

            if (splitAfterHours <= 0 || splitAfterHours >= totalHours) {
                _error.value = "زمان شکستن باید بین ۰ و ${String.format("%.1f", totalHours)} ساعت باشد"
                return@launch
            }

            val splitMs = (splitAfterHours * 60 * 60 * 1000).toLong()
            val splitTime = turn.startTime + splitMs

            val firstTurn = turn.copy(
                id = UUID.randomUUID().toString(),
                endTime = splitTime,
                status = "PENDING"
            )
            val secondTurn = turn.copy(
                id = UUID.randomUUID().toString(),
                startTime = splitTime,
                status = "PENDING"
            )

            turnDao.deleteTurn(turn.id)
            turnDao.insertTurns(listOf(firstTurn, secondTurn))
            _info.value = "نوبت به دو بخش شکسته شد"
            onSuccess()
        }
    }

    /**
     * شکستن نوبت به N بخش مساوی
     */
    fun splitTurnIntoEqualParts(
        turn: IrrigationTurn,
        partsCount: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (partsCount < 2 || partsCount > 10) {
                _error.value = "تعداد بخش‌ها باید بین ۲ و ۱۰ باشد"
                return@launch
            }

            val totalMs = turn.endTime - turn.startTime
            val partMs = totalMs / partsCount

            turnDao.deleteTurn(turn.id)
            val newTurns = (0 until partsCount).map { i ->
                turn.copy(
                    id = UUID.randomUUID().toString(),
                    startTime = turn.startTime + (partMs * i),
                    endTime = if (i == partsCount - 1) turn.endTime
                    else turn.startTime + (partMs * (i + 1)),
                    status = "PENDING"
                )
            }
            turnDao.insertTurns(newTurns)
            _info.value = "نوبت به $partsCount بخش مساوی شکسته شد"
            onSuccess()
        }
    }
}
