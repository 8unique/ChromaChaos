package com.chromachaos.game.domain.usecase

import com.chromachaos.game.data.model.*
import com.chromachaos.game.data.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    fun getGameSettings(): Flow<GameSettings> = repository.getGameSettings()
    
    suspend fun updateGameSettings(settings: GameSettings) {
        repository.updateGameSettings(settings)
    }
    
    fun getGameStats(): Flow<GameStats> = repository.getGameStats()
    
    suspend fun saveHighScore(score: Int) {
        repository.saveHighScore(score)
    }
    
    suspend fun incrementGamesPlayed() {
        repository.incrementGamesPlayed()
    }
    
    suspend fun addLinesCleared(count: Int) {
        repository.addLinesCleared(count)
    }
    
    suspend fun updateBestCombo(combo: Int) {
        repository.updateBestCombo(combo)
    }
    
    suspend fun addPlayTime(playTime: Long) {
        repository.addPlayTime(playTime)
    }
    
    fun generateRandomBlock(): Block = repository.generateRandomBlock()
    
    fun generateSpecialBlock(): Block = repository.generateSpecialBlock()
    
    fun calculateScore(linesCleared: Int, combo: Int): Int {
        return when (linesCleared) {
            1 -> 100 * (combo + 1)
            2 -> 300 * (combo + 1)
            3 -> 500 * (combo + 1)
            4 -> 800 * (combo + 1)
            else -> 0
        }
    }
    
    fun calculateLevel(linesCleared: Int): Int {
        return (linesCleared / 10) + 1
    }
    
    fun calculateGameSpeed(level: Int): Long {
        return maxOf(100L, 1000L - (level * 50L))
    }
} 