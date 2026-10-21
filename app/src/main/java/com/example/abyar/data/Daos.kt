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
    @Query("SELECT * FROM wells WHERE id = :wellId LIMIT 1")
    suspend fun getWellById(wellId: String): Well?
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
    @Query("SELECT * FROM users WHERE groupId = :groupId ORDER BY shareHours DESC")
    fun getUsersByGroup(groupId: String): Flow<List<User>>
    @Query("SELECT * FROM users WHERE groupId = :groupId ORDER BY shareHours DESC")
    suspend fun getUsersByGroupSync(groupId: String): List<User>
    @Query("SELECT * FROM users WHERE wellId = :wellId AND role = 'MAIN_ADMIN' LIMIT 1")
    suspend fun getMainAdmin(wellId: String): User?
    @Query("SELECT * FROM users WHERE wellId = :wellId AND role = 'OPERATOR' LIMIT 1")
    suspend fun getOperator(wellId: String): User?
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: OwnerGroup)
    @Update
    suspend fun updateGroup(group: OwnerGroup)
    @Query("SELECT * FROM owner_groups WHERE wellId = :wellId")
    fun getGroupsByWell(wellId: String): Flow<List<OwnerGroup>>
    @Query("SELECT * FROM owner_groups WHERE wellId = :wellId")
    suspend fun getGroupsByWellSync(wellId: String): List<OwnerGroup>
    @Query("SELECT * FROM owner_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): OwnerGroup?
    @Query("DELETE FROM owner_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)
    @Query("SELECT COUNT(*) FROM owner_groups WHERE wellId = :wellId")
    suspend fun countGroupsByWell(wellId: String): Int
}

@Dao
interface TurnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurns(turns: List<IrrigationTurn>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: IrrigationTurn)
    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId ORDER BY startTime ASC")
    fun getTurnsByWell(wellId: String): Flow<List<IrrigationTurn>>
    // این متد برای پردازش همزمان جابجایی اضافه شد
    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId ORDER BY startTime ASC")
    suspend fun getTurnsByWellSync(wellId: String): List<IrrigationTurn>
    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId AND userId = :userId ORDER BY startTime ASC")
    fun getTurnsByUser(wellId: String, userId: String): Flow<List<IrrigationTurn>>
    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId AND userId = :userId AND status = 'PENDING' ORDER BY startTime ASC LIMIT 1")
    fun getNextPendingTurn(wellId: String, userId: String): Flow<IrrigationTurn?>
    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId AND userId = :userId AND status = 'PENDING' ORDER BY startTime ASC LIMIT 1")
    suspend fun getNextPendingTurnSync(wellId: String, userId: String): IrrigationTurn?
    @Query("DELETE FROM irrigation_turns WHERE wellId = :wellId")
    suspend fun deleteTurnsByWell(wellId: String)
    @Query("DELETE FROM irrigation_turns WHERE id = :turnId")
    suspend fun deleteTurn(turnId: String)
    @Query("SELECT COUNT(*) FROM irrigation_turns WHERE wellId = :wellId")
    suspend fun countTurnsByWell(wellId: String): Int
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    @Query("SELECT * FROM transactions WHERE wellId = :wellId ORDER BY date DESC")
    fun getTransactionsByWell(wellId: String): Flow<List<Transaction>>
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)
    @Query("""
        SELECT * FROM notifications 
        WHERE wellId = :wellId 
        AND (recipientUserId = :userId OR (recipientGroupId IS NOT NULL AND recipientGroupId = :groupId))
        ORDER BY sentAt DESC LIMIT 20
    """)
    fun getNotificationsForUser(wellId: String, userId: String, groupId: String?): Flow<List<Notification>>
    @Query("""
        SELECT * FROM notifications 
        WHERE wellId = :wellId 
        AND (recipientUserId = :userId OR (recipientGroupId IS NOT NULL AND recipientGroupId = :groupId))
        ORDER BY sentAt DESC LIMIT 20
    """)
    suspend fun getNotificationsForUserSync(wellId: String, userId: String, groupId: String?): List<Notification>
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)
}
