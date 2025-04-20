package com.forge.chromachaos.model

data class GameState(
    val grid: List<List<GameCell>>,
    val currentBlock: Block? = null,
    val score: Int = 0,
    val isGameOver: Boolean = false
)