package com.forge.chromachaos.model

import androidx.compose.ui.graphics.Color

data class GameCell(
    val isOccupied: Boolean = false,
    val color: Color = Color.Transparent
)