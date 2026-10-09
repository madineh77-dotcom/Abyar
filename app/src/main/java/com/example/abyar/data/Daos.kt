package com.example.abyar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WellDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWell(well: Well)

    @Update
    suspend fun updateWell(well: Well)

    @Query("SELECT * FROM wells LIMIT 1")
    fun getFirstWell(): Flow<Well?>

    @Query("SELECT * FROM wells LIMIT 1")
    suspend fun getFirstWellSync(): Well?

    @Query("SELECT * FROM wells")
    fun getAllWells(): Flow<List<Well>>

    @Query("SELECT * FROM wells WHERE code = :code LIMIT 1")
    suspend fun getWellByCode(code: String): Well?

    @Query("SELECT COUNT(*) FROM wells WHERE code = :code")
    suspend fun countByCode(code: String): Int
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE wellId = :wellId ORDER BY shareHours DESC")
    fun getUsersByWell(wellId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE wellId = :wellId ORDER BY shareHours DESC")
    suspend fun getUsersByWellSync(wellId: String): List<User>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE wellId = :wellId AND nationalCode = :nationalCode LIMIT 1")
    suspend fun getUserByNationalCode(wellId: String, nationalCode: String): User?

    @Query("SELECT * FROM users WHERE nationalCode = :nationalCode")
    suspend fun getAllByNationalCode(nationalCode: String): List<User>

    @Query("SELECT SUM(shareHours) FROM users WHERE wellId = :wellId")
    suspend fun getTotalShareHours(wellId: String): Double?

    @Query("SELECT * FROM users WHERE wellId = :wellId AND groupLeaderId = :leaderId")
    fun getGroupMembers(wellId: String, leaderId: String): Flow<List<User>>

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface TurnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurns(turns: List<IrrigationTurn>)

    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId ORDER BY startTime ASC")
    fun getTurnsByWell(wellId: String): Flow<List<IrrigationTurn>>

    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId AND userId = :userId ORDER BY startTime ASC")
    fun getTurnsByUser(wellId: String, userId: String): Flow<List<IrrigationTurn>>

    @Query("DELETE FROM irrigation_turns WHERE wellId = :wellId")
    suspend fun deleteTurnsByWell(wellId: String)
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE wellId = :wellId ORDER BY date DESC")
    fun getTransactionsByWell(wellId: String): Flow<List<Transaction>>
}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: NotificationGroup)

    @Query("SELECT * FROM notification_groups WHERE wellId = :wellId")
    fun getGroupsByWell(wellId: String): Flow<List<NotificationGroup>>
}
