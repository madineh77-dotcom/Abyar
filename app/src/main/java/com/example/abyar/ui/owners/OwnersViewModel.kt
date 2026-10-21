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
    val operators: List<User>,
    val availableLeaders: List<User>,
    val leaderGroupMap: Map<String, String> = emptyMap()
)

class OwnersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val wellDao = db.wellDao()
    private val groupDao = db.groupDao()

    private val _stats = MutableStateFlow(OwnersStats(0.0, 0.0, 0.0, 0))
    val stats: StateFlow<OwnersStats> = _stats

    private val _grouped = MutableStateFlow(
        GroupedOwners(null, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap())
    )
    val grouped: StateFlow<GroupedOwners> = _grouped

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    fun clearError() { _error.value = "" }

    fun loadOwners(wellId: String) {
        viewModelScope.launch {
            val well = wellDao.getWellById(wellId) ?: return@launch
            val allUsers = userDao.getUsersByWellSync(wellId)
            val allGroups = groupDao.getGroupsByWellSync(wellId)

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

            val leaderGroupMap = allGroups.associate { it.leaderId to it.id }
            val leaders = allUsers.filter { it.isGroupLeader && leaderGroupMap.containsKey(it.id) }

            _grouped.value = GroupedOwners(
                mainAdmin = mainAdmin,
                majorOwners = majorOwners,
                smallOwners = smallOwners,
                operators = operators,
                availableLeaders = leaders,
                leaderGroupMap = leaderGroupMap
            )
        }
    }

    fun saveUser(
        currentUserRole: UserRole?,
        wellId: String,
        existingUser: User?,
        fullName: String,
        phone: String,
        nationalCode: String,
        shareHours: Double,
        role: UserRole,
        isGroupLeader: Boolean,
        groupId: String?,
        operatorWageType: WageType?,
        operatorWageAmount: Double,
        includeWageInSchedule: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // ============ بررسی دسترسی ============
            when (currentUserRole) {
                UserRole.MAIN_ADMIN -> { /* دسترسی کامل */ }
                UserRole.OPERATOR -> {
                    if (existingUser != null) {
                        _error.value = "موتوربان اجازه ویرایش کاربران را ندارد"
                        return@launch
                    }
                    if (role != UserRole.SMALL_OWNER) {
                        _error.value = "موتوربان فقط می‌تواند خرده‌مالک جدید اضافه کند"
                        return@launch
                    }
                    if (isGroupLeader) {
                        _error.value = "موتوربان نمی‌تواند سرگروه تعیین کند"
                        return@launch
                    }
                }
                else -> {
                    _error.value = "شما اجازه این عملیات را ندارید"
                    return@launch
                }
            }

            val well = wellDao.getWellById(wellId) ?: run {
                _error.value = "چاه یافت نشد"; return@launch
            }

            when {
                fullName.isBlank() -> { _error.value = "نام الزامی است"; return@launch }
                phone.length != 11 -> { _error.value = "شماره تماس باید ۱۱ رقم باشد"; return@launch }
                nationalCode.length != 10 -> { _error.value = "کد ملی باید ۱۰ رقم باشد"; return@launch }
                shareHours < 0 -> { _error.value = "سهم نمی‌تواند منفی باشد"; return@launch }
                isGroupLeader && groupId != null -> {
                    _error.value = "سرگروه نمی‌تواند همزمان عضو گروه دیگری باشد"; return@launch
                }
            }

            if (existingUser == null || existingUser.nationalCode != nationalCode) {
                val duplicate = userDao.getUserByNationalCode(wellId, nationalCode)
                if (duplicate != null) {
                    _error.value = "کاربری با کد ملی ${nationalCode} در این چاه وجود دارد (${duplicate.fullName})"
                    return@launch
                }
            }

            val currentTotal = userDao.getTotalShareHours(wellId) ?: 0.0
            val previousShare = existingUser?.shareHours ?: 0.0
            if ((currentTotal - previousShare) + shareHours > well.totalHours) {
                _error.value = "سهم وارد شده از باقیمانده بیشتر است"; return@launch
            }

            val userId = existingUser?.id ?: UUID.randomUUID().toString()
            val effectiveGroupId: String?

            if (isGroupLeader) {
                val existingLeaderGroup = groupDao.getGroupsByWellSync(wellId)
                    .find { it.leaderId == userId }
                if (existingLeaderGroup != null) {
                    effectiveGroupId = existingLeaderGroup.id
                } else {
                    val newGroupId = UUID.randomUUID().toString()
                    groupDao.insertGroup(
                        OwnerGroup(
                            id = newGroupId,
                            wellId = wellId,
                            name = "گروه ${fullName.trim()}",
                            leaderId = userId
                        )
                    )
                    effectiveGroupId = newGroupId
                }
            } else {
                if (existingUser != null && existingUser.isGroupLeader) {
                    val oldGroup = groupDao.getGroupsByWellSync(wellId)
                        .find { it.leaderId == existingUser.id }
                    if (oldGroup != null) {
                        userDao.getUsersByGroupSync(oldGroup.id).forEach { m ->
                            userDao.updateUser(m.copy(groupId = null))
                        }
                        groupDao.deleteGroup(oldGroup.id)
                    }
                }
                effectiveGroupId = groupId
            }

            val user = User(
                id = userId,
                wellId = wellId,
                fullName = fullName.trim(),
                phone = phone.trim(),
                nationalCode = nationalCode.trim(),
                shareHours = shareHours,
                role = role,
                isGroupLeader = isGroupLeader,
                operatorWageType = if (role == UserRole.OPERATOR) operatorWageType else null,
                operatorWageAmount = if (role == UserRole.OPERATOR) operatorWageAmount else 0.0,
                includeWageInSchedule = if (role == UserRole.OPERATOR) includeWageInSchedule else true,
                groupId = effectiveGroupId,
                createdAt = existingUser?.createdAt ?: System.currentTimeMillis()
            )

            if (existingUser == null) userDao.insertUser(user) else userDao.updateUser(user)

            _error.value = ""
            onSuccess()
        }
    }

    fun deleteOwner(
        user: User,
        currentUserRole: UserRole?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // ============ بررسی دسترسی ============
            if (currentUserRole != UserRole.MAIN_ADMIN) {
                _error.value = "فقط مدیر اصلی می‌تواند کاربران را حذف کند"
                return@launch
            }

            if (user.role == UserRole.MAIN_ADMIN) {
                _error.value = "مدیر اصلی قابل حذف نیست"
                return@launch
            }

            if (user.isGroupLeader) {
                val leaderGroup = groupDao.getGroupsByWellSync(user.wellId)
                    .find { it.leaderId == user.id }
                if (leaderGroup != null) {
                    userDao.getUsersByGroupSync(leaderGroup.id).forEach { m ->
                        userDao.updateUser(m.copy(groupId = null))
                    }
                    groupDao.deleteGroup(leaderGroup.id)
                }
            }

            userDao.deleteUser(user.id)
            onSuccess()
        }
    }
}
