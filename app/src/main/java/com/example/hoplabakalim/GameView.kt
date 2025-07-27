package com.example.hoplabakalim

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.util.ArrayList
import java.util.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable, SensorEventListener {

    private var holder: SurfaceHolder = getHolder()
    private var gameThread: Thread? = null
    @Volatile private var running: Boolean = false

    private lateinit var player: Player
    private lateinit var platforms: MutableList<Platform>
    private var score: Int = 0
    private var gameStarted: Boolean = false
    private var gameOver: Boolean = false

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val random: Random = Random()

    private lateinit var scoreLabel: TextView
    private lateinit var gameOverLabel: TextView
    private lateinit var retryButton: Button
    private lateinit var startLabel: TextView

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var accelerometerAvailable: Boolean = false

    private val keyboardHandler: Handler = Handler()
    private var keyboardRunnable: Runnable? = null
    private val KEYBOARD_MOVE_SPEED = 5
    private var moveLeft: Boolean = false
    private var moveRight: Boolean = false

    // Resimler için Bitmap nesneleri
    private lateinit var playerBitmap: Bitmap
    private lateinit var platformBitmap: Bitmap
    private lateinit var backgroundBitmap: Bitmap

    init {
        holder.addCallback(this)

        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // Resimleri yükle
        loadBitmaps()

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.let {
            accelerometer = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                accelerometerAvailable = true
            }
        }

        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }

    private fun loadBitmaps() {
        // Resimleri drawable klasöründen yükle
        // Resim adlarını kendi eklediğiniz dosya adlarıyla değiştirin (örn: R.drawable.my_player_image)
        playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.player)
        platformBitmap = BitmapFactory.decodeResource(resources, R.drawable.platform)
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)

        // Arka plan resmini ekran boyutuna göre ölçekle
        backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, screenWidth, screenHeight, true)
    }

    fun setUiElements(scoreLabel: TextView, gameOverLabel: TextView, retryButton: Button, startLabel: TextView) {
        this.scoreLabel = scoreLabel
        this.gameOverLabel = gameOverLabel
        this.retryButton = retryButton
        this.startLabel = startLabel
        updateUiVisibility()
    }

    private fun updateUiVisibility() {
        post {
            if (!gameStarted) {
                startLabel.visibility = View.VISIBLE
                gameOverLabel.visibility = View.GONE
                retryButton.visibility = View.GONE
            } else if (gameOver) {
                startLabel.visibility = View.GONE
                gameOverLabel.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
            } else {
                startLabel.visibility = View.GONE
                gameOverLabel.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
            scoreLabel.text = "Score: $score"
        }
    }

    private fun initializeGame() {
        score = 0
        gameStarted = false
        gameOver = false

        // Oyuncuyu başlangıç konumuna yerleştir (resmi kullanarak)
        // Karakter boyutunu ekran genişliğinin belirli bir oranına göre ayarla
        val playerWidth = screenWidth / 6f // Karakteri daha büyük yapmak için 8f yerine 6f kullanıldı
        val playerHeight = screenWidth / 6f // Kare bir karakter için genişlikle aynı
        player = Player(screenWidth / 2 - playerWidth / 2, 100f, playerWidth, playerHeight, playerBitmap)

        // Platformları oluştur (resmi kullanarak)
        platforms = mutableListOf()
        val platformWidth = screenWidth / 4f // Platform genişliğini ekran genişliğinin 1/4'ü yap
        val platformHeight = screenHeight / 40f // Platform yüksekliğini ekran yüksekliğinin 1/40'ı yap
        for (i in 0 until 5) {
            val p = Platform(
                random.nextInt(screenWidth - platformWidth.toInt()).toFloat(),
                (i * (screenHeight / 5) + 50).toFloat(), // Platformlar arası dikey boşluğu ayarla
                platformWidth, platformHeight, platformBitmap)
            platforms.add(p)
        }
        updateUiVisibility()
    }

    fun startGame() {
        gameStarted = true
        player.jump(8f)
        updateUiVisibility()
    }

    fun retryGame() {
        initializeGame()
        if (accelerometerAvailable) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        moveLeft = false
        moveRight = false
    }

    private fun endGame() {
        gameOver = true
        if (accelerometerAvailable) {
            sensorManager?.unregisterListener(this)
        }
        updateUiVisibility()
    }

    private fun update() {
        if (!gameStarted || gameOver) {
            return
        }

        player.move(screenWidth)

        checkPlatformCollision()

        movePlatforms()

        if (player.getRect().top > screenHeight) {
            endGame()
        }
    }

    private fun checkPlatformCollision() {
        for (platform in platforms) {
            if (player.velocityY < 0 && RectF.intersects(player.getRect(), platform.getRect())) {
                if (player.getRect().bottom >= platform.getRect().top &&
                    player.getRect().bottom <= platform.getRect().top + 20 &&
                    player.getRect().top < platform.getRect().top) {
                    player.setPosition(player.getRect().left, platform.getRect().top - player.getHeight())
                    player.velocityY = 0f
                    player.jump(10f)
                }
            }
        }
    }

    private fun movePlatforms() {
        for (platform in platforms) {
            platform.setY(platform.getY() + 2)
            if (platform.getY() > screenHeight) {
                platform.setY(-platform.getHeight() - random.nextInt(100).toFloat())
                platform.setX(random.nextInt(screenWidth - platform.getWidth().toInt()).toFloat())
                score++
                updateUiVisibility()
            }
        }
    }

    private fun draw() {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                // Arka planı çiz
                canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)

                player.draw(canvas)

                for (platform in platforms) {
                    platform.draw(canvas)
                }
            }
        } finally {
            canvas?.let {
                holder.unlockCanvasAndPost(it)
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initializeGame()
        running = true
        gameThread = Thread(this)
        gameThread?.start()
        if (accelerometerAvailable) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        startKeyboardMovement()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Ekran boyutu değiştiğinde arka plan resmini yeniden ölçekle
        backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, width, height, true)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        running = false
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        if (accelerometerAvailable) {
            sensorManager?.unregisterListener(this)
        }
        stopKeyboardMovement()
    }

    override fun run() {
        var lastFrameTime = System.currentTimeMillis()
        while (running) {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastFrameTime

            if (elapsedTime >= (1000 / 60)) {
                update()
                draw()
                lastFrameTime = currentTime
            }
        }
    }

    fun pauseGame() {
        running = false
        if (accelerometerAvailable) {
            sensorManager?.unregisterListener(this)
        }
        stopKeyboardMovement()
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    fun resumeGame() {
        running = true
        gameThread = Thread(this)
        gameThread?.start()
        if (accelerometerAvailable) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        startKeyboardMovement()
    }

    fun stopGame() {
        running = false
        if (accelerometerAvailable) {
            sensorManager?.unregisterListener(this)
        }
        stopKeyboardMovement()
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (!gameStarted) {
                startGame()
            } else if (!gameOver) {
                player.jump(8f)
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            if (gameStarted && !gameOver) {
                val x = event.values[0]
                player.velocityX = -x * 5f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (gameStarted && !gameOver) {
            if (keyCode == KeyEvent.KEYCODE_A) {
                moveLeft = true
                return true
            } else if (keyCode == KeyEvent.KEYCODE_D) {
                moveRight = true
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (gameStarted && !gameOver) {
            if (keyCode == KeyEvent.KEYCODE_A) {
                moveLeft = false
                player.velocityX = 0f
                return true
            } else if (keyCode == KeyEvent.KEYCODE_D) {
                moveRight = false
                player.velocityX = 0f
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun startKeyboardMovement() {
        if (keyboardRunnable == null) {
            keyboardRunnable = Runnable {
                if (gameStarted && !gameOver) {
                    if (moveLeft) {
                        player.velocityX = -KEYBOARD_MOVE_SPEED.toFloat()
                    } else if (moveRight) {
                        player.velocityX = KEYBOARD_MOVE_SPEED.toFloat()
                    } else {
                        player.velocityX = 0f
                    }
                }
                keyboardHandler.postDelayed(keyboardRunnable!!, 16)
            }
            keyboardHandler.post(keyboardRunnable!!)
        }
    }

    private fun stopKeyboardMovement() {
        if (keyboardRunnable != null) {
            keyboardHandler.removeCallbacks(keyboardRunnable!!)
            keyboardRunnable = null
        }
    }
}
