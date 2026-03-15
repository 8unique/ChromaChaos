package com.chromachaos.game.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.chromachaos.game.data.model.GameSettings
import com.chromachaos.game.data.model.GameStats
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GameStats::class, GameSettings::class],
    version = 2,
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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default rows via raw SQL — avoids the
                            // INSTANCE race condition (INSTANCE is still null
                            // when onCreate fires during .build()).
                            db.execSQL(
                                "INSERT OR IGNORE INTO game_settings " +
                                "(id, gridWidth, gridHeight, enableSpecialBlocks, " +
                                "enableSound, enableVibration, difficulty, gameMode) " +
                                "VALUES (1, 12, 20, 1, 1, 1, 'NORMAL', 'CASUAL')"
                            )
                            db.execSQL(
                                "INSERT OR IGNORE INTO game_stats " +
                                "(id, highScore, totalGamesPlayed, totalLinesCleared, " +
                                "totalPlayTime, bestCombo) " +
                                "VALUES (1, 0, 0, 0, 0, 0)"
                            )
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Safety-net: ensure rows exist on every open
                            // (handles edge-cases like destructive migration).
                            db.execSQL(
                                "INSERT OR IGNORE INTO game_settings " +
                                "(id, gridWidth, gridHeight, enableSpecialBlocks, " +
                                "enableSound, enableVibration, difficulty, gameMode) " +
                                "VALUES (1, 12, 20, 1, 1, 1, 'NORMAL', 'CASUAL')"
                            )
                            db.execSQL(
                                "INSERT OR IGNORE INTO game_stats " +
                                "(id, highScore, totalGamesPlayed, totalLinesCleared, " +
                                "totalPlayTime, bestCombo) " +
                                "VALUES (1, 0, 0, 0, 0, 0)"
                            )
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}