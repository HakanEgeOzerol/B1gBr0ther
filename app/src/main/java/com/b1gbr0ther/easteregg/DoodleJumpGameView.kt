package com.b1gbr0ther.easteregg

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class DoodleJumpGameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: DoodleJumpGameThread? = null
    private var player: Player
    private var platforms: MutableList<Platform>
    private var camera: Camera
    private var gameState: GameState = GameState.PLAYING
    private var score: Int = 0
    private var highScore: Int = 0
    
    // Screen dimensions
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    
    // Paint objects
    private val playerPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val platformPaint = Paint().apply {
        color = Color.parseColor("#3CBC05")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#87CEEB")
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    private val smallTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    enum class GameState {
        PLAYING, GAME_OVER
    }

    init {
        holder.addCallback(this)
        player = Player(0f, 0f)
        platforms = mutableListOf()
        camera = Camera()
        
        // Load high score from preferences
        val prefs = context.getSharedPreferences("DoodleJump", Context.MODE_PRIVATE)
        highScore = prefs.getInt("highScore", 0)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        
        // Initialize game objects
        player = Player(screenWidth / 2, screenHeight - 200f)
        generateInitialPlatforms()
        
        gameThread = DoodleJumpGameThread(holder, this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.setRunning(false)
        gameThread?.join()
    }

    private fun generateInitialPlatforms() {
        platforms.clear()
        
        // Generate initial platforms
        for (i in 0 until 20) {
            val x = Random.nextFloat() * (screenWidth - 120f)
            val y = screenHeight - (i * 150f)
            platforms.add(Platform(x, y))
        }
    }

    fun update() {
        if (gameState == GameState.PLAYING) {
            player.update()
            
            // Check platform collisions
            checkPlatformCollisions()
            
            // Update camera to follow player
            camera.update(player.y)
            
            // Generate new platforms as player goes higher
            generateNewPlatforms()
            
            // Remove platforms that are too far below
            platforms.removeAll { it.y > camera.y + screenHeight + 200 }
            
            // Check if player fell too far
            if (player.y > camera.y + screenHeight + 200) {
                gameOver()
            }
            
            // Update score based on height
            val newScore = maxOf(0, (-player.y / 10).toInt())
            if (newScore > score) {
                score = newScore
            }
        }
    }

    private fun checkPlatformCollisions() {
        if (player.velocityY > 0) { // Only check when falling
            for (platform in platforms) {
                if (player.x + player.width > platform.x &&
                    player.x < platform.x + platform.width &&
                    player.y + player.height > platform.y &&
                    player.y + player.height < platform.y + platform.height + 20) {
                    
                    player.jump()
                    break
                }
            }
        }
    }

    private fun generateNewPlatforms() {
        val highestPlatform = platforms.minByOrNull { it.y }
        if (highestPlatform != null && highestPlatform.y > camera.y - screenHeight) {
            for (i in 0 until 10) {
                val x = Random.nextFloat() * (screenWidth - 120f)
                val y = highestPlatform.y - (i + 1) * 150f
                platforms.add(Platform(x, y))
            }
        }
    }

    private fun gameOver() {
        gameState = GameState.GAME_OVER
        
        // Save high score
        if (score > highScore) {
            highScore = score
            val prefs = context.getSharedPreferences("DoodleJump", Context.MODE_PRIVATE)
            prefs.edit().putInt("highScore", highScore).apply()
        }
    }

    fun render(canvas: Canvas) {
        // Clear screen with background
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, backgroundPaint)
        
        // Save canvas state
        canvas.save()
        
        // Apply camera transformation
        canvas.translate(0f, camera.y)
        
        // Draw platforms
        for (platform in platforms) {
            if (platform.y > camera.y - 100 && platform.y < camera.y + screenHeight + 100) {
                canvas.drawRoundRect(
                    platform.x, platform.y,
                    platform.x + platform.width, platform.y + platform.height,
                    10f, 10f, platformPaint
                )
            }
        }
        
        // Draw player
        canvas.drawRoundRect(
            player.x, player.y,
            player.x + player.width, player.y + player.height,
            15f, 15f, playerPaint
        )
        
        // Restore canvas state
        canvas.restore()
        
        // Draw UI (score, game over screen)
        drawUI(canvas)
    }

    private fun drawUI(canvas: Canvas) {
        // Draw score
        canvas.drawText("Score: $score", screenWidth / 2, 100f, textPaint)
        canvas.drawText("Best: $highScore", screenWidth / 2, 160f, smallTextPaint)
        
        if (gameState == GameState.GAME_OVER) {
            // Draw game over screen
            val overlayPaint = Paint().apply {
                color = Color.argb(128, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
            
            canvas.drawText("Game Over!", screenWidth / 2, screenHeight / 2 - 100f, textPaint)
            canvas.drawText("Final Score: $score", screenWidth / 2, screenHeight / 2 - 30f, smallTextPaint)
            canvas.drawText("Tap to restart", screenWidth / 2, screenHeight / 2 + 50f, smallTextPaint)
            canvas.drawText("Back button to exit", screenWidth / 2, screenHeight / 2 + 100f, smallTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (gameState == GameState.GAME_OVER) {
                    restartGame()
                } else {
                    // Move player left or right based on touch position
                    if (event.x < screenWidth / 2) {
                        player.moveLeft()
                    } else {
                        player.moveRight()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun restartGame() {
        gameState = GameState.PLAYING
        score = 0
        player = Player(screenWidth / 2, screenHeight - 200f)
        camera = Camera()
        generateInitialPlatforms()
    }

    fun pause() {
        gameThread?.setRunning(false)
    }

    fun resume() {
        if (gameThread?.isAlive != true) {
            gameThread = DoodleJumpGameThread(holder, this)
            gameThread?.start()
        }
    }

    // Inner classes
    inner class Player(var x: Float, var y: Float) {
        val width = 50f
        val height = 50f
        var velocityX = 0f
        var velocityY = 0f
        private val gravity = 0.8f
        private val jumpVelocity = -20f
        private val friction = 0.9f
        private val moveSpeed = 8f

        fun update() {
            // Apply gravity
            velocityY += gravity
            
            // Apply friction to horizontal movement
            velocityX *= friction
            
            // Update position
            x += velocityX
            y += velocityY
            
            // Keep player on screen horizontally (wrap around)
            if (x < -width) {
                x = screenWidth
            } else if (x > screenWidth) {
                x = -width
            }
        }

        fun jump() {
            velocityY = jumpVelocity
        }

        fun moveLeft() {
            velocityX = -moveSpeed
        }

        fun moveRight() {
            velocityX = moveSpeed
        }
    }

    inner class Platform(val x: Float, val y: Float) {
        val width = 120f
        val height = 20f
    }

    inner class Camera {
        var y = 0f
        private val followSpeed = 0.1f

        fun update(playerY: Float) {
            val targetY = -playerY + screenHeight * 0.7f
            y += (targetY - y) * followSpeed
        }
    }

    inner class DoodleJumpGameThread(private val surfaceHolder: SurfaceHolder, private val gameView: DoodleJumpGameView) : Thread() {
        private var running = false
        private val targetFPS = 60
        private val targetTime = 1000 / targetFPS

        fun setRunning(running: Boolean) {
            this.running = running
        }

        override fun run() {
            running = true
            while (running) {
                val startTime = System.currentTimeMillis()
                
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        gameView.update()
                        gameView.render(canvas)
                    }
                } finally {
                    canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
                }
                
                val elapsedTime = System.currentTimeMillis() - startTime
                val sleepTime = targetTime - elapsedTime
                if (sleepTime > 0) {
                    try {
                        sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        running = false
                    }
                }
            }
        }
    }
} 