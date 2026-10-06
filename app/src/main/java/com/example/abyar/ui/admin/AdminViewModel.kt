package com.example.abyar.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val wellDao = db.wellDao()
    private val userDao = db.userDao()
    private val turnDao = db.turnDao()
    private val transactionDao = db.transactionDao()

    private val _well = MutableStateFlow<Well?>(null)
    val well: StateFlow<Well?> = _well

    private val _owners = MutableStateFlow<List<User>>(emptyList())
    val owners: StateFlow<List<User>> = _owners

    private val _turns = MutableStateFlow<List<IrrigationTurn>>(emptyList())
    val turns: StateFlow<List<IrrigationTurn>> = _turns

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    init {
        viewModelScope.launch {
            wellDao.getFirstWell().collect { w ->
                _well.value = w
                if (w != null) {
                    userDao.getOwnersByWell(w.id).collect { _owners.value = it }
                    turnDao.getTurnsByWell(w.id).collect { _turns.value = it }
                    transactionDao.getTransactionsByWell(w.id).collect { _transactions.value = it }
                }
            }
        }
    }

    fun createWell(name: String, totalShares: Int, cycleDays: Int) {
        viewModelScope.launch {
            val wellId = UUID.randomUUID().toString()
            wellDao.insertWell(Well(wellId, name, totalShares, cycleDays, "HYBRID"))
        }
    }

    fun addOwner(fullName: String, phone: String, role: UserRole, hours: Double) {
        val currentWell = _well.value ?: return
        viewModelScope.launch {
            userDao.insertUser(
                User(
                    id = UUID.randomUUID().toString(),
                    wellId = currentWell.id,
                    fullName = fullName,
                    phone = phone,
                    role = role,
                    shareHours = hours
                )
            )
        }
    }

    fun generateTurns() {
        val currentWell = _well.value ?: return
        val ownerList = _owners.value
        if (ownerList.isEmpty()) return
        viewModelScope.launch {
            turnDao.deleteTurnsByWell(currentWell.id)
            val newTurns = Scheduler.generateSchedule(currentWell, ownerList, System.currentTimeMillis())
            turnDao.insertTurns(newTurns)
        }
    }

    fun addTransaction(type: String, category: String, amount: Long) {
        val currentWell = _well.value ?: return
        viewModelScope.launch {
            transactionDao.insertTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    wellId = currentWell.id,
                    type = type,
                    category = category,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    description = ""
                )
            )
        }
    }
}
