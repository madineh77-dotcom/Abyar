package com.example.abyar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WageType { CASH, FROM_TOTAL, FROM_OWN_SHARE }

@Entity(tableName = "wells")
data class Well(
    @PrimaryKey val id: String,
    val code: String,               // کد ۸ رقمی یکتای چاه
    val name: String,
    val location: String? = null,
    val totalHours: Double,         // کل ساعت آب (مثلاً 240.0)
    val cycleDurationDays: Int,     // مدت مدار (روز)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val wellId: String,
    val fullName: String,
    val phone: String,
    val nationalCode: String,        // کد ملی ۱۰ رقمی
    val shareHours: Double = 0.0,
    val isOperator: Boolean = false,
    val isAbyar: Boolean = false,
    val operatorWageType: WageType? = null,
    val operatorWageAmount: Double = 0.0,
    val abyarWageType: WageType? = null,
    val abyarWageAmount: Double = 0.0,
    val groupLeaderId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // نقش خودکار بر اساس سهم
    val roleLabel: String
        get() = when {
            shareHours > 5.0 -> "مالک عمده"
            shareHours > 0.0 -> "خرده‌مالک"
            isOperator -> "موتوربان"
            isAbyar -> "آبیار"
            else -> "کاربر"
        }
}

@Entity(tableName = "irrigation_turns")
data class IrrigationTurn(
    @PrimaryKey val id: String,
    val wellId: String,
    val userId: String,
    val userName: String,
    val startTime: Long,
    val endTime: Long,
    val status: String
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val wellId: String,
    val type: String,       // INCOME یا EXPENSE
    val category: String,
    val amount: Long,
    val date: Long,
    val description: String? = null,
    val relatedUserId: String? = null
)

@Entity(tableName = "notification_groups")
data class NotificationGroup(
    @PrimaryKey val id: String,
    val wellId: String,
    val name: String,
    val leaderId: String,
    val memberIds: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
