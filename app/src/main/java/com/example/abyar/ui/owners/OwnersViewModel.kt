package com.example.abyar.ui.owners

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class OwnersStats(
    val totalHours: Double,
    val allocatedHours: Double,
    val remainingHours: Double,
    val totalOwners: Int
)

data class GroupedOwners(
    val majorOwners: List<User>,
    val membersOfMajor: Map<String, List<User>>,
    val independentSmallOwners: List<User>,
    val operatorsAndAbyars: List<User>
)

class OwnersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val wellDao = db.wellDao()

    private val _stats = MutableStateFlow(OwnersStats(0.0, 0.0, 0.0, 0))
    val stats: StateFlow<OwnersStats> = _stats

    private val _grouped = MutableStateFlow(
        GroupedOwners(emptyList(), emptyMap(), emptyList(), emptyList())
    )
    val grouped: StateFlow<GroupedOwners> = _grouped

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    fun clearError() { _error.value = "" }

    fun loadOwners(wellId: String) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: return@launch
            val allUsers = userDao.getUsersByWellSync(wellId)

            val allocated = allUsers.sumOf { it.shareHours }
            _stats.value = OwnersStats(
                totalHours = well.totalHours,
                allocatedHours = allocated,
                remainingHours = well.totalHours - allocated,
                totalOwners = allUsers.count { it.shareHours > 0 }
            )

            val majorOwners = allUsers.filter { it.shareHours > 5.0 }
            val smallOwners = allUsers.filter { it.shareHours in 0.01..5.0 }
            val operatorAbyars = allUsers.filter { it.isOperator || it.isAbyar }

            // گروه‌بندی خرده‌مالکان بر اساس گروه
            val memberMap = mutableMapOf<String, List<User>>()
            for (major in majorOwners) {
                // پیدا کردن گروهی که این مالک عمده سرگروه آن است
                val group = db.groupDao().getGroupsByWellSync(wellId)
                    .find { it.leaderId == major.id }
                if (group != null) {
                    memberMap[major.id] = smallOwners.filter { it.groupId == group.id }
                } else {
                    memberMap[major.id] = emptyList()
                }
            }

            val independentSmall = smallOwners.filter { it.groupId == null }

            _grouped.value = GroupedOwners(
                majorOwners = majorOwners,
                membersOfMajor = memberMap,
                independentSmallOwners = independentSmall,
                operatorsAndAbyars = operatorAbyars
            )
        }
    }

    fun addOwner(
        wellId: String,
        fullName: String,
        phone: String,
        nationalCode: String,
        shareHours: Double,
        isOperator: Boolean,
        operatorWageType: WageType?,
        operatorWageAmount: Double,
        isAbyar: Boolean,
        abyarWageType: WageType?,
        abyarWageAmount: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: run {
                _error.value = "چاه یافت نشد"
                return@launch
            }

            when {
                fullName.isBlank() -> { _error.value = "نام الزامی است"; return@launch }
                phone.length < 11 -> { _error.value = "شماره تماس باید ۱۱ رقم باشد"; return@launch }
                nationalCode.length != 10 -> { _error.value = "کد ملی باید ۱۰ رقم باشد"; return@launch }
                shareHours < 0 -> { _error.value = "سهم نمی‌تواند منفی باشد"; return@launch }
            }

            val existing = userDao.getUserByNationalCode(wellId, nationalCode)
            if (existing != null) {
                _error.value = "کاربری با این کد ملی در این چاه ثبت شده است"
                return@launch
            }

            val currentTotal = userDao.getTotalShareHours(wellId) ?: 0.0
            if (currentTotal + shareHours > well.totalHours) {
                val remaining = well.totalHours - currentTotal
                _error.value = "سهم شما از باقیمانده (${remaining} ساعت) بیشتر است"
                return@launch
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                wellId = wellId,
                fullName = fullName.trim(),
                phone = phone.trim(),
                nationalCode = nationalCode.trim(),
                shareHours = shareHours,
                isOperator = isOperator,
                operatorWageType = if (isOperator) operatorWageType else null,
                operatorWageAmount = if (isOperator) operatorWageAmount else 0.0,
                isAbyar = isAbyar,
                abyarWageType = if (isAbyar) abyarWageType else null,
                abyarWageAmount = if (isAbyar) abyarWageAmount else 0.0,
                groupId = null
            )
            userDao.insertUser(user)
            _error.value = ""
            onSuccess()
        }
    }

    fun deleteOwner(user: User, onSuccess: () -> Unit) {
        viewModelScope.launch {
            userDao.deleteUser(user.id)
            onSuccess()
        }
    }
}
