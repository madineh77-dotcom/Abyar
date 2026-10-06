package com.example.abyar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WellDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWell(well: Well)

    @Query("SELECT * FROM wells LIMIT 1")
    fun getFirstWell(): Flow<Well?>
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE wellId = :wellId")
    fun getUsersByWell(wellId: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE wellId = :wellId AND role != 'OPERATOR'")
    fun getOwnersByWell(wellId: String): Flow<List<User>>
}

@Dao
interface TurnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurns(turns: List<IrrigationTurn>)

    @Query("SELECT * FROM irrigation_turns WHERE wellId = :wellId ORDER BY startTime ASC")
    fun getTurnsByWell(wellId: String): Flow<List<IrrigationTurn>>

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
