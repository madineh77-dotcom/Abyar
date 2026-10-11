package com.example.abyar.ui.groups

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class GroupWithDetails(
    val group: OwnerGroup,
    val leaderName: String,
    val membersCount: Int,
    val totalShares: Double
)

class GroupViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val groupDao = db.groupDao()
    private val userDao = db.userDao()
    private val wellDao = db.wellDao()

    private val _groups = MutableStateFlow<List<GroupWithDetails>>(emptyList())
    val groups: StateFlow<List<GroupWithDetails>> = _groups

    private val _ungroupedUsers = MutableStateFlow<List<User>>(emptyList())
    val ungroupedUsers: StateFlow<List<User>> = _ungroupedUsers

    private val _currentGroupMembers = MutableStateFlow<List<User>>(emptyList())
    val currentGroupMembers: StateFlow<List<User>> = _currentGroupMembers

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _info = MutableStateFlow("")
    val info: StateFlow<String> = _info

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun clearMessages() { _error.value = ""; _info.value = "" }

    fun loadGroups(wellId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val allGroups = groupDao.getGroupsByWellSync(wellId)
                val allUsers = userDao.getUsersByWellSync(wellId)

                val details = allGroups.map { g ->
                    val leader = allUsers.find { it.id == g.leaderId }
                    val members = allUsers.filter { it.groupId == g.id }
                    GroupWithDetails(
                        group = g,
                        leaderName = leader?.fullName ?: "نامشخص",
                        membersCount = members.size,
                        totalShares = members.sumOf { it.shareHours }
                    )
                }
                _groups.value = details
                _ungroupedUsers.value = allUsers.filter { it.groupId == null && it.shareHours > 0.0 }
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            userDao.getUsersByGroup(groupId).collect { list ->
                _currentGroupMembers.value = list
            }
        }
    }

    /**
     * ساخت گروه جدید
     * قانون: سرگروه باید یک کاربر موجود با سهم > 5 ساعت باشد
     */
    fun createGroup(
        wellId: String,
        groupName: String,
        leaderId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            when {
                groupName.isBlank() -> { _error.value = "نام گروه الزامی است"; return@launch }
                leaderId.isBlank() -> { _error.value = "سرگروه باید انتخاب شود"; return@launch }
            }

            // بررسی اینکه سرگروه قبلاً در گروه دیگری نباشد
            val leader = userDao.getUserById(leaderId)
            if (leader == null) {
                _error.value = "سرگروه یافت نشد"
                return@launch
            }
            if (leader.groupId != null) {
                _error.value = "این کاربر قبلاً در گروه دیگری است"
                return@launch
            }

            val groupId = UUID.randomUUID().toString()
            val newGroup = OwnerGroup(
                id = groupId,
                wellId = wellId,
                name = groupName.trim(),
                leaderId = leaderId
            )
            groupDao.insertGroup(newGroup)

            // اضافه کردن سرگروه به گروه
            userDao.updateUser(leader.copy(groupId = groupId))

            _error.value = ""
            _info.value = "گروه «$groupName» با موفقیت ساخته شد"
            onSuccess()
        }
    }

    /**
     * حذف گروه
     * توجه: همه اعضا به حالت «بدون گروه» برمی‌گردند
     */
    fun deleteGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // خارج کردن همه اعضا از گروه
            val members = userDao.getUsersByGroupSync(groupId)
            for (m in members) {
                userDao.updateUser(m.copy(groupId = null))
            }
            groupDao.deleteGroup(groupId)
            _info.value = "گروه حذف شد و اعضا به حالت بدون گروه برگشتند"
            onSuccess()
        }
    }

    /**
     * افزودن یک کاربر به گروه
     */
    fun addUserToGroup(userId: String, groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserById(userId) ?: run {
                _error.value = "کاربر یافت نشد"
                return@launch
            }

            if (user.groupId != null) {
                _error.value = "این کاربر قبلاً در گروهی است"
                return@launch
            }

            userDao.updateUser(user.copy(groupId = groupId))
            _info.value = "${user.fullName} به گروه اضافه شد"
            onSuccess()
        }
    }

    /**
     * خارج کردن یک کاربر از گروه
     */
    fun removeUserFromGroup(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserById(userId) ?: return@launch
            userDao.updateUser(user.copy(groupId = null))
            _info.value = "${user.fullName} از گروه خارج شد"
            onSuccess()
        }
    }

    /**
     * تغییر سرگروه یک گروه
     */
    fun changeGroupLeader(groupId: String, newLeaderId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val group = groupDao.getGroupById(groupId) ?: run {
                _error.value = "گروه یافت نشد"
                return@launch
            }
            val newLeader = userDao.getUserById(newLeaderId) ?: run {
                _error.value = "کاربر جدید یافت نشد"
                return@launch
            }

            // اگر سرگروه جدید قبلاً در گروه دیگری بود، خطا
            if (newLeader.groupId != null && newLeader.groupId != groupId) {
                _error.value = "این کاربر در گروه دیگری است"
                return@launch
            }

            // اضافه کردن سرگروه جدید به گروه
            userDao.updateUser(newLeader.copy(groupId = groupId))

            // به‌روزرسانی سرگروه در جدول گروه
            groupDao.updateGroup(group.copy(leaderId = newLeaderId))

            _error.value = ""
            _info.value = "سرگروه با موفقیت تغییر کرد"
            onSuccess()
        }
    }
}
