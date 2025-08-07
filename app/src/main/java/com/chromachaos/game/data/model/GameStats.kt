package com.chromachaos.game.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalLinesCleared: Int = 0,
    val totalPlayTime: Long = 0L,
    val bestCombo: Int = 0
) 