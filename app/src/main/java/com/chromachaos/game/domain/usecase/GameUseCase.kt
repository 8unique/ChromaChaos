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

    /**
     * Color-line scoring: 10 points per cleared block.
     */
    fun calculateColorLineScore(blocksCleared: Int): Int {
        return blocksCleared * 10
    }

    /**
     * Combo multiplier for chain reactions.
     * Combo 1 = x1.0, 2 = x1.5, 3 = x2.0, 4 = x2.5, 5+ = x3.0
     */
    fun getComboMultiplier(comboStep: Int): Double {
        return when {
            comboStep <= 1 -> 1.0
            comboStep == 2 -> 1.5
            comboStep == 3 -> 2.0
            comboStep == 4 -> 2.5
            else -> 3.0
        }
    }

    fun calculateLevel(linesCleared: Int): Int {
        return (linesCleared / 10) + 1
    }

    fun calculateGameSpeed(level: Int): Long {
        return maxOf(100L, 1000L - (level * 50L))
    }
} 