package com.chromachaos.game.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.chromachaos.game.data.model.GameSettings
import com.chromachaos.game.data.model.GameStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GameStats::class, GameSettings::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun settingsDao(): GameSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "chroma_chaos_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize database with default values
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val database = INSTANCE
                                    database?.let { db ->
                                        // Insert default settings
                                        db.settingsDao().insertSettings(GameSettings())
                                        // Insert default stats
                                        db.gameDao().insertStats(GameStats())
                                    }
                                } catch (e: Exception) {
                                    // Log error but don't crash the app
                                    e.printStackTrace()
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}