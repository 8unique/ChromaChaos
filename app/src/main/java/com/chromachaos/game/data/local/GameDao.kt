package com.chromachaos.game.data.local

import androidx.room.*
import com.chromachaos.game.data.model.GameStats
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_stats LIMIT 1")
    fun getStats(): Flow<GameStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: GameStats)

    @Update
    suspend fun updateStats(stats: GameStats)

    @Query("UPDATE game_stats SET highScore = :score WHERE highScore < :score")
    suspend fun updateHighScore(score: Int)

    @Query("UPDATE game_stats SET totalGamesPlayed = totalGamesPlayed + 1")
    suspend fun incrementGamesPlayed()

    @Query("UPDATE game_stats SET totalLinesCleared = totalLinesCleared + :count")
    suspend fun addLinesCleared(count: Int)

    @Query("UPDATE game_stats SET bestCombo = :combo WHERE bestCombo < :combo")
    suspend fun updateBestCombo(combo: Int)
    
    @Query("UPDATE game_stats SET totalPlayTime = totalPlayTime + :playTime")
    suspend fun addPlayTime(playTime: Long)
} 