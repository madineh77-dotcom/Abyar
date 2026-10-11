package com.example.abyar.util

/**
 * تبدیل اعداد فارسی و عربی به انگلیسی
 * رفع باگ: کاربران ممکن است اعداد را فارسی وارد کنند (۱۲۳)
 */
fun toEnglishDigits(input: String): String {
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
    var result = input
    for (i in 0..9) {
        result = result.replace(persianDigits[i], ('0' + i))
        result = result.replace(arabicDigits[i], ('0' + i))
    }
    return result
}

/**
 * تبدیل String به Int با پشتیبانی از اعداد فارسی
 */
fun String.toIntSafe(): Int? = toEnglishDigits(this).trim().toIntOrNull()

/**
 * تبدیل String به Long با پشتیبانی از اعداد فارسی
 */
fun String.toLongSafe(): Long? = toEnglishDigits(this).trim().toLongOrNull()

/**
 * تبدیل String به Double با پشتیبانی از اعداد فارسی و اعشار
 */
fun String.toDoubleSafe(): Double? {
    val cleaned = toEnglishDigits(this).trim().replace("٫", ".").replace("،", "")
    return cleaned.toDoubleOrNull()
}
