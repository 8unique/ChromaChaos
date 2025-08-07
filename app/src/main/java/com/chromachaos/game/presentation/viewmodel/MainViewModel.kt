package com.chromachaos.game.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chromachaos.game.data.model.*
import com.chromachaos.game.domain.usecase.GameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val gameUseCase: GameUseCase
) : ViewModel() {
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val _gameSettings = MutableStateFlow(GameSettings())
    val gameSettings: StateFlow<GameSettings> = _gameSettings.asStateFlow()
    
    private val _gameStats = MutableStateFlow(GameStats())
    val gameStats: StateFlow<GameStats> = _gameStats.asStateFlow()
    
    private var gameStartTime: Long = 0L
    
    init {
        loadGameData()
    }
    
    private fun loadGameData() {
        viewModelScope.launch {
            gameUseCase.getGameSettings().collect { settings ->
                _gameSettings.value = settings
            }
        }
        
        viewModelScope.launch {
            gameUseCase.getGameStats().collect { stats ->
                _gameStats.value = stats
            }
        }
    }
    
    fun startNewGame() {
        val settings = _gameSettings.value
        val gridWidth = settings.gridWidth
        val gridHeight = settings.gridHeight
        
        // Create grid with proper dimensions
        val grid = List(gridHeight) { List(gridWidth) { GridCell.Empty } }
        
        val newBlock = gameUseCase.generateRandomBlock().copy(position = Position(gridWidth / 2 - 1, 0))
        val nextBlock = gameUseCase.generateRandomBlock()
        
        _gameState.value = GameState(
            grid = grid,
            currentBlock = newBlock,
            nextBlock = nextBlock,
            gameSpeed = gameUseCase.calculateGameSpeed(1)
        )
        
        gameStartTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            gameUseCase.incrementGamesPlayed()
        }
    }
    
    fun moveBlock(direction: MoveDirection) {
        val currentState = _gameState.value
        val currentBlock = currentState.currentBlock ?: return
        
        val newPosition = when (direction) {
            MoveDirection.LEFT -> currentBlock.position.copy(x = currentBlock.position.x - 1)
            MoveDirection.RIGHT -> currentBlock.position.copy(x = currentBlock.position.x + 1)
            MoveDirection.DOWN -> currentBlock.position.copy(y = currentBlock.position.y + 1)
        }
        
        if (isValidPosition(currentBlock.copy(position = newPosition))) {
            _gameState.value = currentState.copy(
                currentBlock = currentBlock.copy(position = newPosition)
            )
        } else if (direction == MoveDirection.DOWN) {
            // Block can't move down further, place it and start next block
            placeBlock(currentBlock)
        }
    }
    
    fun rotateBlock() {
        val currentState = _gameState.value
        val currentBlock = currentState.currentBlock ?: return
        
        val newRotation = (currentBlock.rotation + 90) % 360
        val rotatedBlock = currentBlock.copy(rotation = newRotation)
        
        if (isValidPosition(rotatedBlock)) {
            _gameState.value = currentState.copy(
                currentBlock = rotatedBlock
            )
        }
    }
    
    fun dropBlock() {
        val currentState = _gameState.value
        val currentBlock = currentState.currentBlock ?: return
        
        // Find the lowest valid position
        var dropPosition = currentBlock.position
        while (isValidPosition(currentBlock.copy(position = dropPosition.copy(y = dropPosition.y + 1)))) {
            dropPosition = dropPosition.copy(y = dropPosition.y + 1)
        }
        
        // Place the block at the lowest position
        placeBlock(currentBlock.copy(position = dropPosition))
    }
    
    private fun placeBlock(block: Block) {
        val currentState = _gameState.value
        val newGrid = placeBlockOnGrid(currentState.grid, block)
        
        // Check for line clears
        val (clearedGrid, linesCleared) = clearLines(newGrid)
        
        // Calculate new score and level
        val newScore = currentState.score + gameUseCase.calculateScore(linesCleared, currentState.combo)
        val newLinesCleared = currentState.linesCleared + linesCleared
        val newLevel = gameUseCase.calculateLevel(newLinesCleared)
        val newCombo = if (linesCleared > 0) currentState.combo + 1 else 0
        
        // Generate next block
        val nextBlock = currentState.nextBlock
        val newNextBlock = gameUseCase.generateRandomBlock()
        
        // Check for game over
        val settings = _gameSettings.value
        val startX = settings.gridWidth / 2 - 1
        val isGameOver = !nextBlock?.let { isValidPosition(it.copy(position = Position(startX, 0))) }!!
        
        _gameState.value = currentState.copy(
            grid = clearedGrid,
            currentBlock = nextBlock,
            nextBlock = newNextBlock,
            score = newScore,
            level = newLevel,
            linesCleared = newLinesCleared,
            combo = newCombo,
            isGameOver = isGameOver,
            gameSpeed = gameUseCase.calculateGameSpeed(newLevel)
        )
        
        // Update stats
        if (linesCleared > 0) {
            viewModelScope.launch {
                gameUseCase.addLinesCleared(linesCleared)
                gameUseCase.updateBestCombo(newCombo)
            }
        }
        
        if (isGameOver) {
            val playTime = System.currentTimeMillis() - gameStartTime
            viewModelScope.launch {
                gameUseCase.saveHighScore(newScore)
                gameUseCase.addPlayTime(playTime)
            }
        }
    }
    
    private fun isValidPosition(block: Block): Boolean {
        val grid = _gameState.value.grid
        val shape = block.getRotatedShape()
        
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x]) {
                    val gridX = block.position.x + x
                    val gridY = block.position.y + y
                    
                    if (gridX < 0 || gridX >= grid[0].size || 
                        gridY >= grid.size || 
                        (gridY >= 0 && grid[gridY][gridX].color != null)) {
                        return false
                    }
                }
            }
        }
        return true
    }
    
    private fun placeBlockOnGrid(grid: List<List<GridCell>>, block: Block): List<List<GridCell>> {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        val shape = block.getRotatedShape()
        
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x]) {
                    val gridX = block.position.x + x
                    val gridY = block.position.y + y
                    
                    if (gridY >= 0 && gridY < newGrid.size && gridX >= 0 && gridX < newGrid[0].size) {
                        newGrid[gridY][gridX] = GridCell(
                            color = block.color,
                            isSpecial = block.isSpecial,
                            specialType = block.specialType
                        )
                    }
                }
            }
        }
        
        return newGrid
    }
    
    private fun clearLines(grid: List<List<GridCell>>): Pair<List<List<GridCell>>, Int> {
        val newGrid = grid.toMutableList()
        var linesCleared = 0
        
        // Check horizontal lines
        for (y in grid.indices) {
            if (grid[y].all { it.color != null }) {
                newGrid.removeAt(y)
                newGrid.add(0, List(grid[0].size) { GridCell.Empty })
                linesCleared++
            }
        }
        
        return Pair(newGrid, linesCleared)
    }
    
    fun pauseGame() {
        _gameState.value = _gameState.value.copy(isPaused = true)
    }
    
    fun resumeGame() {
        _gameState.value = _gameState.value.copy(isPaused = false)
    }
    
    fun updateSettings(settings: GameSettings) {
        viewModelScope.launch {
            gameUseCase.updateGameSettings(settings)
        }
        
        // Update the settings state
        _gameSettings.value = settings
    }
    
    fun restartGameWithNewSettings() {
        startNewGame()
    }
}

enum class MoveDirection {
    LEFT, RIGHT, DOWN
} 