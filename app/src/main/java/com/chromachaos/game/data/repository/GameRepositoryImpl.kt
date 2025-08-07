package com.chromachaos.game.data.repository

import com.chromachaos.game.data.model.*
import com.chromachaos.game.data.local.GameDao
import com.chromachaos.game.data.local.GameSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val gameDao: GameDao,
    private val settingsDao: GameSettingsDao
) : GameRepository {

    override fun getGameSettings(): Flow<GameSettings> =
        settingsDao.getSettings().map { it ?: GameSettings() }

    override suspend fun updateGameSettings(settings: GameSettings) {
        settingsDao.updateSettings(settings)
    }

    override fun getGameStats(): Flow<GameStats> =
        gameDao.getStats().map { it ?: GameStats() }

    override suspend fun updateGameStats(stats: GameStats) {
        gameDao.updateStats(stats)
    }

    override suspend fun saveHighScore(score: Int) {
        gameDao.updateHighScore(score)
    }

    override suspend fun incrementGamesPlayed() {
        gameDao.incrementGamesPlayed()
    }

    override suspend fun addLinesCleared(count: Int) {
        gameDao.addLinesCleared(count)
    }

        override suspend fun updateBestCombo(combo: Int) {
        gameDao.updateBestCombo(combo)
    }
    
    override suspend fun addPlayTime(playTime: Long) {
        gameDao.addPlayTime(playTime)
    }
    
    override fun generateRandomBlock(): Block {
        val random = Random()
        val shapes = BlockShape.values()
        val colors = BlockColors.allColors

        val shape = shapes[random.nextInt(shapes.size)]
        val color = colors[random.nextInt(colors.size)]

        return Block(
            id = UUID.randomUUID().toString(),
            color = color,
            shape = shape,
            isSpecial = false
        )
    }

    override fun generateSpecialBlock(): Block {
        val random = Random()
        val specialTypes = SpecialBlockType.values()
        val colors = BlockColors.allColors
        val shapes = BlockShape.values()

        val specialType = specialTypes[random.nextInt(specialTypes.size)]
        val color = colors[random.nextInt(colors.size)]
        val shape = shapes[random.nextInt(shapes.size)]

        return Block(
            id = UUID.randomUUID().toString(),
            color = color,
            shape = shape,
            isSpecial = true,
            specialType = specialType
        )
    }
} 