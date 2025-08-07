package com.chromachaos.game.data.model

import androidx.compose.ui.graphics.Color

data class Block(
    val id: String = "",
    val color: Color,
    val position: Position = Position(0, 0),
    val shape: BlockShape = BlockShape.I,
    val rotation: Int = 0,
    val isSpecial: Boolean = false,
    val specialType: SpecialBlockType? = null
) {
    fun getRotatedShape(): List<List<Boolean>> {
        return when (rotation % 360) {
            0 -> shape.blocks
            90 -> rotateMatrix90(shape.blocks)
            180 -> rotateMatrix180(shape.blocks)
            270 -> rotateMatrix270(shape.blocks)
            else -> shape.blocks
        }
    }
    
    private fun rotateMatrix90(matrix: List<List<Boolean>>): List<List<Boolean>> {
        val rows = matrix.size
        val cols = matrix[0].size
        return List(cols) { col ->
            List(rows) { row ->
                matrix[rows - 1 - row][col]
            }
        }
    }
    
    private fun rotateMatrix180(matrix: List<List<Boolean>>): List<List<Boolean>> {
        return matrix.reversed().map { it.reversed() }
    }
    
    private fun rotateMatrix270(matrix: List<List<Boolean>>): List<List<Boolean>> {
        val rows = matrix.size
        val cols = matrix[0].size
        return List(cols) { col ->
            List(rows) { row ->
                matrix[row][cols - 1 - col]
            }
        }
    }
}

data class Position(
    val x: Int,
    val y: Int
)

enum class BlockShape(val blocks: List<List<Boolean>>) {
    I(listOf(
        listOf(true, true, true, true)
    )),
    O(listOf(
        listOf(true, true),
        listOf(true, true)
    )),
    T(listOf(
        listOf(false, true, false),
        listOf(true, true, true)
    )),
    S(listOf(
        listOf(false, true, true),
        listOf(true, true, false)
    )),
    Z(listOf(
        listOf(true, true, false),
        listOf(false, true, true)
    )),
    J(listOf(
        listOf(true, false, false),
        listOf(true, true, true)
    )),
    L(listOf(
        listOf(false, false, true),
        listOf(true, true, true)
    ));
    
    fun rotate90Degrees(): List<List<Boolean>> {
        val original = this.blocks
        val rows = original.size
        val cols = original[0].size
        
        return List(cols) { col ->
            List(rows) { row ->
                original[rows - 1 - row][col]
            }
        }
    }
}

enum class SpecialBlockType {
    BOMB,
    COLOR_CLEAR,
    LINE_CLEAR,
    SCORE_MULTIPLIER,
    WILDCARD,
    PERSISTENT,
    SHAPE_SHIFTING
}

object BlockColors {
    val RED = Color(0xFFE53935)
    val BLUE = Color(0xFF2196F3)
    val GREEN = Color(0xFF4CAF50)
    val YELLOW = Color(0xFFFFEB3B)
    val PURPLE = Color(0xFF9C27B0)
    val ORANGE = Color(0xFFFF9800)
    val CYAN = Color(0xFF00BCD4)
    
    val allColors = listOf(RED, BLUE, GREEN, YELLOW, PURPLE, ORANGE, CYAN)
} 