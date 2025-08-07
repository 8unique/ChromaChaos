package com.chromachaos.game.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_settings")
data class GameSettings(
    @PrimaryKey val id: Int = 1,
    val gridWidth: Int = 12,
    val gridHeight: Int = 20,
    val enableSpecialBlocks: Boolean = true,
    val enableSound: Boolean = true,
    val enableVibration: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL
)
