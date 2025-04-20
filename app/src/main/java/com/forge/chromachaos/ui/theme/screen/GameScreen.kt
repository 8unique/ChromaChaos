package com.forge.chromachaos.ui.theme.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.forge.chromachaos.R
import com.forge.chromachaos.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val rows = gameState.grid.size
        val cols = gameState.grid[0].size

        // Total padding/border space you add (1.dp border * 2 = 2.dp total per box)
        val spacingPerBox = 1.dp * 2

        // Calculate the max available width/height for the grid
        val availableWidth = maxWidth - 20.dp // account for horizontal padding (10.dp each side)
        val availableHeight = maxHeight - 200.dp // account for score + buttons + spacing

        // Box size is the minimum based on both dimensions
        val boxSize = min(
            (availableWidth / cols.toFloat()) - spacingPerBox,
            (availableHeight / rows.toFloat()) - spacingPerBox
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Score: ${gameState.score}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Game Grid
            Column(
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                for (rowIndex in gameState.grid.indices) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (colIndex in gameState.grid[rowIndex].indices) {
                            val block = gameState.currentBlock
                            val isBlockPart = block?.shape?.any {
                                block.position.first + it.first == rowIndex &&
                                        block.position.second + it.second == colIndex
                            } == true

                            val color = when {
                                isBlockPart -> block?.color ?: Color.Black
                                gameState.grid[rowIndex][colIndex].isOccupied -> gameState.grid[rowIndex][colIndex].color
                                else -> Color.LightGray
                            }

                            Box(
                                modifier = Modifier
                                    .size(boxSize)
                                    .background(color)
                                    .border(1.dp, Color.DarkGray)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Centered Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = { viewModel.moveBlockLeft() }) {
                    Text(stringResource(R.string.button_left))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.moveBlockRight() }) {
                    Text(stringResource(R.string.button_right))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.moveBlockDown() }) {
                    Text(stringResource(R.string.button_down))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
