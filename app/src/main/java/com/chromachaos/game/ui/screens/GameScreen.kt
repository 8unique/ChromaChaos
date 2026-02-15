package com.chromachaos.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chromachaos.game.R
import com.chromachaos.game.data.model.Block
import com.chromachaos.game.data.model.GridCell
import com.chromachaos.game.presentation.viewmodel.MainViewModel
import com.chromachaos.game.presentation.viewmodel.MoveDirection
import kotlinx.coroutines.delay

// ── Neon color palette ──────────────────────────────────────────────────────────
private val NeonBackground = Color(0xFF0A0E2A)
private val NeonBackgroundLight = Color(0xFF0F1640)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonCyanDim = Color(0xFF004D66)
private val NeonCyanBorder = Color(0xFF0088AA)
private val NeonGridLine = Color(0xFF0C3A5A)
private val NeonCellBg = Color(0xFF0B1A3A)
private val NeonGlow = Color(0xFF00BFFF)
private val NeonTextPrimary = Color.White
private val NeonTextSecondary = Color(0xFFAADDFF)

@Composable
fun GameScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startNewGame()
    }

    LaunchedEffect(gameState.isPaused, gameState.gameSpeed, gameState.isGameOver) {
        if (!gameState.isPaused && !gameState.isGameOver) {
            while (true) {
                delay(gameState.gameSpeed)
                viewModel.moveBlock(MoveDirection.DOWN)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NeonBackground, Color(0xFF060A1E), NeonBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── Top info bar ──────────────────────────────────────────────
            NeonTopBar(
                score = gameState.score,
                level = gameState.level,
                linesCleared = gameState.linesCleared,
                nextBlock = gameState.nextBlock
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Game grid ─────────────────────────────────────────────────
            NeonGameGrid(
                grid = gameState.grid,
                currentBlock = gameState.currentBlock,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Bottom controls ───────────────────────────────────────────
            NeonControls(
                onLeft = { viewModel.moveBlock(MoveDirection.LEFT) },
                onRight = { viewModel.moveBlock(MoveDirection.RIGHT) },
                onDown = { viewModel.moveBlock(MoveDirection.DOWN) },
                onRotate = { viewModel.rotateBlock() },
                onDrop = { viewModel.dropBlock() }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Game Over overlay ─────────────────────────────────────────────
        if (gameState.isGameOver) {
            GameOverDialog(
                score = gameState.score,
                onPlayAgain = { viewModel.startNewGame() },
                onBackToMenu = {
                    navController.navigate("main_menu") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Top bar: Score (left) + Next Piece preview (right)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NeonTopBar(
    score: Int,
    level: Int,
    linesCleared: Int,
    nextBlock: Block?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Score / level / lines — left side
        Column(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = NeonCyanBorder.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.game_screen_score, score),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NeonTextPrimary
            )
            Text(
                text = stringResource(R.string.game_screen_level_lines, level, linesCleared),
                fontSize = 13.sp,
                color = NeonTextSecondary
            )
        }

        // Next Piece — right side
        Column(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = NeonCyanBorder.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.game_screen_next_piece),
                fontSize = 12.sp,
                color = NeonTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            nextBlock?.let { block ->
                NeonNextBlockPreview(block = block)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Next-piece mini preview (coloured cells on transparent background)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NeonNextBlockPreview(block: Block) {
    val shape = block.getRotatedShape()
    val color = block.color

    Column {
        shape.forEach { row ->
            Row {
                row.forEach { isFilled ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .padding(1.dp)
                            .background(
                                color = if (isFilled) color else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .then(
                                if (isFilled) Modifier.border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(2.dp)
                                ) else Modifier
                            )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Game grid — drawn on a Canvas for crisp neon lines + glow border
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun NeonGameGrid(
    grid: List<List<GridCell>>,
    currentBlock: Block?,
    modifier: Modifier = Modifier
) {
    if (grid.isEmpty()) return

    val rows = grid.size
    val cols = grid[0].size

    // Pre-compute which cells belong to the current block
    val blockCells = mutableSetOf<Pair<Int, Int>>()
    currentBlock?.let { block ->
        val shape = block.getRotatedShape()
        for (sy in shape.indices) {
            for (sx in shape[sy].indices) {
                if (shape[sy][sx]) {
                    blockCells.add(Pair(block.position.x + sx, block.position.y + sy))
                }
            }
        }
    }

    val blockColor = currentBlock?.color ?: Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.7f), NeonCyanDim, NeonCyan.copy(alpha = 0.7f))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .drawBehind {
                // Outer glow
                drawRoundRect(
                    color = NeonGlow.copy(alpha = 0.10f),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx())
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / cols
            val cellH = size.height / rows

            // Background fill
            drawRect(color = NeonCellBg)

            // Draw grid lines (vertical)
            for (c in 0..cols) {
                val x = c * cellW
                drawLine(
                    color = NeonGridLine,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }

            // Draw grid lines (horizontal)
            for (r in 0..rows) {
                val y = r * cellH
                drawLine(
                    color = NeonGridLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Draw filled cells (landed blocks + current block)
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val cell = grid[y][x]
                    val isBlock = blockCells.contains(Pair(x, y))
                    val fillColor: Color? = when {
                        isBlock -> blockColor
                        cell.color != null -> cell.color
                        else -> null
                    }

                    if (fillColor != null) {
                        val left = x * cellW + 1f
                        val top = y * cellH + 1f
                        val cw = cellW - 2f
                        val ch = cellH - 2f

                        // Cell glow background
                        drawRect(
                            color = fillColor.copy(alpha = 0.25f),
                            topLeft = Offset(left - 2f, top - 2f),
                            size = Size(cw + 4f, ch + 4f)
                        )

                        // Solid cell
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(left, top),
                            size = Size(cw, ch),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        // Highlight edge (top-left shine)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.25f),
                            topLeft = Offset(left + 1f, top + 1f),
                            size = Size(cw - 2f, ch * 0.35f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Bottom controls — arrows · rotate · star / drop
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NeonControls(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onRotate: () -> Unit,
    onDrop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {


        // Rotate button with label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NeonCircleButton(
                icon = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.game_screen_rotate),
                onClick = onRotate,
                size = 52
            )
            Text(
                text = stringResource(R.string.game_screen_rotate),
                fontSize = 11.sp,
                color = NeonCyan,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // ← → ↓  movement cluster
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonCircleButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.game_screen_control_left),
                onClick = onLeft
            )
            NeonCircleButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.game_screen_control_right),
                onClick = onRight
            )
            NeonCircleButton(
                icon = Icons.Filled.ArrowDownward,
                contentDescription = stringResource(R.string.game_screen_control_down),
                onClick = onDown
            )
        }


        // Star / drop button
        NeonCircleButton(
            icon = Icons.Filled.KeyboardDoubleArrowDown,
            contentDescription = stringResource(R.string.game_screen_drop),
            onClick = onDrop,
            tint = NeonCyan
        )
    }
}

@Composable
private fun NeonCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Int = 48,
    tint: Color = NeonCyan
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size.dp)
            .border(
                width = 1.5.dp,
                color = NeonCyan.copy(alpha = 0.6f),
                shape = CircleShape
            )
            .background(
                color = NeonCyan.copy(alpha = 0.08f),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size((size * 0.5f).dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Game-over dialog (kept mostly identical, re-themed to neon)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GameOverDialog(
    score: Int,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.6f), NeonCyanDim)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = NeonBackgroundLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_screen_game_over),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.game_screen_final_score, score),
                    fontSize = 18.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.game_screen_play_again),
                            color = NeonBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onBackToMenu,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.game_screen_main_menu),
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}