package com.kafappstore.ciphervault.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CipherProject::class], version = 1, exportSchema = false)
abstract class CipherVaultDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: CipherVaultDatabase? = null

        fun getDatabase(context: Context): CipherVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CipherVaultDatabase::class.java,
                    "ciphervault_projects.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
