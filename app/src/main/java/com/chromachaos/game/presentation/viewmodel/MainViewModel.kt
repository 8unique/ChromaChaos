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
        /** Rule A — minimum consecutive same-color blocks in a straight line to clear. */
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
    
    // ══════════════════════════════════════════════════════════════════════
    //  CORE GAME LOOP
    //
    //  Piece locks → merge to board →
    //    Step 1: detect ALL straight lines (Rule A)
    //    Step 2: detect ALL solid 2×3 / 3×2 rectangles (Rule B)
    //    Step 3: merge marks (union — no double-clear)
    //    Step 4: clear all marked simultaneously
    //    Step 5: apply gravity
    //    Step 6: repeat detection (combo loop)
    //  Stop when no new clear → spawn next piece
    // ══════════════════════════════════════════════════════════════════════

    private fun placeBlock(block: Block) {
        val currentState = _gameState.value
        var boardGrid = placeBlockOnGrid(currentState.grid, block)
        var totalScore = 0
        var totalBlocksCleared = 0

        // Combo = chain depth within THIS placement.
        // Increases when gravity causes another clear.
        // Resets when new piece spawns.
        var combo = 0

        // ── Chain reaction loop: detect → clear → gravity → repeat ──────
        while (true) {
            // Step 1: Rule A — straight line detection (≥4 consecutive same-color)
            val lineCells = findColorLines(boardGrid, MIN_LINE_LENGTH)
            // Step 2: Rule B — solid rectangle detection (2×3 / 3×2 same-color)
            val rectCells = findSolidRectangles(boardGrid)
            // Step 3: Merge (union) — no double-clearing
            val cellsToClear = lineCells union rectCells

            if (cellsToClear.isEmpty()) break          // no clears → stop chain

            combo++
            totalBlocksCleared += cellsToClear.size

            // Score = clearedBlocks × 10 × comboMultiplier
            val multiplier = gameUseCase.getComboMultiplier(combo)
            val stepScore = gameUseCase.calculateColorLineScore(cellsToClear.size)
            totalScore += (stepScore * multiplier).toInt()

            // Step 4: Clear all marked cells simultaneously
            boardGrid = clearCells(boardGrid, cellsToClear)
            // Step 5: Classic gravity — compact non-empty cells downward
            boardGrid = applyGravity(boardGrid)
            // Step 6: Loop back — scan again after gravity for chain reaction
        }

        // ── Level / speed ───────────────────────────────────────────────
        val newTotalLines = currentState.linesCleared + totalBlocksCleared
        val newLevel = gameUseCase.calculateLevel(newTotalLines)
        val newScore = currentState.score + totalScore

        // ── Spawn next piece (spec §16 final step) ──────────────────────
        val nextBlock = currentState.nextBlock
        val newNextBlock = gameUseCase.generateRandomBlock()
        val settings = _gameSettings.value
        val startX = settings.gridWidth / 2 - 1

        val spawnedBlock = nextBlock?.copy(position = Position(startX, 0))
        // Check against the cleared+gravity grid, not the stale pre-clear grid
        val isGameOver = spawnedBlock == null || !isValidPosition(spawnedBlock, boardGrid)

        _gameState.value = currentState.copy(
            grid = boardGrid,
            currentBlock = if (isGameOver) null else spawnedBlock,
            nextBlock = newNextBlock,
            score = newScore,
            level = newLevel,
            linesCleared = newTotalLines,
            combo = combo,                // per-placement combo (spec §12)
            chainCount = combo,           // same value, kept for UI display
            isGameOver = isGameOver,
            gameSpeed = gameUseCase.calculateGameSpeed(newLevel)
        )

        // ── Persist stats ───────────────────────────────────────────────
        if (combo > 0) {
            viewModelScope.launch {
                gameUseCase.addLinesCleared(totalBlocksCleared)
                gameUseCase.updateBestCombo(combo)
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
    
    private fun isValidPosition(
        block: Block,
        grid: List<List<GridCell>> = _gameState.value.grid
    ): Boolean {
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

    // ══════════════════════════════════════════════════════════════════════
    //  RULE A — STRAIGHT LINE CLEAR
    //
    //  4+ consecutive same-color blocks in a straight horizontal or
    //  vertical line. No diagonals, no L/T/stair shapes — only runs.
    //
    //  Board scanned fully (H then V); cells collected into a
    //  de-duplicated set so crossing-point cells are listed once.
    // ══════════════════════════════════════════════════════════════════════

    private data class Cell(val x: Int, val y: Int)

    /**
     * RULE A — Scan for ≥[minLength] consecutive same-color runs.
     *
     * Step 1 — Horizontal: each row, left→right.
     * Step 2 — Vertical: each column, top→bottom.
     *
     * Returns de-duplicated [Set].
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
                if (runEnd - runStart >= minLength) {
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
                if (runEnd - runStart >= minLength) {
                    for (y in runStart until runEnd) {
                        toClear.add(Cell(x, y))
                    }
                }
                runStart = runEnd
            }
        }

        return toClear
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RULE B — SOLID RECTANGLE CLEAR
    //
    //  A solid, fully-filled rectangle of same-color blocks clears when
    //  its dimensions are exactly 2×3 (2 wide, 3 tall) or 3×2 (3 wide,
    //  2 tall). The system uses a sliding window to find ALL such
    //  sub-rectangles on the board.
    //
    //  Clears:     3×2, 2×3, and larger shapes containing them (e.g. 3×3
    //              is covered by overlapping 3×2 + 2×3 windows).
    //  No clear:   2×2 (only 4), staircases, thin zigzags, irregular
    //              shapes that don't contain a full 2×3 or 3×2.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * RULE B — Sliding-window scan for every solid same-color rectangle
     * of size 3×2 or 2×3. Returns the de-duplicated union of all cells
     * belonging to qualifying rectangles.
     */
    private fun findSolidRectangles(board: List<List<GridCell>>): Set<Cell> {
        val height = board.size
        if (height == 0) return emptySet()
        val width = board[0].size
        val toClear = mutableSetOf<Cell>()

        // ── Scan all 3-wide × 2-tall rectangles ─────────────────────────
        for (y in 0..height - 2) {
            for (x in 0..width - 3) {
                val color = board[y][x].color ?: continue
                if (isFilledRect(board, x, y, 3, 2, color)) {
                    for (dy in 0..1) {
                        for (dx in 0..2) {
                            toClear.add(Cell(x + dx, y + dy))
                        }
                    }
                }
            }
        }

        // ── Scan all 2-wide × 3-tall rectangles ─────────────────────────
        for (y in 0..height - 3) {
            for (x in 0..width - 2) {
                val color = board[y][x].color ?: continue
                if (isFilledRect(board, x, y, 2, 3, color)) {
                    for (dy in 0..2) {
                        for (dx in 0..1) {
                            toClear.add(Cell(x + dx, y + dy))
                        }
                    }
                }
            }
        }

        return toClear
    }

    /**
     * Check whether every cell in the rectangle starting at ([x],[y])
     * with the given [w]idth and [h]eight is filled with [color].
     */
    private fun isFilledRect(
        board: List<List<GridCell>>,
        x: Int, y: Int,
        w: Int, h: Int,
        color: androidx.compose.ui.graphics.Color
    ): Boolean {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                if (board[y + dy][x + dx].color != color) return false
            }
        }
        return true
    }

    /**
     * Step 4 – Clear all marked cells simultaneously.
     * Mark first, clear after — prevents detection errors.
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
     * Step 5 – GRAVITY SYSTEM
     * For each column, compact all non-empty cells downward and
     * fill the top with empty cells. Classic Tetris gravity.
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