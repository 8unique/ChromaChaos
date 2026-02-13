package com.chromachaos.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chromachaos.game.data.model.*
import com.chromachaos.game.presentation.viewmodel.MainViewModel
import com.chromachaos.game.presentation.viewmodel.MoveDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val scope = rememberCoroutineScope()
    
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
            .background(Color(0xFF1A237E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            GameHeader(
                score = gameState.score,
                level = gameState.level,
                linesCleared = gameState.linesCleared,
                isPaused = gameState.isPaused,
                onPauseToggle = {
                    if (gameState.isPaused) viewModel.resumeGame() else viewModel.pauseGame()
                },
                onBack = { navController.navigateUp() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Game Area
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game Grid
                GameGrid(
                    grid = gameState.grid,
                    currentBlock = gameState.currentBlock,
                    modifier = Modifier.weight(2f)
                )
                
                // Side Panel
                GameSidePanel(
                    nextBlock = gameState.nextBlock,
                    combo = gameState.combo,
                    chainCount = gameState.chainCount,
                    onRotate = { viewModel.rotateBlock() },
                    onDrop = { viewModel.dropBlock() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls
            GameControls(
                onLeft = { viewModel.moveBlock(MoveDirection.LEFT) },
                onRight = { viewModel.moveBlock(MoveDirection.RIGHT) },
                onDown = { viewModel.moveBlock(MoveDirection.DOWN) },
                onRotate = { viewModel.rotateBlock() },
                onDrop = { viewModel.dropBlock() }
            )
        }
        
        // Game Over Dialog
        if (gameState.isGameOver) {
            GameOverDialog(
                score = gameState.score,
                onPlayAgain = {
                    viewModel.startNewGame()
                },
                onBackToMenu = {
                    navController.navigate("main_menu") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun GameHeader(
    score: Int,
    level: Int,
    linesCleared: Int,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Score: $score",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Level: $level | Lines: $linesCleared",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        
        IconButton(onClick = onPauseToggle) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                tint = Color.White
            )
        }
    }
}

@Composable
fun GameGrid(
    grid: List<List<GridCell>>,
    currentBlock: Block?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            grid.forEachIndexed { y, row ->
                Row(modifier = Modifier.weight(1f)) {
                    row.forEachIndexed { x, cell ->
                        GridCell(
                            cell = cell,
                            isCurrentBlock = currentBlock?.let { block ->
                                val shape = block.getRotatedShape()
                                val blockX = block.position.x
                                val blockY = block.position.y

                                for (sy in shape.indices) {
                                    for (sx in shape[sy].indices) {
                                        if (shape[sy][sx] &&
                                            x == blockX + sx &&
                                            y == blockY + sy) {
                                            return@let true
                                        }
                                    }
                                }
                                false
                            } ?: false,
                            blockColor = currentBlock?.color,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(0.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridCell(
    cell: GridCell,
    isCurrentBlock: Boolean,
    blockColor: Color?,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCurrentBlock -> blockColor ?: Color.Transparent
        cell.color != null -> cell.color
        else -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(3.dp)
            )
            .border(
                width = if (backgroundColor == Color.Transparent) 0.dp else 1.dp,
                color = if (backgroundColor == Color.Transparent) 
                    Color.White.copy(alpha = 0.1f) 
                else 
                    Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(3.dp)
            )
    )
}

@Composable
fun GameSidePanel(
    nextBlock: Block?,
    combo: Int,
    chainCount: Int,
    onRotate: () -> Unit,
    onDrop: () -> Unit
) {
    Column(
        modifier = Modifier.width(100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Next Block Preview
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Next",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                nextBlock?.let { block ->
                    NextBlockPreview(block = block)
                }
            }
        }
        
        // Combo
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Combo",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "$combo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }

        // Chain indicator
        if (chainCount > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00E676).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chain",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "x$chainCount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
        
        // Action Buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onRotate,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = "Rotate",
                    tint = Color.White
                )
            }
            
            Button(
                onClick = onDrop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Drop",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NextBlockPreview(block: Block) {
    val shape = block.getRotatedShape()
    val color = block.color

    Column {
        shape.forEach { row ->
            Row {
                row.forEach { isFilled ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isFilled) color else Color.Transparent,
                                shape = RoundedCornerShape(1.dp)
                            )
                            .padding(1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameControls(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onRotate: () -> Unit,
    onDrop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Movement Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                text = "←",
                onClick = onLeft,
                modifier = Modifier.size(48.dp)
            )
            
            ControlButton(
                text = "→",
                onClick = onRight,
                modifier = Modifier.size(48.dp)
            )
            
            ControlButton(
                text = "↓",
                onClick = onDown,
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Action Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                text = "Rotate",
                onClick = onRotate,
                modifier = Modifier.width(80.dp).height(48.dp)
            )
            
            ControlButton(
                text = "Drop",
                onClick = onDrop,
                modifier = Modifier.width(80.dp).height(48.dp)
            )
        }
    }
}

@Composable
fun ControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A237E)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Game Over",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Final Score: $score",
                    fontSize = 18.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Play Again",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = onBackToMenu,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Main Menu",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
} 