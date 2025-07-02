package com.eldercare.assistant.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eldercare.assistant.data.dao.FavoriteContactDao
import com.eldercare.assistant.data.entities.FavoriteContact

@Database(
    entities = [FavoriteContact::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun favoriteContactDao(): FavoriteContactDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "elder_care_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
