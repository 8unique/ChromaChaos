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

    companion object {
        /** Minimum consecutive same-color blocks in a straight line to clear. */
        private const val MIN_LINE_LENGTH = 4
    }
    
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
        
        val grid = List(gridHeight) { List(gridWidth) { GridCell.Empty } }
        
        val newBlock = gameUseCase.generateRandomBlock().copy(
            position = Position(gridWidth / 2 - 1, 0)
        )
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
        if (currentState.isPaused || currentState.isGameOver) return
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
            placeBlock(currentBlock)
        }
    }
    
    fun rotateBlock() {
        val currentState = _gameState.value
        if (currentState.isPaused || currentState.isGameOver) return
        val currentBlock = currentState.currentBlock ?: return
        
        val newRotation = (currentBlock.rotation + 90) % 360
        val rotatedBlock = currentBlock.copy(rotation = newRotation)
        
        if (isValidPosition(rotatedBlock)) {
            _gameState.value = currentState.copy(currentBlock = rotatedBlock)
        }
    }
    
    fun dropBlock() {
        val currentState = _gameState.value
        if (currentState.isPaused || currentState.isGameOver) return
        val currentBlock = currentState.currentBlock ?: return
        
        var dropPosition = currentBlock.position
        while (isValidPosition(currentBlock.copy(position = dropPosition.copy(y = dropPosition.y + 1)))) {
            dropPosition = dropPosition.copy(y = dropPosition.y + 1)
        }
        
        placeBlock(currentBlock.copy(position = dropPosition))
    }
    
    // ── Core game flow after a piece locks ──────────────────────────────
    private fun placeBlock(block: Block) {
        val currentState = _gameState.value
        var boardGrid = placeBlockOnGrid(currentState.grid, block)
        var totalScore = 0
        var totalBlocksCleared = 0
        var chainStep = 0

        // ── Chain reaction loop: scan → clear → gravity → repeat ────────
        while (true) {
            val cellsToClear = findColorLines(boardGrid, MIN_LINE_LENGTH)
            if (cellsToClear.isEmpty()) break

            chainStep++
            totalBlocksCleared += cellsToClear.size

            // Score: 10 pts per block × combo multiplier
            val multiplier = gameUseCase.getComboMultiplier(chainStep)
            val stepScore = gameUseCase.calculateColorLineScore(cellsToClear.size)
            totalScore += (stepScore * multiplier).toInt()

            boardGrid = clearCells(boardGrid, cellsToClear)
            boardGrid = applyGravity(boardGrid)
        }

        val anyClearHappened = chainStep > 0

        // ── Combo tracking (across piece placements) ────────────────────
        val newCombo = if (anyClearHappened) currentState.combo + 1 else 0

        // ── Level / speed ───────────────────────────────────────────────
        val newTotalLines = currentState.linesCleared + totalBlocksCleared
        val newLevel = gameUseCase.calculateLevel(newTotalLines)
        val newScore = currentState.score + totalScore

        // ── Spawn next piece ────────────────────────────────────────────
        val nextBlock = currentState.nextBlock
        val newNextBlock = gameUseCase.generateRandomBlock()
        val settings = _gameSettings.value
        val startX = settings.gridWidth / 2 - 1

        val spawnedBlock = nextBlock?.copy(position = Position(startX, 0))
        val isGameOver = spawnedBlock == null || !isValidPosition(spawnedBlock)

        _gameState.value = currentState.copy(
            grid = boardGrid,
            currentBlock = if (isGameOver) null else spawnedBlock,
            nextBlock = newNextBlock,
            score = newScore,
            level = newLevel,
            linesCleared = newTotalLines,
            combo = newCombo,
            chainCount = chainStep,
            isGameOver = isGameOver,
            gameSpeed = gameUseCase.calculateGameSpeed(newLevel)
        )

        // ── Persist stats ───────────────────────────────────────────────
        if (anyClearHappened) {
            viewModelScope.launch {
                gameUseCase.addLinesCleared(totalBlocksCleared)
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
    
    // ── Board helpers ───────────────────────────────────────────────────
    
    private fun isValidPosition(block: Block): Boolean {
        val grid = _gameState.value.grid
        if (grid.isEmpty()) return false
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
                    
                    if (gridY in 0 until newGrid.size && gridX in 0 until newGrid[0].size) {
                        newGrid[gridY][gridX] = GridCell(
                            color = block.color,
                            isSpecial = block.isSpecial,
                            specialType = block.specialType
                        )
                    }
                }
            }
        }
        
        return newGrid.map { it.toList() }
    }

    // ── Straight-line color clear system ─────────────────────────────────

    private data class Cell(val x: Int, val y: Int)

    /**
     * Scan the board for 4+ consecutive same-color blocks in
     * straight horizontal or vertical lines. Returns a de-duplicated
     * set of cells to clear (a cell at an intersection is only listed once).
     */
    private fun findColorLines(board: List<List<GridCell>>, minLength: Int): Set<Cell> {
        val height = board.size
        if (height == 0) return emptySet()
        val width = board[0].size
        val toClear = mutableSetOf<Cell>()

        // ── Horizontal scan ─────────────────────────────────────────────
        for (y in 0 until height) {
            var runStart = 0
            while (runStart < width) {
                val color = board[y][runStart].color
                if (color == null) { runStart++; continue }

                var runEnd = runStart + 1
                while (runEnd < width && board[y][runEnd].color == color) {
                    runEnd++
                }
                val runLen = runEnd - runStart
                if (runLen >= minLength) {
                    for (x in runStart until runEnd) {
                        toClear.add(Cell(x, y))
                    }
                }
                runStart = runEnd
            }
        }

        // ── Vertical scan ───────────────────────────────────────────────
        for (x in 0 until width) {
            var runStart = 0
            while (runStart < height) {
                val color = board[runStart][x].color
                if (color == null) { runStart++; continue }

                var runEnd = runStart + 1
                while (runEnd < height && board[runEnd][x].color == color) {
                    runEnd++
                }
                val runLen = runEnd - runStart
                if (runLen >= minLength) {
                    for (y in runStart until runEnd) {
                        toClear.add(Cell(x, y))
                    }
                }
                runStart = runEnd
            }
        }

        return toClear
    }

    /**
     * Set the given cells to empty.
     */
    private fun clearCells(board: List<List<GridCell>>, cells: Set<Cell>): List<List<GridCell>> {
        if (cells.isEmpty()) return board
        val newGrid = board.map { it.toMutableList() }
        for (cell in cells) {
            newGrid[cell.y][cell.x] = GridCell.Empty
        }
        return newGrid.map { it.toList() }
    }

    /**
     * Gravity: for each column, compact filled cells downward.
     */
    private fun applyGravity(board: List<List<GridCell>>): List<List<GridCell>> {
        val height = board.size
        if (height == 0) return board
        val width = board[0].size
        val newGrid = MutableList(height) { MutableList(width) { GridCell.Empty } }

        for (x in 0 until width) {
            var writeRow = height - 1
            for (y in height - 1 downTo 0) {
                val cell = board[y][x]
                if (cell.color != null) {
                    newGrid[writeRow][x] = cell
                    writeRow--
                }
            }
        }

        return newGrid.map { it.toList() }
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
        _gameSettings.value = settings
    }
    
    fun restartGameWithNewSettings() {
        startNewGame()
    }
}

enum class MoveDirection {
    LEFT, RIGHT, DOWN
} 