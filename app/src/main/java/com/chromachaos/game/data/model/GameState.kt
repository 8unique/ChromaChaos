package com.chromachaos.game.data.model

import androidx.compose.ui.graphics.Color

data class GameState(
    val grid: List<List<GridCell>> = emptyList(),
    val currentBlock: Block? = null,
    val nextBlock: Block? = null,
    val score: Int = 0,
    val level: Int = 1,
    val linesCleared: Int = 0,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val gameSpeed: Long = 1000L,
    val combo: Int = 0,
    val chainCount: Int = 0
)

data class GridCell(
    val color: Color? = null,
    val isSpecial: Boolean = false,
    val specialType: SpecialBlockType? = null
) {
    companion object {
        val Empty = GridCell()
    }
}

enum class Difficulty {
    EASY, NORMAL, HARD, EXPERT
}