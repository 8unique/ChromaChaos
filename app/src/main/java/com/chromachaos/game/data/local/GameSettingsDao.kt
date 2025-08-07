package com.chromachaos.game.data.local

import androidx.room.*
import com.chromachaos.game.data.model.GameSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSettingsDao {
    @Query("SELECT * FROM game_settings LIMIT 1")
    fun getSettings(): Flow<GameSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: GameSettings)

    @Update
    suspend fun updateSettings(settings: GameSettings)
} 