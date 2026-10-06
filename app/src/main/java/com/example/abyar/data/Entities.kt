package com.example.abyar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { SUPER_ADMIN, MAJOR_OWNER, SMALL_OWNER, OPERATOR }

@Entity(tableName = "wells")
data class Well(
    @PrimaryKey val id: String,
    val name: String,
    val totalShares: Int,
    val cycleDurationDays: Int,
    val shareType: String
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val wellId: String,
    val fullName: String,
    val phone: String,
    val role: UserRole,
    val parentId: String? = null,
    val shareHours: Double? = null,
    val sharePercentage: Double? = null
)

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
    val type: String,
    val category: String,
    val amount: Long,
    val date: Long,
    val description: String?
)
