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
    val groupLeaders: List<User>,
    val smallOwners: List<User>,
    val operatorsAndAbyars: List<User>
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

            // آمار: کل ساعت تخصیص‌یافته فقط از مالکان (بدون موتوربان/آبیار که سهم 0 دارند)
            val allocated = allUsers.sumOf { it.shareHours }
            _stats.value = OwnersStats(
                totalHours = well.totalHours,
                allocatedHours = allocated,
                remainingHours = well.totalHours - allocated,
                totalOwners = allUsers.count { it.shareHours > 0 }
            )

            // گروه‌بندی بر اساس نقش
            val mainAdmin = allUsers.find { it.role == UserRole.MAIN_ADMIN }
            val groupLeaders = allUsers.filter { it.role == UserRole.GROUP_LEADER }
                .sortedByDescending { it.shareHours }
            val smallOwners = allUsers.filter { it.role == UserRole.SMALL_OWNER }
                .sortedByDescending { it.shareHours }
            val operatorsAndAbyars = allUsers.filter {
                it.role == UserRole.OPERATOR || it.role == UserRole.ABYAR
            }

            _grouped.value = GroupedOwners(
                mainAdmin = mainAdmin,
                groupLeaders = groupLeaders,
                smallOwners = smallOwners,
                operatorsAndAbyars = operatorsAndAbyars
            )
        }
    }

    fun addOwner(
        wellId: String,
        fullName: String,
        phone: String,
        nationalCode: String,
        shareHours: Double,
        role: UserRole,
        operatorWageType: WageType?,
        operatorWageAmount: Double,
        abyarWageType: WageType?,
        abyarWageAmount: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: run {
                _error.value = "چاه یافت نشد"
                return@launch
            }

            // اعتبارسنجی‌ها
            when {
                fullName.isBlank() -> { _error.value = "نام الزامی است"; return@launch }
                phone.length != 11 -> { _error.value = "شماره تماس باید ۱۱ رقم باشد"; return@launch }
                nationalCode.length != 10 -> { _error.value = "کد ملی باید ۱۰ رقم باشد"; return@launch }
                shareHours < 0 -> { _error.value = "سهم نمی‌تواند منفی باشد"; return@launch }
            }

            // نقش MAIN_ADMIN فقط یکی می‌تواند وجود داشته باشد
            if (role == UserRole.MAIN_ADMIN) {
                val existingAdmin = userDao.getMainAdmin(wellId)
                if (existingAdmin != null) {
                    _error.value = "مدیر اصلی قبلاً تعیین شده است. برای تغییر، از گزینه تغییر مدیر استفاده کنید."
                    return@launch
                }
            }

            // نقش OPERATOR فقط یکی
            if (role == UserRole.OPERATOR) {
                val existingOp = userDao.getOperator(wellId)
                if (existingOp != null) {
                    _error.value = "موتوربان قبلاً تعیین شده است"
                    return@launch
                }
            }

            // سرگروه باید سهم > 5 داشته باشد
            if (role == UserRole.GROUP_LEADER && shareHours <= 5.0) {
                _error.value = "سرگروه باید سهم بیشتر از ۵ ساعت داشته باشد"
                return@launch
            }
            if (role == UserRole.SMALL_OWNER && shareHours > 5.0) {
                _error.value = "خرده‌مالک باید سهم ۵ ساعت یا کمتر داشته باشد"
                return@launch
            }

            // بررسی تکراری بودن کد ملی
            val existing = userDao.getUserByNationalCode(wellId, nationalCode)
            if (existing != null) {
                _error.value = "کاربری با این کد ملی در این چاه ثبت شده است"
                return@launch
            }

            // بررسی مجموع سهم (فقط برای نقش‌هایی که سهم دارند)
            if (shareHours > 0) {
                val currentTotal = userDao.getTotalShareHours(wellId) ?: 0.0
                if (currentTotal + shareHours > well.totalHours) {
                    val remaining = well.totalHours - currentTotal
                    _error.value = "سهم شما از باقیمانده (${remaining} ساعت) بیشتر است"
                    return@launch
                }
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                wellId = wellId,
                fullName = fullName.trim(),
                phone = phone.trim(),
                nationalCode = nationalCode.trim(),
                shareHours = shareHours,
                role = role,
                operatorWageType = if (role == UserRole.OPERATOR) operatorWageType else null,
                operatorWageAmount = if (role == UserRole.OPERATOR) operatorWageAmount else 0.0,
                abyarWageType = if (role == UserRole.ABYAR) abyarWageType else null,
                abyarWageAmount = if (role == UserRole.ABYAR) abyarWageAmount else 0.0,
                groupId = null
            )
            userDao.insertUser(user)
            _error.value = ""
            onSuccess()
        }
    }

    fun deleteOwner(user: User, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (user.role == UserRole.MAIN_ADMIN) {
                _error.value = "مدیر اصلی قابل حذف نیست. ابتدا مدیر جدید تعیین کنید."
                return@launch
            }
            userDao.deleteUser(user.id)
            onSuccess()
        }
    }
}
