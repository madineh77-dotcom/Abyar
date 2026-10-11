package com.example.abyar.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.AppDatabase
import com.example.abyar.data.IrrigationTurn
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

    private val _well = MutableStateFlow<Well?>(null)
    val well: StateFlow<Well?> = _well

    private val _nextTurn = MutableStateFlow<IrrigationTurn?>(null)
    val nextTurn: StateFlow<IrrigationTurn?> = _nextTurn

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Job برای ردیابی collect قبلی (تا در تعویض چاه، جمع‌آوری قبلی لغو شود)
    private var loadJob: Job? = null

    fun loadWell(wellId: String, nationalCode: String) {
        // لغو کامل عملیات قبلی قبل از شروع عملیات جدید
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            // پاک کردن داده‌های قبلی برای جلوگیری از نمایش داده‌های چاه قدیم
            _well.value = null
            _nextTurn.value = null
            _currentUser.value = null

            val w = wellDao.getWellById(wellId)
            if (w == null) {
                return@launch
            }
            _well.value = w

            val u = userDao.getUserByNationalCode(w.id, nationalCode)
            if (u == null) {
                return@launch
            }
            _currentUser.value = u

            turnDao.getNextPendingTurn(w.id, u.id).collect { turn ->
                _nextTurn.value = turn
            }
        }
    }

    fun clear() {
        loadJob?.cancel()
        loadJob = null
        _well.value = null
        _nextTurn.value = null
        _currentUser.value = null
    }
}
