package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [PricePackageEntity::class, ActivityLogEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class JarvisDatabase : RoomDatabase() {
  abstract fun jarvisDao(): JarvisDao

  companion object {
    @Volatile private var INSTANCE: JarvisDatabase? = null

    fun getInstance(context: Context): JarvisDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                JarvisDatabase::class.java,
                "mec_chamet_jarvis.db",
              )
              .fallbackToDestructiveMigration(true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
