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
import kotlin.math.sqrt
import kotlin.random.Random

data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

class GameViewModel {
    var isGameOver by mutableStateOf(false)
        private set

    private val maxHighScores = 5
    private var context: Context? = null
    val highScores = mutableStateListOf<Int>()
    
    var playerX by mutableStateOf(0f)
        private set
    var playerY by mutableStateOf(0f)
        private set
    var isFacingRight by mutableStateOf(false)
        private set

    // Camera offset for scrolling
    private var cameraOffsetY = 0f

    var score by mutableStateOf(0)
        private set

    // Player dimensions for collision detection
    private val playerWidth = 80f
    private val playerHeight = 80f

    private var standardPlatformWidth = 0f
    private var standardPlatformHeight = 0f

    val platforms = mutableStateListOf<Platform>()

    private var screenWidth = 0f
    private var screenHeight = 0f

    // Physics variables
    private var velocityY = 0f
    private var velocityX = 0f
    private var speedMultiplier = 1.0f
    private var lastSpeedIncreaseScore = 0
    private val maxSpeedMultiplier = 5.0f
    private val baseGravity = 0.5f
    private val baseJumpPower = -25f
    private val gravity get() = baseGravity * minOf(speedMultiplier, maxSpeedMultiplier)
    private val jumpPower get() = baseJumpPower * minOf(sqrt(speedMultiplier.toDouble()).toFloat(), sqrt(maxSpeedMultiplier.toDouble()).toFloat())

    // Track highest platform position for new platform generation
    private var highestPlatformY = 0f

    // Minimum distance between platforms in Y direction
    private val minPlatformDistance = 100f
    
    private var gameJob: Job? = null
    private val gameScope = CoroutineScope(Dispatchers.Default)

    var isPaused = false
        private set

    fun pauseGame() {
        isPaused = true
    }

    fun resumeGame() {
        isPaused = false
    }

    fun setScreenDimensions(width: Float, height: Float, platformWidth: Float, platformHeight: Float) {
        screenWidth = width
        screenHeight = height
        standardPlatformWidth = platformWidth
        standardPlatformHeight = platformHeight
    }

    fun setContext(context: Context) {
        this.context = context
        loadHighScores()
    }

    fun addPlatform(x: Float, y: Float, width: Float, height: Float) {
        platforms.add(Platform(x, y, width, height))
    }

    fun clearPlatforms() {
        platforms.clear()
    }

    fun resetPlayerPosition() {
        playerX = screenWidth / 2 - playerWidth / 2 // Center player
        playerY = screenHeight * 0.7f - playerHeight - 5f
        cameraOffsetY = 0f
        velocityY = 0f
        velocityX = 0f
        score = 0
        speedMultiplier = 1.0f       // Reset speed multiplier
        lastSpeedIncreaseScore = 0    // Reset last score threshold

        // Initialize highest platform position
        highestPlatformY = Float.MAX_VALUE
        platforms.forEach { platform ->
            if (platform.y < highestPlatformY) {
                highestPlatformY = platform.y
            }
        }
    }

    fun updatePlayerHorizontalPosition(tiltValue: Float) {
        velocityX = tiltValue * 2

        // Update sprite direction based on movement
        if (tiltValue > 0) {
            isFacingRight = true
        } else if (tiltValue < 0) {
            isFacingRight = false
        }
    }

    fun generateRandomPlatform(yPosition: Float) {
        val randomX = Random.nextFloat() * (screenWidth - standardPlatformWidth)
        addPlatform(randomX, yPosition, standardPlatformWidth, standardPlatformHeight)

        // Update highest position
        if (yPosition < highestPlatformY) {
            highestPlatformY = yPosition
        }
    }

    // Main game update loop
    private fun updateGame() {
        if (isGameOver || isPaused) return

        val previousY = playerY

        // Apply gravity
        velocityY += gravity

        // Calculate new player position
        val newPlayerY = playerY + velocityY

        // Fixed position for screen middle
        val screenMidPoint = screenHeight / 2

        // Check if we reached a new 40 point threshold
        val currentScoreThreshold = (score / 40) * 40
        if (currentScoreThreshold > lastSpeedIncreaseScore && currentScoreThreshold > 0) {
            // Increase speed by 1.8x for each new threshold
            speedMultiplier *= 1.8f
            lastSpeedIncreaseScore = currentScoreThreshold
        }

        // If player tries to go above screen middle
        if (newPlayerY < screenMidPoint && velocityY < 0) {
            // Lock player at screen middle
            val offset = -velocityY  // Offset is opposite of velocity

            // Move all platforms down
            for (i in platforms.indices) {
                val platform = platforms[i]
                platforms[i] = platform.copy(y = platform.y + offset)
            }

            // Increase score based on movement
            cameraOffsetY -= offset
            score = (-cameraOffsetY / 100).toInt()

            // Generate new platforms when player ascends
            if (highestPlatformY > cameraOffsetY - minPlatformDistance) {
                val newPlatformY = highestPlatformY - minPlatformDistance - Random.nextFloat() * 50f
                generateRandomPlatform(newPlatformY)
            }

            // Only remove platforms when they're really off screen
            // 1000f ensures they're completely out of view before removal
            platforms.removeAll { it.y > screenHeight + 1000f }
        } else {
            // If player is below middle, let them move normally
            playerY = newPlayerY
        }

        // Apply horizontal velocity in all cases
        playerX += velocityX

        // Check platform collisions
        handleCollisions(previousY)

        // Screen horizontal boundaries
        if (playerX < -40f) playerX = screenWidth + 40f
        if (playerX > screenWidth + 40f) playerX = -40f

        checkGameOver()
    }

    private fun handleCollisions(previousY: Float) {
        // Only check when player is moving downward
        if (velocityY <= 0f) return
    
        val prevBottom = previousY + playerHeight
        val currBottom = playerY + playerHeight
    
        for (platform in platforms) {
            val platTop = platform.y
    
            // Vertical culling: skip if platform not between last & current bottom
            if (platTop < prevBottom || platTop > currBottom) continue
    
            // Horizontal AABB
            val platLeft = platform.x
            val platRight = platLeft + platform.width
            val playerLeft = playerX
            val playerRight = playerX + playerWidth
    
            if (playerRight >= platLeft && playerLeft <= platRight) {
                // Collision! Snap player to platform and bounce
                velocityY = jumpPower
                playerY = platTop - playerHeight
                return
            }
        }
    }

    private fun checkGameOver() {
        // Game over if player falls below visible screen
        if (playerY > screenHeight + 100) {
            isGameOver = true
            saveHighScore(score)
            gameJob?.cancel()
        }
    }

    // Load high scores from SharedPreferences
    private fun loadHighScores() {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("doodle_jump_prefs", Context.MODE_PRIVATE)
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
            // Use removeAt instead of removeLast for better compatibility
            highScores.removeAt(highScores.size - 1)
        }

        // Save to SharedPreferences
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("doodle_jump_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            for (i in highScores.indices) {
                editor.putInt("high_score_$i", highScores[i])
            }

            editor.apply()
        }
    }

    fun startGame() {
        if (gameJob?.isActive == true) return

        isGameOver = false
        gameJob = gameScope.launch {
            while (true) {
                updateGame()
                delay(16) // ~60 FPS
            }
        }
    }

    fun stopGame() {
        gameJob?.cancel()
        gameJob = null
    }

    // Restart game after game over
    fun restartGame() {
        stopGame()
        resetPlayerPosition()
        isGameOver = false
        startGame()
    }
}
