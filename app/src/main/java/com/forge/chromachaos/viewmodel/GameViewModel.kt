package com.forge.chromachaos.viewmodel

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.chromachaos.model.GameCell
import com.forge.chromachaos.model.GameState
import com.forge.chromachaos.util.BlockGenerator
import com.forge.chromachaos.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.compose.ui.tooling.preview.Preview
import com.forge.chromachaos.ui.theme.screen.GameScreen

class GameViewModel : ViewModel() {

    private val TAG = "GameViewModel"
    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState

    init {
        spawnNewBlock()
        startBlockFallLoop()
    }

    private fun createInitialGameState(): GameState {
        val emptyGrid = List(23) {
            List(17) { GameCell() }
        }
        return GameState(grid = emptyGrid)
    }

    private fun spawnNewBlock() {
        val block = BlockGenerator.generateRandomBlock()
        val shapeWidth = block.shape.maxOf { it.second }
        val centerCol = (10 - shapeWidth) / 2
        val positionedBlock = block.copy(position = Pair(0, centerCol))
        _gameState.value = _gameState.value.copy(currentBlock = positionedBlock)
    }

    private fun mergeBlockToGrid(block: Block) {
        val newGrid = _gameState.value.grid.map { it.toMutableList() }

        block.shape.forEach { (r, c) ->
            val row = block.position.first + r
            val col = block.position.second + c
            if (row in newGrid.indices && col in newGrid[0].indices) {
                newGrid[row][col] = GameCell(isOccupied = true, color = block.color)
            }
        }

        _gameState.value = _gameState.value.copy(grid = newGrid)
    }

    private fun clearFullLines() {
        val grid = _gameState.value.grid.toMutableList()
        val newGrid = mutableListOf<List<GameCell>>()
        var linesCleared = 0

        for (row in grid) {
            if (row.all { it.isOccupied }) {
                linesCleared++
            } else {
                newGrid.add(row)
            }
        }

        // Add empty rows at the top to maintain grid size
        repeat(linesCleared) {
            newGrid.add(0, List(10) { GameCell() })
        }

        val newScore = _gameState.value.score + when (linesCleared) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800
            else -> 0
        }

        _gameState.value = _gameState.value.copy(
            grid = newGrid,
            score = newScore
        )
    }

    private fun spawnNewBlockOrGameOver() {
        val newBlock = BlockGenerator.generateRandomBlock()
        val shapeWidth = newBlock.shape.maxOf { it.second }
        val centerCol = (10 - shapeWidth) / 2
        val startPos = Pair(0, centerCol)
        val positionedBlock = newBlock.copy(position = startPos)

        if (canMove(positionedBlock, startPos)) {
            _gameState.value = _gameState.value.copy(currentBlock = positionedBlock)
        } else {
            // 💀 Game over
            _gameState.value = _gameState.value.copy(currentBlock = null)
        }
    }


    private fun startBlockFallLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(500L)
                val block = _gameState.value.currentBlock
                if (block != null) {
                    val newPos = block.position.copy(first = block.position.first + 1)
                    val movedBlock = block.copy(position = newPos)
                    if (canMove(movedBlock, newPos)) {
                        _gameState.value = _gameState.value.copy(currentBlock = movedBlock)
                    } else {
                        //  Collision! Merge + Clear + Respawn
                        mergeBlockToGrid(block)
                        clearFullLines()
                        spawnNewBlockOrGameOver()
                    }
                }
            }
        }
    }


    fun moveBlockLeft() = moveBlockBy(0, -1)
    fun moveBlockRight() = moveBlockBy(0, 1)
    fun moveBlockDown() = moveBlockBy(1, 0)

    private fun moveBlockBy(rowOffset: Int, colOffset: Int) {
        val state = _gameState.value
        val block = state.currentBlock ?: return
        val newPos = block.position.copy(
            first = block.position.first + rowOffset,
            second = block.position.second + colOffset
        )

        if (canMove(block, newPos)) {
            val movedBlock = block.copy(position = newPos)
            _gameState.value = state.copy(currentBlock = movedBlock)
        } else if (rowOffset == 1) {
            // Block hit bottom – stop movement
            // TODO: Merge into grid in Phase 4
            spawnNewBlock()
        }

        Log.d(TAG, "Moving to row=${newPos.first}, col=${newPos.second}")
    }

    private fun canMove(block: Block, newPosition: Pair<Int, Int>): Boolean {
        val (rowOffset, colOffset) = newPosition
        val grid = _gameState.value.grid

        return block.shape.all { (r, c) ->
            val newRow = rowOffset + r
            val newCol = colOffset + c
            newRow in grid.indices &&
                    newCol in grid[0].indices &&
                    !grid[newRow][newCol].isOccupied
        }
    }


}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GameScreenPreview() {
    // Provide a fake ViewModel or mocked state for preview
    val dummyViewModel = GameViewModel().apply {
        // Set up a simple mock state if needed
    }

    GameScreen(viewModel = dummyViewModel)
}