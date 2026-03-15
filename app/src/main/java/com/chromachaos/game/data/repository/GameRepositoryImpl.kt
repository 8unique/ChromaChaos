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
        // updateSettings is now @Insert(REPLACE) → true upsert
        settingsDao.updateSettings(settings)
    }

    override fun getGameStats(): Flow<GameStats> =
        gameDao.getStats().map { it ?: GameStats() }

    override suspend fun updateGameStats(stats: GameStats) {
        gameDao.updateStats(stats)
    }

    /**
     * Guarantees a stats row exists before running UPDATE queries.
     * Uses IGNORE strategy so it is a no-op when the row already exists.
     */
    private suspend fun ensureStatsRow() {
        gameDao.ensureStatsExist(GameStats())
    }

    override suspend fun saveHighScore(score: Int) {
        ensureStatsRow()
        gameDao.updateHighScore(score)
    }

    override suspend fun incrementGamesPlayed() {
        ensureStatsRow()
        gameDao.incrementGamesPlayed()
    }

    override suspend fun addLinesCleared(count: Int) {
        ensureStatsRow()
        gameDao.addLinesCleared(count)
    }

    override suspend fun updateBestCombo(combo: Int) {
        ensureStatsRow()
        gameDao.updateBestCombo(combo)
    }

    override suspend fun addPlayTime(playTime: Long) {
        ensureStatsRow()
        gameDao.addPlayTime(playTime)
    }
    
    override fun generateRandomBlock(): Block {
        val random = Random()
        // Exclude I-shape: a 4-block straight line self-clears immediately
        val shapes = BlockShape.entries.filter { it != BlockShape.I }
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
        // Weighted selection: WILD ~4%, CROSS_CLEAR ~2%, AREA_EXPLOSION ~3%
        // Normalize: 4+2+3 = 9 → pick within 9
        val roll = random.nextInt(9)
        val specialType = when {
            roll < 4 -> SpecialBlockType.WILD
            roll < 6 -> SpecialBlockType.CROSS_CLEAR
            else     -> SpecialBlockType.AREA_EXPLOSION
        }

        val color = when (specialType) {
            SpecialBlockType.WILD           -> BlockColors.RAINBOW
            SpecialBlockType.CROSS_CLEAR    -> BlockColors.CROSS_CLEAR
            SpecialBlockType.AREA_EXPLOSION -> BlockColors.AREA_EXPLOSION
        }

        // Special blocks always spawn as a single-cell DOT shape
        return Block(
            id = UUID.randomUUID().toString(),
            color = color,
            shape = BlockShape.DOT,
            isSpecial = true,
            specialType = specialType
        )
    }
} 