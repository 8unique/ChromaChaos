package com.forge.chromachaos.util

import androidx.compose.ui.graphics.Color
import com.forge.chromachaos.model.Block
import kotlin.random.Random

object BlockGenerator {

    private val shapes = listOf(
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0)), // I
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)), // O
        listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, 2)), // T
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(2, 1)), // L
        listOf(Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(2, 0)), // J
        listOf(Pair(0, 1), Pair(1, 1), Pair(1, 0), Pair(2, 0)), // Z
        listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(2, 1))  // S
    )

    private val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta
    )

    fun generateRandomBlock(): Block {
        val id = Random.nextInt()
        val shape = shapes.random()
        val color = colors.random()
        return Block(id = id, shape = shape, color = color)
    }
}
