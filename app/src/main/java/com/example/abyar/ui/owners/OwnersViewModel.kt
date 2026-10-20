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
    val mainAdmin: User?,
    val majorOwners: List<User>,
    val smallOwners: List<User>,
    val operators: List<User>
)

class OwnersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val wellDao = db.wellDao()

    private val _stats = MutableStateFlow(OwnersStats(0.0, 0.0, 0.0, 0))
    val stats: StateFlow<OwnersStats> = _stats

    private val _grouped = MutableStateFlow(
        GroupedOwners(null, emptyList(), emptyList(), emptyList())
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

            val mainAdmin = allUsers.find { it.role == UserRole.MAIN_ADMIN }
            val majorOwners = allUsers.filter { it.role == UserRole.MAJOR_OWNER }.sortedByDescending { it.shareHours }
            val smallOwners = allUsers.filter { it.role == UserRole.SMALL_OWNER }.sortedByDescending { it.shareHours }
            val operators = allUsers.filter { it.role == UserRole.OPERATOR }

            _grouped.value = GroupedOwners(mainAdmin, majorOwners, smallOwners, operators)
        }
    }

    fun saveUser(
        wellId: String,
        existingUser: User?,
        fullName: String,
        phone: String,
        nationalCode: String,
        shareHours: Double,
        role: UserRole,
        isGroupLeader: Boolean,
        operatorWageType: WageType?,
        operatorWageAmount: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: run { _error.value = "چاه یافت نشد"; return@launch }

            // اعتبارسنجی‌ها
            when {
                fullName.isBlank() -> { _error.value = "نام الزامی است"; return@launch }
                phone.length != 11 -> { _error.value = "شماره تماس باید ۱۱ رقم باشد"; return@launch }
                nationalCode.length != 10 -> { _error.value = "کد ملی باید ۱۰ رقم باشد"; return@launch }
                shareHours < 0 -> { _error.value = "سهم نمی‌تواند منفی باشد"; return@launch }
            }

            // بررسی تکراری نبودن کد ملی (اگر کاربر جدید است یا کد ملی عوض شده)
            if (existingUser == null || existingUser.nationalCode != nationalCode) {
                val dup = userDao.getUserByNationalCode(wellId, nationalCode)
                if (dup != null) {
                    _error.value = "کاربری با این کد ملی در این چاه ثبت شده است"
                    return@launch
                }
            }

            // بررسی مجموع سهم (با کسر سهم قبلی در صورت ویرایش)
            val currentTotal = userDao.getTotalShareHours(wellId) ?: 0.0
            val previousShare = existingUser?.shareHours ?: 0.0
            val otherSharesTotal = currentTotal - previousShare

            if (otherSharesTotal + shareHours > well.totalHours) {
                val remaining = well.totalHours - otherSharesTotal
                _error.value = "سهم وارد شده از باقیمانده (${remaining} ساعت) بیشتر است"
                return@launch
            }

            if (existingUser == null) {
                // ثبت جدید
                val user = User(
                    id = UUID.randomUUID().toString(),
                    wellId = wellId,
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    nationalCode = nationalCode.trim(),
                    shareHours = shareHours,
                    role = role,
                    isGroupLeader = isGroupLeader,
                    operatorWageType = if (role == UserRole.OPERATOR) operatorWageType else null,
                    operatorWageAmount = if (role == UserRole.OPERATOR) operatorWageAmount else 0.0,
                    groupId = null
                )
                userDao.insertUser(user)
            } else {
                // ویرایش
                val updated = existingUser.copy(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    nationalCode = nationalCode.trim(),
                    shareHours = shareHours,
                    role = role,
                    isGroupLeader = isGroupLeader,
                    operatorWageType = if (role == UserRole.OPERATOR) operatorWageType else null,
                    operatorWageAmount = if (role == UserRole.OPERATOR) operatorWageAmount else 0.0
                )
                userDao.updateUser(updated)
            }

            _error.value = ""
            onSuccess()
        }
    }

    fun deleteOwner(user: User, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (user.role == UserRole.MAIN_ADMIN) {
                _error.value = "مدیر اصلی قابل حذف نیست"
                return@launch
            }
            userDao.deleteUser(user.id)
            onSuccess()
        }
    }
}
