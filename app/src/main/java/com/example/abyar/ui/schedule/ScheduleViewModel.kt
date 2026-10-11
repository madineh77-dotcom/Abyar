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
            val allOwners = userDao.getUsersByWellSync(wellId)
                .filter { it.shareHours > 0.0 }
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

                // محاسبه خلاصه هر مالک
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

    /**
     * شکستن نوبت به دو بخش با نقطه دلخواه
     * مجموع دو بخش = زمان نوبت اصلی (بدون کم و زیاد شدن)
     */
    fun splitTurnIntoTwo(
        turn: IrrigationTurn,
        splitAfterHours: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val totalMs = turn.endTime - turn.startTime
            val totalHours = totalMs / (1000.0 * 60.0 * 60.0)

            if (splitAfterHours <= 0.0 || splitAfterHours >= totalHours) {
                _error.value = "زمان شکستن باید بین ۰ و ${String.format("%.1f", totalHours)} ساعت باشد"
                return@launch
            }

            val splitMs = (splitAfterHours * 60.0 * 60.0 * 1000.0).toLong()
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
            _info.value = "نوبت به دو بخش شکسته شد (مجموع ثابت: ${String.format("%.1f", totalHours)} ساعت)"
            onSuccess()
        }
    }

    /**
     * شکستن نوبت به N بخش مساوی
     * مجموع بخش‌ها = زمان نوبت اصلی
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

            if (partMs < 60_000L) {
                _error.value = "هر بخش کمتر از ۱ دقیقه می‌شود. تعداد بخش‌ها را کم کنید."
                return@launch
            }

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
            val totalHours = totalMs / (1000.0 * 60.0 * 60.0)
            _info.value = "نوبت به $partsCount بخش مساوی شکسته شد (مجموع ثابت: ${String.format("%.1f", totalHours)} ساعت)"
            onSuccess()
        }
    }
}
