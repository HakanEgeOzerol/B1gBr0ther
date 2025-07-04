package com.b1gbr0ther.easteregg

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b1gbr0ther.util.LocaleHelper
import com.b1gbr0ther.R
import com.b1gbr0ther.util.ThemeManager

class DoodleJumpActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val gameViewModel = GameViewModel()

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

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        gameViewModel.setContext(this)

        setContent {
            GameScreen(gameViewModel, this@DoodleJumpActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        isGameActive = true
        // Resume game only if it's not game over
        if (!gameViewModel.isGameOver) {
            gameViewModel.resumeGame()
        }
        // Register accelerometer listener with safety check
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        isGameActive = false
        gameViewModel.pauseGame()
        // Disable sensor when app is paused
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isGameActive = false
        gameViewModel.stopGame()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Use X value of accelerometer to move player
                // Multiply by -1 so tilting right moves player right
                val tiltValue = -it.values[0] * 1.5f
                gameViewModel.updatePlayerHorizontalPosition(tiltValue)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for our implementation
    }
}

@Composable
fun GameScreen(gameViewModel: GameViewModel, context: Context) {
    val playerX = gameViewModel.playerX
    val playerY = gameViewModel.playerY
    val density = LocalDensity.current
    val isGameOver = gameViewModel.isGameOver
    val isPaused = gameViewModel.isPaused
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val score = gameViewModel.score

    // Generate initial platforms with proper dp to px conversion
    fun generateInitialPlatforms(
        screenWidthPx: Float,
        screenHeightPx: Float,
        platformWidthPx: Float
    ) {
        // Central starting platform
        val platformXPx = (screenWidthPx / 2) - (platformWidthPx / 2)
        val platformYPx = screenHeightPx * 0.8f
        val platformHeightPx = with(density) { 20.dp.toPx() }
        gameViewModel.addPlatform(platformXPx, platformYPx, platformWidthPx, platformHeightPx)

        // Initial platforms
        for (i in 1..8) {
            val newPlatformY = screenHeightPx * (0.8f - i * 0.1f)
            gameViewModel.generateRandomPlatform(newPlatformY)
        }
    }

    // Start game and configure platforms
    LaunchedEffect(Unit) {
        with(density) {
            val screenWidthPx = screenWidth.toPx()
            val screenHeightPx = screenHeight.toPx()
            val platformWidthPx = 90.dp.toPx()
            val platformHeightPx = 20.dp.toPx()

            gameViewModel.setScreenDimensions(
                screenWidthPx,
                screenHeightPx,
                platformWidthPx,
                platformHeightPx
            )
            gameViewModel.clearPlatforms()
            generateInitialPlatforms(screenWidthPx, screenHeightPx, platformWidthPx)
        }

        gameViewModel.resetPlayerPosition()
        gameViewModel.startGame()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.back),
            contentDescription = context.getString(R.string.game_background),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        if (!isPaused && !isGameOver) {
            Button(
                onClick = { gameViewModel.pauseGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.game_pause_button)
                ),
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 40.dp)
            ) {
                Text(
                    text = context.getString(R.string.game_pause),
                    color = colorResource(id = R.color.game_pause_text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Improved score display with better visibility
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = 50.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = context.getString(R.string.game_score, score),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        gameViewModel.platforms.forEach { platform ->
            SpriteSheetImage(
                spriteSheetResId = R.drawable.game_tiles,
                srcX = 0,
                srcY = 0,
                srcWidth = 60,
                srcHeight = 17,
                modifier = Modifier
                    .size(
                        width = with(density) { platform.width.toDp() },
                        height = with(density) { platform.height.toDp() }
                    )
                    .offset(
                        x = with(density) { platform.x.toDp() },
                        y = with(density) { platform.y.toDp() }
                    )
            )
        }

        if (!isGameOver) {
            val spriteResId = if (gameViewModel.isFacingRight) R.drawable.doodle_right else R.drawable.doodle_left
            Image(
                painter = painterResource(id = spriteResId),
                contentDescription = context.getString(R.string.game_player),
                modifier = Modifier
                    .size(width = 80.dp, height = 80.dp)
                    .offset(
                        x = with(density) { playerX.toDp() },
                        y = with(density) { playerY.toDp() }
                    )
            )
        }

        if (isGameOver) {
            GameOverScreen(
                score = score,
                highScores = gameViewModel.highScores.toList(),
                onRestart = { gameViewModel.restartGame() },
                onExit = { (context as? Activity)?.finish() },
                context = context
            )
        }

        if (isPaused) {
            PauseMenu(
                gameViewModel = gameViewModel,
                onExit = { (context as? Activity)?.finish() },
                context = context
            )
        }
    }
}

@Composable
fun PauseMenu(
    gameViewModel: GameViewModel,
    onExit: () -> Unit,
    context: Context
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
                text = context.getString(R.string.game_paused_title),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            // Resume button
            Button(
                onClick = { gameViewModel.resumeGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.game_play_again_button)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = context.getString(R.string.game_resume),
                    color = Color.White,
                    fontSize = 22.sp
                )
            }

            // Exit button
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.game_exit_button)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = context.getString(R.string.game_exit_to_app),
                    color = Color.White,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
fun GameOverScreen(
    score: Int,
    highScores: List<Int>,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.game_overlay_background))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = context.getString(R.string.game_over),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorResource(id = R.color.game_over_text),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = context.getString(R.string.game_your_score, score),
                fontSize = 28.sp,
                color = colorResource(id = R.color.game_score_display)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // High Scores Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = context.getString(R.string.game_high_scores),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.game_high_scores_title)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (highScores.isEmpty()) {
                    Text(
                        text = context.getString(R.string.game_no_scores),
                        fontSize = 18.sp,
                        color = colorResource(id = R.color.game_no_scores_text)
                    )
                } else {
                    highScores.forEachIndexed { index, highScore ->
                        Text(
                            text = "${index + 1}. $highScore",
                            fontSize = 18.sp,
                            color = if (highScore == score) colorResource(id = R.color.game_current_score_highlight)
                            else colorResource(id = R.color.game_other_scores)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.game_play_again_button)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = context.getString(R.string.game_play_again),
                    color = colorResource(id = R.color.game_button_text),
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.game_exit_button)
                ),
                modifier = Modifier.size(width = 200.dp, height = 60.dp)
            ) {
                Text(
                    text = context.getString(R.string.game_exit),
                    color = colorResource(id = R.color.game_button_text),
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
fun SpriteSheetImage(
    spriteSheetResId: Int,
    srcX: Int,
    srcY: Int,
    srcWidth: Int,
    srcHeight: Int,
    modifier: Modifier = Modifier
) {
    val imageBitmap = ImageBitmap.imageResource(id = spriteSheetResId)

    Box(
        modifier = modifier
            .drawWithCache {
                onDrawBehind {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(srcWidth, srcHeight),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                }
            }
    )
}