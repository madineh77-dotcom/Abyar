package com.example.abyar.ui.notify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.AppDatabase
import com.example.abyar.data.OwnerGroup
import com.example.abyar.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GroupInfo(
    val group: OwnerGroup,
    val membersCount: Int,
    val totalShares: Double
)

class NotifyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val groupDao = db.groupDao()
    private val userDao = db.userDao()

    private val _groups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val groups: StateFlow<List<GroupInfo>> = _groups

    private val _leaders = MutableStateFlow<List<User>>(emptyList())
    val leaders: StateFlow<List<User>> = _leaders

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers

    private val _selectedGroupMembers = MutableStateFlow<List<User>>(emptyList())
    val selectedGroupMembers: StateFlow<List<User>> = _selectedGroupMembers

    fun loadData(wellId: String) {
        viewModelScope.launch {
            val allUsersList = userDao.getUsersByWellSync(wellId)
            val allGroups = groupDao.getGroupsByWellSync(wellId)

            _allUsers.value = allUsersList

            val leaderIds = allGroups.map { it.leaderId }.toSet()
            _leaders.value = allUsersList.filter { it.id in leaderIds }

            _groups.value = allGroups.map { g ->
                val members = allUsersList.filter { it.groupId == g.id }
                GroupInfo(
                    group = g,
                    membersCount = members.size,
                    totalShares = members.sumOf { it.shareHours }
                )
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            userDao.getUsersByGroup(groupId).collect { members ->
                _selectedGroupMembers.value = members
            }
        }
    }
}
