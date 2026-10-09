package com.example.abyar.util

import android.content.Context

/**
 * ذخیره و بازیابی اطلاعات کاربر در SharedPreferences
 * (در آینده با احراز هویت واقعی جایگزین می‌شود)
 */
class UserPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("abyar_user", Context.MODE_PRIVATE)

    var fullName: String
        get() = prefs.getString("full_name", "کاربر آبیار") ?: "کاربر آبیار"
        set(value) = prefs.edit().putString("full_name", value).apply()

    var phone: String
        get() = prefs.getString("phone", "۰۹۱۲۳۴۵۶۷۸۹") ?: "۰۹۱۲۳۴۵۶۷۸۹"
        set(value) = prefs.edit().putString("phone", value).apply()

    var nationalCode: String
        get() = prefs.getString("national_code", "") ?: ""
        set(value) = prefs.edit().putString("national_code", value).apply()

    var currentWellId: String?
        get() = prefs.getString("current_well_id", null)
        set(value) = prefs.edit().putString("current_well_id", value).apply()
}
