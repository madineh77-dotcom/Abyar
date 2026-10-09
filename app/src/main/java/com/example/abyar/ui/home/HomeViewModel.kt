package com.example.abyar.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.AppDatabase
import com.example.abyar.data.IrrigationTurn
import com.example.abyar.data.User
import com.example.abyar.data.Well
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

    fun loadWell(wellId: String, nationalCode: String) {
        viewModelScope.launch {
            val w = wellDao.getWellById(wellId)
            _well.value = w
            if (w == null) {
                _nextTurn.value = null
                _currentUser.value = null
                return@launch
            }

            val u = userDao.getUserByNationalCode(w.id, nationalCode)
            _currentUser.value = u

            if (u != null) {
                turnDao.getNextPendingTurn(w.id, u.id).collect { turn ->
                    _nextTurn.value = turn
                }
            } else {
                _nextTurn.value = null
            }
        }
    }

    fun clear() {
        _well.value = null
        _nextTurn.value = null
        _currentUser.value = null
    }
}
