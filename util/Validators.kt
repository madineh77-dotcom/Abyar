package com.example.abyar.util

/**
 * توابع اعتبارسنجی مشترک در سراسر اپلیکیشن
 */

/**
 * اعتبارسنجی شماره تماس
 * قانون: شروع با 0 + دقیقاً 11 رقم
 */
fun isValidIranianPhone(phone: String): Boolean {
    val cleaned = toEnglishDigits(phone).trim()
    return cleaned.length == 11 && cleaned.startsWith("0") && cleaned.all { it.isDigit() }
}

/**
 * پیام خطای شماره تماس
 */
fun phoneErrorMessage(phone: String): String? {
    val cleaned = toEnglishDigits(phone).trim()
    return when {
        cleaned.isEmpty() -> "شماره تماس الزامی است"
        !cleaned.all { it.isDigit() } -> "شماره تماس فقط باید عدد باشد"
        !cleaned.startsWith("0") -> "شماره تماس باید با 0 شروع شود"
        cleaned.length != 11 -> "شماره تماس باید دقیقاً 11 رقم باشد"
        else -> null
    }
}

/**
 * اعتبارسنجی کد ملی (10 رقم)
 */
fun isValidNationalCode(code: String): Boolean {
    val cleaned = toEnglishDigits(code).trim()
    return cleaned.length == 10 && cleaned.all { it.isDigit() }
}

/**
 * پیام خطای کد ملی
 */
fun nationalCodeErrorMessage(code: String): String? {
    val cleaned = toEnglishDigits(code).trim()
    return when {
        cleaned.isEmpty() -> "کد ملی الزامی است"
        !cleaned.all { it.isDigit() } -> "کد ملی فقط باید عدد باشد"
        cleaned.length != 10 -> "کد ملی باید دقیقاً 10 رقم باشد"
        else -> null
    }
}
