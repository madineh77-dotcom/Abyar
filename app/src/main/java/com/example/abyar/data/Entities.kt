package com.example.abyar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WageType { CASH, FROM_TOTAL, FROM_OWN_SHARE }

enum class UserRole { MAIN_ADMIN, GROUP_LEADER, SMALL_OWNER, OPERATOR, ABYAR }

/**
 * چاه کشاورزی
 */
@Entity(tableName = "wells")
data class Well(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val location: String? = null,
    val totalHours: Double,
    val cycleDurationDays: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * گروه مالکان
 */
@Entity(tableName = "owner_groups")
data class OwnerGroup(
    @PrimaryKey val id: String,
    val wellId: String,
    val name: String,
    val leaderId: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * کاربر (با نقش ثابت)
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val wellId: String,
    val fullName: String,
    val phone: String,
    val nationalCode: String,
    val shareHours: Double = 0.0,
    val role: UserRole = UserRole.SMALL_OWNER,
    val operatorWageType: WageType? = null,
    val operatorWageAmount: Double = 0.0,
    val abyarWageType: WageType? = null,
    val abyarWageAmount: Double = 0.0,
    val groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val roleLabel: String
        get() = when (role) {
            UserRole.MAIN_ADMIN -> "مدیر اصلی"
            UserRole.GROUP_LEADER -> "سرگروه"
            UserRole.SMALL_OWNER -> "خرده‌مالک"
            UserRole.OPERATOR -> "موتوربان"
            UserRole.ABYAR -> "آبیار"
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
    val status: String
)

/**
 * تراکنش مالی
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val wellId: String,
    val type: String,
    val category: String,
    val amount: Long,
    val date: Long,
    val description: String? = null,
    val relatedUserId: String? = null
)

/**
 * پیام‌های اطلاع‌رسانی (جداگانه برای هر کاربر)
 */
@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String,
    val wellId: String,
    val senderId: String,
    val senderName: String,
    val recipientUserId: String?,   // برای پیام فردی
    val recipientGroupId: String?,  // برای پیام گروهی
    val text: String,
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
