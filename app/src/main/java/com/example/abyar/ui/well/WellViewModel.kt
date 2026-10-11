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
    private val userDao = db.userDao()

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

    fun clearError() { _error.value = "" }

    /**
     * محاسبه کل ساعت آب از مدت مدار
     * هر روز = 24 ساعت
     */
    fun calculateTotalHours(cycleDays: Int): Double {
        return (cycleDays * 24).toDouble()
    }

    /**
     * ذخیره چاه جدید
     */
    fun saveWell(
        name: String,
        location: String,
        cycleDays: Int,
        onSuccess: (Well) -> Unit
    ) {
        viewModelScope.launch {
            when {
                name.isBlank() -> { _error.value = "نام چاه الزامی است"; return@launch }
                cycleDays <= 0 -> { _error.value = "مدت مدار باید بیشتر از صفر باشد"; return@launch }
                cycleDays > 365 -> { _error.value = "مدت مدار نمی‌تواند بیشتر از 365 روز باشد"; return@launch }
            }

            val existing = wellDao.getFirstWellSync()
            if (existing != null) {
                _error.value = "یک چاه قبلاً ثبت شده است"
                return@launch
            }

            val code = generateUniqueCode()
            val totalHours = calculateTotalHours(cycleDays)
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

    /**
     * ویرایش چاه موجود
     */
    fun updateWell(
        wellId: String,
        name: String,
        location: String,
        cycleDays: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            when {
                name.isBlank() -> { _error.value = "نام چاه الزامی است"; return@launch }
                cycleDays <= 0 -> { _error.value = "مدت مدار باید بیشتر از صفر باشد"; return@launch }
                cycleDays > 365 -> { _error.value = "مدت مدار نمی‌تواند بیشتر از 365 روز باشد"; return@launch }
            }

            val existing = wellDao.getWellById(wellId) ?: run {
                _error.value = "چاه یافت نشد"
                return@launch
            }

            val newTotalHours = calculateTotalHours(cycleDays)
            val usedHours = userDao.getTotalShareHours(wellId) ?: 0.0

            if (usedHours > newTotalHours) {
                _error.value = "مجموع سهم مالکان (${usedHours} ساعت) از کل ساعت جدید (${newTotalHours} ساعت) بیشتر است. ابتدا سهم مالکان را کاهش دهید."
                return@launch
            }

            wellDao.updateWell(
                existing.copy(
                    name = name.trim(),
                    location = location.trim().ifBlank { null },
                    cycleDurationDays = cycleDays,
                    totalHours = newTotalHours
                )
            )
            _error.value = ""
            onSuccess()
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
