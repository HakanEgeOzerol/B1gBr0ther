package com.b1gbr0ther.easteregg

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Point(val x: Float, val y: Float)

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class SnakeViewModel {
    // Game state
    var isGameOver by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var score by mutableStateOf(0)
        private set
    
    // Snake properties
    val snake = mutableStateListOf<Point>()
    var food by mutableStateOf(Point(0f, 0f))
        private set
    private var direction = Direction.RIGHT
    private var nextDirection = Direction.RIGHT
    
    // Game dimensions
    private var screenWidth = 0f
    private var screenHeight = 0f
    var cellSize = 0f
        private set
    private var gridWidth = 0
    private var gridHeight = 0
    
    // Game speed
    private var gameSpeed = 150L // milliseconds per move
    private var minGameSpeed = 80L // fastest speed
    
    // High scores
    private val maxHighScores = 5
    val highScores = mutableStateListOf<Int>()
    private var context: Context? = null
    
    // Game loop
    private var gameJob: Job? = null
    private val gameScope = CoroutineScope(Dispatchers.Default)
    
    fun setContext(context: Context) {
        this.context = context
        loadHighScores()
    }
    
    fun setScreenDimensions(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        
        // Calculate cell size and grid dimensions
        cellSize = screenWidth / 20 // 20 cells across
        gridWidth = (screenWidth / cellSize).toInt()
        gridHeight = (screenHeight / cellSize).toInt()
        
        // Initialize snake
        resetGame()
    }
    
    private fun resetGame() {
        // Clear snake
        snake.clear()
        
        // Create initial snake (3 segments)
        val startX = (gridWidth / 2) * cellSize
        val startY = (gridHeight / 2) * cellSize
        
        snake.add(Point(startX, startY)) // Head
        snake.add(Point(startX - cellSize, startY)) // Body
        snake.add(Point(startX - cellSize * 2, startY)) // Tail
        
        // Reset direction
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        
        // Place initial food
        placeFood()
        
        // Reset score
        score = 0
        
        // Reset game speed
        gameSpeed = 150L
    }
    
    private fun placeFood() {
        var validPosition = false
        var newFood: Point
        
        // Keep trying until we find a position not occupied by the snake
        do {
            val foodX = Random.nextInt(gridWidth) * cellSize
            val foodY = Random.nextInt(gridHeight) * cellSize
            newFood = Point(foodX.toFloat(), foodY.toFloat())
            
            // Check if food position overlaps with snake
            validPosition = !snake.any { it.x == newFood.x && it.y == newFood.y }
        } while (!validPosition)
        
        food = newFood
    }
    
    fun changeDirection(newDirection: Direction) {
        // Prevent 180-degree turns
        nextDirection = when (direction) {
            Direction.UP -> if (newDirection != Direction.DOWN) newDirection else direction
            Direction.DOWN -> if (newDirection != Direction.UP) newDirection else direction
            Direction.LEFT -> if (newDirection != Direction.RIGHT) newDirection else direction
            Direction.RIGHT -> if (newDirection != Direction.LEFT) newDirection else direction
        }
    }
    
    private fun updateGame() {
        if (isGameOver || isPaused) return
        
        // Update direction
        direction = nextDirection
        
        // Calculate new head position
        val head = snake.first()
        val newHead = when (direction) {
            Direction.UP -> Point(head.x, head.y - cellSize)
            Direction.DOWN -> Point(head.x, head.y + cellSize)
            Direction.LEFT -> Point(head.x - cellSize, head.y)
            Direction.RIGHT -> Point(head.x + cellSize, head.y)
        }
        
        // Check for collisions with walls
        val hitWall = newHead.x < 0 || newHead.x >= screenWidth || 
                      newHead.y < 0 || newHead.y >= screenHeight
        
        // Check for collisions with self (except tail which will move)
        val hitSelf = snake.dropLast(1).any { it.x == newHead.x && it.y == newHead.y }
        
        if (hitWall || hitSelf) {
            isGameOver = true
            saveHighScore(score)
            return
        }
        
        // Check if snake eats food
        val ateFood = newHead.x == food.x && newHead.y == food.y
        
        // Add new head
        snake.add(0, newHead)
        
        if (ateFood) {
            // Increase score
            score++
            
            // Place new food
            placeFood()
            
            // Increase speed every 5 points
            if (score % 5 == 0 && gameSpeed > minGameSpeed) {
                gameSpeed -= 10
            }
        } else {
            // Remove tail if no food was eaten
            snake.removeAt(snake.size - 1)
        }
    }
    
    fun startGame() {
        if (gameJob?.isActive == true) return
        
        isGameOver = false
        isPaused = false
        
        gameJob = gameScope.launch {
            while (true) {
                updateGame()
                delay(gameSpeed)
            }
        }
    }
    
    fun pauseGame() {
        isPaused = true
    }
    
    fun resumeGame() {
        isPaused = false
    }
    
    fun stopGame() {
        gameJob?.cancel()
        gameJob = null
    }
    
    fun restartGame() {
        stopGame()
        resetGame()
        isGameOver = false
        startGame()
    }
    
    // Load high scores from SharedPreferences
    private fun loadHighScores() {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("snake_game_prefs", Context.MODE_PRIVATE)
            highScores.clear()
            
            for (i in 0 until maxHighScores) {
                val score = prefs.getInt("high_score_$i", 0)
                if (score > 0) {
                    highScores.add(score)
                }
            }
        }
    }
    
    // Save new high score
    private fun saveHighScore(newScore: Int) {
        if (newScore <= 0) return
        
        // Add new score to list
        highScores.add(newScore)
        
        // Sort scores in descending order
        highScores.sortByDescending { it }
        
        // Keep only top N scores
        while (highScores.size > maxHighScores) {
            highScores.removeAt(highScores.size - 1)
        }
        
        // Save to SharedPreferences
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("snake_game_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            for (i in highScores.indices) {
                editor.putInt("high_score_$i", highScores[i])
            }
            
            editor.apply()
        }
    }
}
