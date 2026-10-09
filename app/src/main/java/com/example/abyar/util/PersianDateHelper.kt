package com.example.abyar.util

import android.icu.util.PersianCalendar
import java.util.Date
import java.util.Locale
import com.ibm.icu.util.PersianCalendar

fun formatPersianDateTime(timestamp: Long): String {
    val pc = PersianCalendar()
    pc.time = Date(timestamp)
    val year = pc.get(PersianCalendar.YEAR)
    val month = pc.get(PersianCalendar.MONTH) + 1
    val day = pc.get(PersianCalendar.DAY_OF_MONTH)
    val hour = pc.get(PersianCalendar.HOUR_OF_DAY)
    val minute = pc.get(PersianCalendar.MINUTE)
    return String.format(Locale.US, "%04d/%02d/%02d - %02d:%02d", year, month, day, hour, minute)
}

fun formatPersianDate(timestamp: Long): String {
    val pc = PersianCalendar()
    pc.time = Date(timestamp)
    val year = pc.get(PersianCalendar.YEAR)
    val month = pc.get(PersianCalendar.MONTH) + 1
    val day = pc.get(PersianCalendar.DAY_OF_MONTH)
    return String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
}

fun persianMonthName(month: Int): String {
    return when (month) {
        1 -> "فروردین"; 2 -> "اردیبهشت"; 3 -> "خرداد"
        4 -> "تیر"; 5 -> "مرداد"; 6 -> "شهریور"
        7 -> "مهر"; 8 -> "آبان"; 9 -> "آذر"
        10 -> "دی"; 11 -> "بهمن"; 12 -> "اسفند"
        else -> ""
    }
}
