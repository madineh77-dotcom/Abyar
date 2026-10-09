package com.example.abyar.ui.well

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.AppDatabase
import com.example.abyar.data.Well
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class WellViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val wellDao = db.wellDao()

    private val _well = MutableStateFlow<Well?>(null)
    val well: StateFlow<Well?> = _well

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init {
        viewModelScope.launch {
            wellDao.getFirstWell().collect { _well.value = it }
        }
    }

    fun saveWell(
        name: String,
        location: String,
        totalHours: Double,
        cycleDays: Int,
        onSuccess: (Well) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank()) { _error.value = "نام چاه الزامی است"; return@launch }
            if (totalHours <= 0) { _error.value = "کل ساعت آب باید بیشتر از صفر باشد"; return@launch }
            if (cycleDays <= 0) { _error.value = "مدت مدار باید بیشتر از صفر باشد"; return@launch }

            val existing = wellDao.getFirstWellSync()
            if (existing != null) {
                _error.value = "یک چاه قبلاً ثبت شده است"
                return@launch
            }

            val code = generateUniqueCode()
            val newWell = Well(
                id = UUID.randomUUID().toString(),
                code = code,
                name = name.trim(),
                location = location.trim().ifBlank { null },
                totalHours = totalHours,
                cycleDurationDays = cycleDays
            )
            wellDao.insertWell(newWell)
            _error.value = ""
            _saved.value = true
            onSuccess(newWell)
        }
    }

    private suspend fun generateUniqueCode(): String {
        var code: String
        var attempts = 0
        do {
            code = Random.nextInt(10000000, 100000000).toString()
            attempts++
        } while (wellDao.countByCode(code) > 0 && attempts < 50)
        return code
    }
}
