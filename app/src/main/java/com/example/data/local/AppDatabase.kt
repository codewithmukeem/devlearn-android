package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CertificateDao
import com.example.data.local.dao.LabDao
import com.example.data.local.dao.ProfileDao
import com.example.data.local.dao.ProgressDao
import com.example.data.local.entity.CertificateEntity
import com.example.data.local.entity.LabSnippetEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.UserProgressEntity

@Database(
    entities = [
        UserProgressEntity::class,
        UserProfileEntity::class,
        CertificateEntity::class,
        LabSnippetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun profileDao(): ProfileDao
    abstract fun certificateDao(): CertificateDao
    abstract fun labDao(): LabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "devlearn_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
