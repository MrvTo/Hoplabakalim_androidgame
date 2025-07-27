package com.example.hoplabakalim

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var scoreLabel: TextView
    private lateinit var gameOverLabel: TextView
    private lateinit var retryButton: Button
    private lateinit var startLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)

        gameView = findViewById(R.id.gameView)
        scoreLabel = findViewById(R.id.scoreLabel)
        gameOverLabel = findViewById(R.id.gameOverLabel)
        retryButton = findViewById(R.id.retryButton)
        startLabel = findViewById(R.id.startLabel)

        gameView.setUiElements(scoreLabel, gameOverLabel, retryButton, startLabel)

        retryButton.setOnClickListener {
            gameView.retryGame()
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView.stopGame()
    }
}