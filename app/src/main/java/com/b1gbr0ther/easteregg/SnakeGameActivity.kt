package com.b1gbr0ther.easteregg

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b1gbr0ther.LocaleHelper
import com.b1gbr0ther.R
import com.b1gbr0ther.ThemeManager
import kotlinx.coroutines.*
import kotlin.random.Random

class SnakeGameActivity : ComponentActivity() {
    private val snakeViewModel = SnakeViewModel()
    
    companion object {
        @Volatile
        private var isGameActive = false

        fun isGameRunning(): Boolean = isGameActive
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ThemeManager.applyTheme(this)
        
        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        snakeViewModel.setContext(this)
        
        setContent {
            SnakeGameScreen(snakeViewModel, this)
        }
    }
    
    override fun onResume() {
        super.onResume()
        isGameActive = true
        if (!snakeViewModel.isGameOver) {
            snakeViewModel.resumeGame()
        }
    }
    
    override fun onPause() {
        super.onPause()
        isGameActive = false
        snakeViewModel.pauseGame()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isGameActive = false
        snakeViewModel.stopGame()
    }
}

@Composable
fun SnakeGameScreen(snakeViewModel: SnakeViewModel, context: ComponentActivity) {
    val score = snakeViewModel.score
    val isGameOver = snakeViewModel.isGameOver
    val isPaused = snakeViewModel.isPaused
    
    // Get screen dimensions
    val density = LocalDensity.current
    val screenWidth = with(density) { 360.dp.toPx() }
    val screenHeight = with(density) { 640.dp.toPx() }
    
    // Start game
    LaunchedEffect(Unit) {
        snakeViewModel.setScreenDimensions(screenWidth, screenHeight)
        snakeViewModel.startGame()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!isPaused && !isGameOver) {
            // Game canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Determine direction based on tap position
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            
                            when {
                                // Tap on left side
                                offset.x < centerX && offset.y > centerY - 100 && offset.y < centerY + 100 -> {
                                    snakeViewModel.changeDirection(Direction.LEFT)
                                }
                                // Tap on right side
                                offset.x > centerX && offset.y > centerY - 100 && offset.y < centerY + 100 -> {
                                    snakeViewModel.changeDirection(Direction.RIGHT)
                                }
                                // Tap on top
                                offset.y < centerY -> {
                                    snakeViewModel.changeDirection(Direction.UP)
                                }
                                // Tap on bottom
                                else -> {
                                    snakeViewModel.changeDirection(Direction.DOWN)
                                }
                            }
                        }
                    }
            ) {
                // Draw game board
                drawRect(
                    color = Color.DarkGray,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height)
                )
                
                // Draw snake
                snakeViewModel.snake.forEach { segment ->
                    drawRect(
                        color = Color(0xFF6200EE), // Purple color
                        topLeft = Offset(segment.x, segment.y),
                        size = Size(snakeViewModel.cellSize, snakeViewModel.cellSize)
                    )
                }
                
                // Draw food
                drawRect(
                    color = Color(0xFFBB86FC), // Light purple color
                    topLeft = Offset(snakeViewModel.food.x, snakeViewModel.food.y),
                    size = Size(snakeViewModel.cellSize, snakeViewModel.cellSize)
                )
            }
            
            // Pause button
            Button(
                onClick = { snakeViewModel.pauseGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.game_pause_button)
                ),
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Pause",
                    color = colorResource(id = R.color.game_pause_text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Score display
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        if (isGameOver) {
            GameOverScreen(
                score = score,
                highScores = snakeViewModel.highScores.toList(),
                onRestart = { snakeViewModel.restartGame() },
                onExit = { context.finish() }
            )
        }
        
        if (isPaused) {
            PauseScreen(
                onResume = { snakeViewModel.resumeGame() },
                onExit = { context.finish() }
            )
        }
    }
}

@Composable
fun GameOverScreen(
    score: Int,
    highScores: List<Int>,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Game Over",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Score: $score",
                color = Color.White,
                fontSize = 24.sp
            )
            
            // High scores
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "High Scores",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (highScores.isEmpty()) {
                    Text(
                        text = "No high scores yet",
                        fontSize = 18.sp,
                        color = Color.LightGray
                    )
                } else {
                    highScores.forEachIndexed { index, highScore ->
                        Text(
                            text = "${index + 1}. $highScore",
                            fontSize = 18.sp,
                            color = if (highScore == score) Color(0xFFBB86FC) else Color.LightGray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Play again button
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = "Play Again",
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
            
            // Exit button
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8E4EC6)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = "Exit",
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
fun PauseScreen(
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Game Paused",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Resume button
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = "Resume",
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
            
            // Exit button
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8E4EC6)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = "Exit",
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
        }
    }
}
