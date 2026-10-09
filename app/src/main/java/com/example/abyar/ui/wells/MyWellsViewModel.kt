package com.example.abyar.ui.wells

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.abyar.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class WellWithShare(
    val well: Well,
    val shareHours: Double
)

class MyWellsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val wellDao = db.wellDao()
    private val userDao = db.userDao()

    private val _myWells = MutableStateFlow<List<WellWithShare>>(emptyList())
    val myWells: StateFlow<List<WellWithShare>> = _myWells

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun loadMyWells(nationalCode: String) {
        viewModelScope.launch {
            val userRecords = userDao.getAllByNationalCode(nationalCode)
            val result = mutableListOf<WellWithShare>()
            for (u in userRecords) {
                val w = wellDao.getWellById(u.wellId)
                if (w != null) {
                    result.add(WellWithShare(w, u.shareHours))
                }
            }
            _myWells.value = result
        }
    }

    fun clearError() { _error.value = "" }

    // ایجاد چاه جدید (کاربر مدیر آن می‌شود)
    fun createNewWell(
        nationalCode: String,
        fullName: String,
        phone: String,
        wellName: String,
        location: String,
        totalHours: Double,
        cycleDays: Int,
        myShare: Double,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            when {
                wellName.isBlank() -> { _error.value = "نام چاه الزامی است"; return@launch }
                totalHours <= 0 -> { _error.value = "کل ساعت آب باید بیشتر از صفر باشد"; return@launch }
                cycleDays <= 0 -> { _error.value = "مدت مدار باید بیشتر از صفر باشد"; return@launch }
                myShare <= 0 -> { _error.value = "سهم شما باید بیشتر از صفر باشد"; return@launch }
                myShare > totalHours -> { _error.value = "سهم شما نمی‌تواند از کل ساعت بیشتر باشد"; return@launch }
            }

            val code = generateUniqueCode()
            val wellId = UUID.randomUUID().toString()
            val well = Well(
                id = wellId,
                code = code,
                name = wellName.trim(),
                location = location.trim().ifBlank { null },
                totalHours = totalHours,
                cycleDurationDays = cycleDays
            )
            wellDao.insertWell(well)

            val user = User(
                id = UUID.randomUUID().toString(),
                wellId = wellId,
                fullName = fullName,
                phone = phone,
                nationalCode = nationalCode,
                shareHours = myShare
            )
            userDao.insertUser(user)

            _error.value = ""
            _success.value = true
            onSuccess(wellId)
        }
    }

    // پیوستن به چاه موجود با کد
    fun joinWellByCode(
        nationalCode: String,
        fullName: String,
        phone: String,
        wellCode: String,
        myShare: Double,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (wellCode.length != 8) { _error.value = "کد چاه باید ۸ رقمی باشد"; return@launch }
            if (myShare <= 0) { _error.value = "سهم شما باید بیشتر از صفر باشد"; return@launch }

            val well = wellDao.getWellByCode(wellCode)
            if (well == null) {
                _error.value = "چاهی با این کد یافت نشد"
                return@launch
            }

            // بررسی تکراری بودن کاربر
            val existing = userDao.getUserByNationalCode(well.id, nationalCode)
            if (existing != null) {
                _error.value = "شما قبلاً در این چاه ثبت شده‌اید"
                return@launch
            }

            // بررسی اعتبارسنجی مجموع سهم
            val totalShares = userDao.getTotalShareHours(well.id) ?: 0.0
            if (totalShares + myShare > well.totalHours) {
                _error.value = "مجموع سهم‌ها از کل ساعت چاه (${well.totalHours}) بیشتر می‌شود"
                return@launch
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                wellId = well.id,
                fullName = fullName,
                phone = phone,
                nationalCode = nationalCode,
                shareHours = myShare
            )
            userDao.insertUser(user)

            _error.value = ""
            _success.value = true
            onSuccess(well.id)
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
