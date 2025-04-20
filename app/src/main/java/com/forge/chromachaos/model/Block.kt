package com.forge.chromachaos.model

import androidx.compose.ui.graphics.Color

data class Block(
    val id: Int,
    val shape: List<Pair<Int, Int>>,
    val color: Color,
    val position: Pair<Int, Int> = Pair(0, 4) // Row 0, center column (4 if 10x10 grid)
)
