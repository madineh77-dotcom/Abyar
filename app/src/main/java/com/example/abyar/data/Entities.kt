package com.example.abyar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WageType { CASH, FROM_TOTAL, FROM_OWN_SHARE }

// نقش آبیار حذف شد
enum class UserRole {
    MAIN_ADMIN,      // مدیر اصلی
    MAJOR_OWNER,     // مالک عمده
    SMALL_OWNER,     // خرده‌مالک
    OPERATOR         // موتوربان
}

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

@Entity(tableName = "owner_groups")
data class OwnerGroup(
    @PrimaryKey val id: String,
    val wellId: String,
    val name: String,
    val leaderId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val wellId: String,
    val fullName: String,
    val phone: String,
    val nationalCode: String,
    val shareHours: Double = 0.0,
    val role: UserRole = UserRole.SMALL_OWNER,
    val isGroupLeader: Boolean = false,
    val operatorWageType: WageType? = null,
    val operatorWageAmount: Double = 0.0,
    val includeWageInSchedule: Boolean = true, // آیا دستمزد در برنامه آبیاری لحاظ شود؟
    val groupId: String? = null,               // به کدام سرگروه متصل است
    val createdAt: Long = System.currentTimeMillis()
) {
    val roleLabel: String
        get() = when (role) {
            UserRole.MAIN_ADMIN -> "مدیر اصلی"
            UserRole.MAJOR_OWNER -> "مالک عمده"
            UserRole.SMALL_OWNER -> "خرده‌مالک"
            UserRole.OPERATOR -> "موتوربان"
        }
}

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

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String,
    val wellId: String,
    val senderId: String,
    val senderName: String,
    val recipientUserId: String?,
    val recipientGroupId: String?,
    val text: String,
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
