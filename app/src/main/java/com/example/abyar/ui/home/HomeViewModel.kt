package com.example.abyar.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.AppDatabase
import com.example.abyar.data.IrrigationTurn
import com.example.abyar.data.Notification
import com.example.abyar.data.User
import com.example.abyar.data.Well
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val wellDao = db.wellDao()
    private val turnDao = db.turnDao()
    private val userDao = db.userDao()
    private val notificationDao = db.notificationDao()

    private val _well = MutableStateFlow<Well?>(null)
    val well: StateFlow<Well?> = _well

    private val _nextTurn = MutableStateFlow<IrrigationTurn?>(null)
    val nextTurn: StateFlow<IrrigationTurn?> = _nextTurn

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _operator = MutableStateFlow<User?>(null)
    val operator: StateFlow<User?> = _operator

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _wellNotFound = MutableStateFlow(false)
    val wellNotFound: StateFlow<Boolean> = _wellNotFound

    private var loadJob: Job? = null

    fun loadWell(wellId: String, nationalCode: String) {
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            // Reset
            _well.value = null
            _nextTurn.value = null
            _currentUser.value = null
            _operator.value = null
            _notifications.value = emptyList()
            _wellNotFound.value = false

            val w = wellDao.getWellById(wellId)
            if (w == null) {
                _wellNotFound.value = true
                return@launch
            }
            _well.value = w

            // بارگذاری موتوربان
            _operator.value = userDao.getOperator(w.id)

            val u = userDao.getUserByNationalCode(w.id, nationalCode) ?: return@launch
            _currentUser.value = u

            // بارگذاری نوبت بعدی
            launch {
                turnDao.getNextPendingTurn(w.id, u.id).collect { turn ->
                    _nextTurn.value = turn
                }
            }

            // بارگذاری اطلاعیه‌ها
            launch {
                notificationDao.getNotificationsForUser(w.id, u.id, u.groupId).collect { list ->
                    _notifications.value = list
                }
            }
        }
    }

    fun clear() {
        loadJob?.cancel()
        loadJob = null
        _well.value = null
        _nextTurn.value = null
        _currentUser.value = null
        _operator.value = null
        _notifications.value = emptyList()
        _wellNotFound.value = false
    }
}
