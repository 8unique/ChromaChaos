package com.chromachaos.game.data.repository

import com.chromachaos.game.data.model.Block
import com.chromachaos.game.data.model.GameSettings
import com.chromachaos.game.data.model.GameStats
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun getGameSettings(): Flow<GameSettings>
    suspend fun updateGameSettings(settings: GameSettings)
    
    fun getGameStats(): Flow<GameStats>
    suspend fun updateGameStats(stats: GameStats)
    
    suspend fun saveHighScore(score: Int)
    suspend fun incrementGamesPlayed()
    suspend fun addLinesCleared(count: Int)
    suspend fun updateBestCombo(combo: Int)
    suspend fun addPlayTime(playTime: Long)
    
    fun generateRandomBlock(): Block
    fun generateSpecialBlock(): Block
} 