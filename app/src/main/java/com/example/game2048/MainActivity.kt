package com.example.game2048

import Game2048
import android.content.DialogInterface
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {
    private lateinit var game: Game2048
    private lateinit var scoreTextView: TextView
    private lateinit var bestScoreTextView: TextView
    private lateinit var boardGridLayout: GridLayout
    private lateinit var gestureDetector: GestureDetector
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = MediaPlayer.create(this, R.raw.merge_sound)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationIcon(R.drawable.menu)
        toolbar.setNavigationOnClickListener {
            showGameOptionsDialog()
        }

        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.title = ""

        toolbar.setBackgroundColor(Color.TRANSPARENT)

        boardGridLayout = findViewById(R.id.boardGridLayout)
        scoreTextView = findViewById(R.id.scoreTextView)
        bestScoreTextView = findViewById(R.id.bestScoreTextView)
        game = Game2048()

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)
                val threshold = 100
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (diffX > threshold) game.move("right")
                    else if (diffX < -threshold) game.move("left")
                } else {
                    if (diffY > threshold) game.move("down")
                    else if (diffY < -threshold) game.move("up")
                }
                updateUI()
                return true
            }
        })
        updateUI()
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let { gestureDetector.onTouchEvent(it) } == true || super.onTouchEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> {
                Log.d("Menu", "Menu button clicked!")
                showGameOptionsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showGameOptionsDialog() {
        val options = arrayOf("繼續遊戲", "重新開始", "退出遊戲")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("選項")
        builder.setItems(options) { dialog: DialogInterface, which: Int ->
            when (which) {
                0 -> continueGame()
                1 -> restartGame()
                2 -> exitGame() 
            }
        }
        builder.show()
    }

    private fun continueGame() {
    }
    private fun restartGame() {
        game.resetGame()
        updateUI()
    }
    private fun exitGame() {
        finish()
    }

    private fun updateUI() {
        scoreTextView.text = " ${game.score}"
        bestScoreTextView.text = " ${game.bestScore}"
        boardGridLayout.removeAllViews()

        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val cardView = CardView(this)
                cardView.layoutParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(i)
                    columnSpec = GridLayout.spec(j)
                    width = 190
                    height = 190
                    setMargins(8, 8, 8, 8)
                }

                val value = game.board[i][j]
                cardView.setCardBackgroundColor(getTileColor(value))
                cardView.radius = 10f
                cardView.cardElevation = 4f

                val tile = TextView(this)
                tile.text = if (value > 0) value.toString() else ""
                tile.textSize = 32f
                tile.setTextColor(Color.WHITE)
                tile.gravity = android.view.Gravity.CENTER

                cardView.addView(tile)
                boardGridLayout.addView(cardView)
            }
        }
        // 
        if (game.mergeOccurred) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.prepare() 
            }
            mediaPlayer.start()
        }
        if (game.isGameOver()) {
            showGameOverDialog()
        }
    }

    private fun showGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("遊戲結束")
        builder.setMessage("遊戲已結束，是否重新開始？")
        builder.setPositiveButton("重新開始") { _, _ -> game.resetGame(); updateUI() }
        builder.setNegativeButton("退出") { _, _ -> finish() }
        builder.show()
    }
    private fun getTileColor(value: Int): Int {
        return when (value) {
            2 -> Color.parseColor("#d7d1bd")
            4 -> Color.parseColor("#d0c79e")
            8 -> Color.parseColor("#ffb75e")
            16 -> Color.parseColor("#e58524")
            32 -> Color.parseColor("#f07a6a")
            64 -> Color.parseColor("#fbd05b")
            128 -> Color.parseColor("#FFD700")
            256 -> Color.parseColor("#95c65a")
            512 -> Color.parseColor("#76be7b")
            1024 -> Color.parseColor("#649e50")
            2048 -> Color.parseColor("#ffccad")
            else -> Color.DKGRAY
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
