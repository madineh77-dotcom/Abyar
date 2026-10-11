package com.example.abyar.util

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ==================== میلادی → شمسی ====================

fun formatPersianDateTime(timestamp: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), Locale.US)
    cal.time = Date(timestamp)
    val gy = cal.get(Calendar.YEAR)
    val gm = cal.get(Calendar.MONTH) + 1
    val gd = cal.get(Calendar.DAY_OF_MONTH)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)

    val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
    return String.format(Locale.US, "%04d/%02d/%02d - %02d:%02d", jy, jm, jd, hour, minute)
}

fun formatPersianDate(timestamp: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), Locale.US)
    cal.time = Date(timestamp)
    val gy = cal.get(Calendar.YEAR)
    val gm = cal.get(Calendar.MONTH) + 1
    val gd = cal.get(Calendar.DAY_OF_MONTH)

    val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
    return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
}

fun formatPersianTime(timestamp: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), Locale.US)
    cal.time = Date(timestamp)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

fun persianMonthName(month: Int): String = when (month) {
    1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"
    4 -> "تیر"; 5 -> "مرداد"; 6 -> "شهریور"
    7 -> "مهر"; 8 -> "آبان"; 9 -> "آذر"
    10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"
    else -> ""
}

/**
 * دریافت تاریخ امروز به صورت جلالی
 */
fun getTodayJalali(): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), Locale.US)
    return gregorianToJalali(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

// ==================== شمسی → میلادی (تبدیل به timestamp) ====================

/**
 * تبدیل تاریخ شمسی به timestamp (میلی‌ثانیه)
 * ساعت به صورت پیش‌فرض 00:00 در نظر گرفته می‌شود
 */
fun jalaliToTimestamp(jy: Int, jm: Int, jd: Int, hour: Int = 0, minute: Int = 0): Long {
    val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"), Locale.US)
    cal.set(Calendar.YEAR, gy)
    cal.set(Calendar.MONTH, gm - 1)
    cal.set(Calendar.DAY_OF_MONTH, gd)
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ==================== الگوریتم‌های تبدیل ====================

private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth.take(gm - 1).sum()

    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + (days / 31)
        jd = 1 + (days % 31)
    } else {
        jm = 7 + ((days - 186) / 30)
        jd = 1 + ((days - 186) % 30)
    }
    return Triple(jy, jm, jd)
}

private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
    val jy2 = jy - 979
    val jm2 = jm - 1
    val jd2 = jd - 1
    var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4)
    for (i in 0 until jm2) {
        jDayNo += if (i < 6) 31 else 30
    }
    jDayNo += jd2

    var gDayNo = jDayNo + 79
    var gy = 1600 + 400 * (gDayNo / 146097)
    gDayNo %= 146097
    var leap = true
    if (gDayNo >= 36525) {
        gDayNo--
        gy += 100 * (gDayNo / 36524)
        gDayNo %= 36524
        if (gDayNo >= 365) gDayNo++
        else leap = false
    }
    gy += 4 * (gDayNo / 1461)
    gDayNo %= 1461
    if (gDayNo >= 366) {
        leap = false
        gDayNo--
        gy += gDayNo / 365
        gDayNo %= 365
    }
    var gd = gDayNo + 1
    val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gm = 0
    while (gm < 12 && gd > gDaysInMonth[gm]) {
        gd -= gDaysInMonth[gm]
        gm++
    }
    return Triple(gy, gm + 1, gd)
}
