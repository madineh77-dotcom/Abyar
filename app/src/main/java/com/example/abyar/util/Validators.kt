package com.example.abyar.util

fun isValidIranianPhone(phone: String): Boolean {
    val cleaned = toEnglishDigits(phone).trim()
    return cleaned.length == 11 && cleaned.startsWith("0") && cleaned.all { it.isDigit() }
}

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

fun isValidNationalCode(code: String): Boolean {
    val cleaned = toEnglishDigits(code).trim()
    return cleaned.length == 10 && cleaned.all { it.isDigit() }
}

fun nationalCodeErrorMessage(code: String): String? {
    val cleaned = toEnglishDigits(code).trim()
    return when {
        cleaned.isEmpty() -> "کد ملی الزامی است"
        !cleaned.all { it.isDigit() } -> "کد ملی فقط باید عدد باشد"
        cleaned.length != 10 -> "کد ملی باید دقیقاً 10 رقم باشد"
        else -> null
    }
}
