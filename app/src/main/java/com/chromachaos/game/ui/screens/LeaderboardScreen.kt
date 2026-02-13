package com.chromachaos.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chromachaos.game.R
import com.chromachaos.game.presentation.viewmodel.MainViewModel

@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val gameStats by viewModel.gameStats.collectAsState()
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.leaderboard_back),
                        tint = Color.White
                    )
                }
                
                Text(
                    text = stringResource(R.string.leaderboard_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // High Score Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.leaderboard_high_score),
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "${gameStats.highScore}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Statistics
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = stringResource(R.string.leaderboard_games_played),
                        value = "${gameStats.totalGamesPlayed}",
                        icon = Icons.Default.Star,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                item {
                    StatCard(
                        title = stringResource(R.string.leaderboard_lines_cleared),
                        value = "${gameStats.totalLinesCleared}",
                        icon = Icons.Default.Star,
                        color = Color(0xFF2196F3)
                    )
                }
                
                item {
                    StatCard(
                        title = stringResource(R.string.leaderboard_best_combo),
                        value = "${gameStats.bestCombo}",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFF9800)
                    )
                }
                
                item {
                    StatCard(
                        title = stringResource(R.string.leaderboard_total_play_time),
                        value = formatPlayTime(gameStats.totalPlayTime),
                        icon = Icons.Default.Star,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Achievements Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.leaderboard_achievements),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AchievementItem(
                        title = stringResource(R.string.leaderboard_first_game),
                        description = stringResource(R.string.leaderboard_first_game_desc),
                        isUnlocked = gameStats.totalGamesPlayed > 0,
                        color = Color(0xFF4CAF50)
                    )
                    
                    AchievementItem(
                        title = stringResource(R.string.leaderboard_line_master),
                        description = stringResource(R.string.leaderboard_line_master_desc),
                        isUnlocked = gameStats.totalLinesCleared >= 100,
                        color = Color(0xFF2196F3)
                    )
                    
                    AchievementItem(
                        title = stringResource(R.string.leaderboard_combo_king),
                        description = stringResource(R.string.leaderboard_combo_king_desc),
                        isUnlocked = gameStats.bestCombo >= 5,
                        color = Color(0xFFFF9800)
                    )
                    
                    AchievementItem(
                        title = stringResource(R.string.leaderboard_high_scorer),
                        description = stringResource(R.string.leaderboard_high_scorer_desc),
                        isUnlocked = gameStats.highScore >= 10000,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AchievementItem(
    title: String,
    description: String,
    isUnlocked: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = if (isUnlocked) color else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.5f)
            )
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = if (isUnlocked) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
            )
        }
        
        if (isUnlocked) {
            Text(
                text = stringResource(R.string.leaderboard_unlocked),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun formatPlayTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
} 