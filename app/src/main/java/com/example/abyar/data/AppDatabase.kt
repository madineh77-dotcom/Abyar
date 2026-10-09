package com.example.abyar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Well::class,
        User::class,
        IrrigationTurn::class,
        Transaction::class,
        NotificationGroup::class
    ],
    version = 3,   // نسخه جدید (چون ساختار عوض شد)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wellDao(): WellDao
    abstract fun userDao(): UserDao
    abstract fun turnDao(): TurnDao
    abstract fun transactionDao(): TransactionDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abyar_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
