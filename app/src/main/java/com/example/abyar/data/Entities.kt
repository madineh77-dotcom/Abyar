package com.example.abyar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WageType { CASH, FROM_TOTAL, FROM_OWN_SHARE }

/**
 * چاه کشاورزی
 */
@Entity(tableName = "wells")
data class Well(
    @PrimaryKey val id: String,
    val code: String,               // کد ۸ رقمی یکتای چاه
    val name: String,
    val location: String? = null,
    val totalHours: Double,         // کل ساعت آب
    val cycleDurationDays: Int,     // مدت مدار (روز)
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * گروه مالکان (زیر یک چاه)
 * هر گروه یک سرگروه دارد و چند عضو
 */
@Entity(tableName = "owner_groups")
data class OwnerGroup(
    @PrimaryKey val id: String,
    val wellId: String,
    val name: String,               // نام گروه (مثلاً «گروه آقای احمدی»)
    val leaderId: String,           // شناسه سرگروه (کلید خارجی به User.id)
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * کاربر (مالک، سرگروه، موتوربان، آبیار، مدیر)
 */
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
    val groupId: String? = null,     // شناسه گروه (اگر در گروهی است)
    val createdAt: Long = System.currentTimeMillis()
) {
    val roleLabel: String
        get() = when {
            shareHours > 5.0 -> "مالک عمده"
            shareHours > 0.0 -> "خرده‌مالک"
            isOperator -> "موتوربان"
            isAbyar -> "آبیار"
            else -> "کاربر"
        }
}

/**
 * نوبت آبیاری
 */
@Entity(tableName = "irrigation_turns")
data class IrrigationTurn(
    @PrimaryKey val id: String,
    val wellId: String,
    val userId: String,
    val userName: String,
    val startTime: Long,
    val endTime: Long,
    val status: String              // PENDING, COMPLETED, CANCELLED
)

/**
 * تراکنش مالی
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val wellId: String,
    val type: String,               // INCOME or EXPENSE
    val category: String,
    val amount: Long,
    val date: Long,
    val description: String? = null,
    val relatedUserId: String? = null
)

/**
 * لاگ اطلاع‌رسانی (ثبت هر پیامک ارسالی)
 */
@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey val id: String,
    val wellId: String,
    val senderId: String,           // چه کسی فرستاد
    val targetType: String,         // ALL_LEADERS, ONE_GROUP, ONE_USER, ALL_USERS
    val targetId: String?,          // شناسه گروه یا کاربر (اگر ONE_GROUP یا ONE_USER)
    val text: String,
    val sentAt: Long = System.currentTimeMillis()
)
