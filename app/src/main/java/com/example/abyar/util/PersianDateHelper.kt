package com.example.abyar.util

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * تبدیل زمان میلادی به تاریخ و ساعت شمسی
 * از الگوریتم تبدیل تقویم جلالی استفاده می‌کند
 */
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

fun persianMonthName(month: Int): String = when (month) {
    1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"
    4 -> "تیر"; 5 -> "مرداد"; 6 -> "شهریور"
    7 -> "مهر"; 8 -> "آبان"; 9 -> "آذر"
    10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"
    else -> ""
}

/**
 * تبدیل تاریخ میلادی به جلالی
 * الگوریتم استاندارد و بدون وابستگی به API های اندروید
 */
private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    var gy2 = if (gm > 2) gy + 1 else gy
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
