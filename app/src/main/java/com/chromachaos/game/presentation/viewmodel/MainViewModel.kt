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

    /** Tracks whether the last spawned block was special (no consecutive specials). */
    private var lastBlockWasSpecial: Boolean = false

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
        
        lastBlockWasSpecial = false

        val newBlock = generateNextBlock().copy(
            position = Position(gridWidth / 2 - 1, 0)
        )
        val nextBlock = generateNextBlock()
        
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

    /**
     * Generate the next block, respecting special-block spawn rules.
     * Updates [lastBlockWasSpecial] as a side-effect.
     */
    private fun generateNextBlock(): Block {
        val settings = _gameSettings.value
        val special = gameUseCase.maybeGenerateSpecialBlock(settings, lastBlockWasSpecial)
        return if (special != null) {
            lastBlockWasSpecial = true
            special
        } else {
            lastBlockWasSpecial = false
            gameUseCase.generateRandomBlock()
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

        // ── Auto-trigger special blocks immediately on landing ──────────
        if (block.isSpecial && block.specialType != null) {
            val result = activateSpecialOnLand(boardGrid, block)
            boardGrid = result.grid
            totalScore += result.score
            totalBlocksCleared += result.blocksCleared
            if (result.blocksCleared > 0) combo++
        }

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
        val newNextBlock = generateNextBlock()
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

    // ══════════════════════════════════════════════════════════════════════
    //  SPECIAL BLOCK AUTO-ACTIVATION (triggered on landing, not by tap)
    // ══════════════════════════════════════════════════════════════════════

    private data class SpecialResult(
        val grid: List<List<GridCell>>,
        val score: Int,
        val blocksCleared: Int
    )

    /**
     * Dispatches to the correct special-block handler when the piece lands.
     */
    private fun activateSpecialOnLand(board: List<List<GridCell>>, block: Block): SpecialResult {
        // Special blocks are always DOT (1 cell), so the landing position is clear.
        val cx = block.position.x
        val cy = block.position.y

        return when (block.specialType) {
            SpecialBlockType.CROSS_CLEAR    -> applyCrossClear(board, cx, cy)
            SpecialBlockType.AREA_EXPLOSION -> applyAreaExplosion(board, cx, cy)
            SpecialBlockType.WILD           -> applyWildResolve(board, cx, cy)
            else -> SpecialResult(board, 0, 0)
        }
    }

    /**
     * CROSS CLEAR — clears the entire row AND entire column the block spawns
     * in, then applies gravity.
     */
    private fun applyCrossClear(
        board: List<List<GridCell>>,
        cx: Int, cy: Int
    ): SpecialResult {
        val height = board.size
        val width = board[0].size
        val cellsToClear = mutableSetOf<Cell>()

        // Full row
        for (x in 0 until width) cellsToClear.add(Cell(x, cy))
        // Full column
        for (y in 0 until height) cellsToClear.add(Cell(cx, y))

        val cleared = cellsToClear.count { board[it.y][it.x].color != null }
        val score = gameUseCase.calculateColorLineScore(cleared)

        var grid = clearCells(board, cellsToClear)
        grid = applyGravity(grid)

        return SpecialResult(grid, score, cleared)
    }

    /**
     * AREA EXPLOSION — clears the 3×3 area centred on ([cx],[cy]),
     * then applies gravity.
     */
    private fun applyAreaExplosion(
        board: List<List<GridCell>>,
        cx: Int, cy: Int
    ): SpecialResult {
        val height = board.size
        val width = board[0].size
        val cellsToClear = mutableSetOf<Cell>()

        for (dy in -1..1) {
            for (dx in -1..1) {
                val ny = cy + dy
                val nx = cx + dx
                if (ny in 0 until height && nx in 0 until width) {
                    cellsToClear.add(Cell(nx, ny))
                }
            }
        }

        val cleared = cellsToClear.count { board[it.y][it.x].color != null }
        val score = gameUseCase.calculateColorLineScore(cleared)

        var grid = clearCells(board, cellsToClear)
        grid = applyGravity(grid)

        return SpecialResult(grid, score, cleared)
    }

    /**
     * WILD BLOCK — automatically resolves to the most advantageous
     * adjacent colour.  Tries each adjacent normal colour, picks the one
     * that would clear the most cells in the standard detection pass.
     * If no adjacent colour produces any clear the Wild simply stays as
     * that colour on the board (helping future matches).
     */
    private fun applyWildResolve(
        board: List<List<GridCell>>,
        cx: Int, cy: Int
    ): SpecialResult {
        val height = board.size
        val width = board[0].size

        // Gather unique adjacent normal colours
        val deltas = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        val candidateColors = mutableSetOf<androidx.compose.ui.graphics.Color>()
        for ((dx, dy) in deltas) {
            val nx = cx + dx; val ny = cy + dy
            if (ny in 0 until height && nx in 0 until width) {
                val adj = board[ny][nx]
                if (adj.color != null && !adj.isSpecial) {
                    candidateColors.add(adj.color)
                }
            }
        }

        if (candidateColors.isEmpty()) {
            // No neighbours — just convert Wild to a random normal colour
            val fallback = BlockColors.allColors.random()
            val mGrid = board.map { it.toMutableList() }.toMutableList()
            mGrid[cy][cx] = GridCell(color = fallback, isSpecial = false, specialType = null)
            return SpecialResult(mGrid.map { it.toList() }, 0, 0)
        }

        // For each candidate colour, simulate detection to find which clears most
        data class Candidate(
            val color: androidx.compose.ui.graphics.Color,
            val clearCount: Int
        )

        val candidates = candidateColors.map { tryColor ->
            val simGrid = board.map { it.toMutableList() }.toMutableList()
            simGrid[cy][cx] = GridCell(color = tryColor, isSpecial = false, specialType = null)
            val simBoard: List<List<GridCell>> = simGrid.map { it.toList() }
            val lineCells = findColorLines(simBoard, MIN_LINE_LENGTH)
            val rectCells = findSolidRectangles(simBoard)
            val total = (lineCells union rectCells).size
            Candidate(tryColor, total)
        }

        val best = candidates.maxByOrNull { it.clearCount }!!

        // Apply the best colour
        val mGrid = board.map { it.toMutableList() }.toMutableList()
        mGrid[cy][cx] = GridCell(color = best.color, isSpecial = false, specialType = null)
        val resolvedBoard: List<List<GridCell>> = mGrid.map { it.toList() }

        // The standard chain-reaction loop in placeBlock will now pick up
        // any clears caused by this colour swap, so return score 0 here.
        return SpecialResult(resolvedBoard, 0, 0)
    }
    
    // ── Board helpers ───────────────────────────────────────────────────

    /**
     * Returns `true` if the cell at ([y],[x]) is a Wild cell.
     */
    private fun isWild(board: List<List<GridCell>>, y: Int, x: Int): Boolean {
        val cell = board[y][x]
        return cell.isSpecial && cell.specialType == SpecialBlockType.WILD
    }


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
     * Wild cells match any colour being scanned. A run cannot consist
     * entirely of Wild cells (Wild does NOT start its own cluster).
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
                val startCell = board[y][runStart]
                // Determine the run colour — skip empty, look past leading Wilds
                if (startCell.color == null) { runStart++; continue }

                var runColor: androidx.compose.ui.graphics.Color? =
                    if (isWild(board, y, runStart)) null else startCell.color

                var runEnd = runStart + 1
                while (runEnd < width) {
                    val cell = board[y][runEnd]
                    if (cell.color == null) break
                    if (isWild(board, y, runEnd)) {
                        // Wild extends any run
                        runEnd++; continue
                    }
                    if (runColor == null) {
                        // First non-Wild — this defines the run colour
                        runColor = cell.color
                        runEnd++; continue
                    }
                    if (cell.color == runColor) {
                        runEnd++; continue
                    }
                    break
                }
                // Only clear if we found at least one real colour and length ≥ min
                if (runColor != null && runEnd - runStart >= minLength) {
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
                val startCell = board[runStart][x]
                if (startCell.color == null) { runStart++; continue }

                var runColor: androidx.compose.ui.graphics.Color? =
                    if (isWild(board, runStart, x)) null else startCell.color

                var runEnd = runStart + 1
                while (runEnd < height) {
                    val cell = board[runEnd][x]
                    if (cell.color == null) break
                    if (isWild(board, runEnd, x)) {
                        runEnd++; continue
                    }
                    if (runColor == null) {
                        runColor = cell.color
                        runEnd++; continue
                    }
                    if (cell.color == runColor) {
                        runEnd++; continue
                    }
                    break
                }
                if (runColor != null && runEnd - runStart >= minLength) {
                    for (yy in runStart until runEnd) {
                        toClear.add(Cell(x, yy))
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
     * of size 3×2 or 2×3. Wild cells count as any colour; the anchor
     * colour is the first non-Wild cell in the rectangle.
     * Returns the de-duplicated union of all cells belonging to qualifying rectangles.
     */
    private fun findSolidRectangles(board: List<List<GridCell>>): Set<Cell> {
        val height = board.size
        if (height == 0) return emptySet()
        val width = board[0].size
        val toClear = mutableSetOf<Cell>()

        // ── Scan all 3-wide × 2-tall rectangles ─────────────────────────
        for (y in 0..height - 2) {
            for (x in 0..width - 3) {
                val anchorColor = findAnchorColor(board, x, y, 3, 2) ?: continue
                if (isFilledRect(board, x, y, 3, 2, anchorColor)) {
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
                val anchorColor = findAnchorColor(board, x, y, 2, 3) ?: continue
                if (isFilledRect(board, x, y, 2, 3, anchorColor)) {
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
     * Find the first non-Wild colour in the rectangle starting at ([x],[y])
     * with [w]idth and [h]eight.  Returns null if every cell is Wild or empty.
     */
    private fun findAnchorColor(
        board: List<List<GridCell>>,
        x: Int, y: Int,
        w: Int, h: Int
    ): androidx.compose.ui.graphics.Color? {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val cell = board[y + dy][x + dx]
                if (cell.color != null && !isWild(board, y + dy, x + dx)) {
                    return cell.color
                }
            }
        }
        return null
    }

    /**
     * Check whether every cell in the rectangle starting at ([x],[y])
     * with the given [w]idth and [h]eight is filled with [color].
     * Wild cells are treated as matching any colour.
     */
    private fun isFilledRect(
        board: List<List<GridCell>>,
        x: Int, y: Int,
        w: Int, h: Int,
        color: androidx.compose.ui.graphics.Color
    ): Boolean {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val cell = board[y + dy][x + dx]
                if (cell.color == null) return false
                if (isWild(board, y + dy, x + dx)) continue  // Wild matches any
                if (cell.color != color) return false
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