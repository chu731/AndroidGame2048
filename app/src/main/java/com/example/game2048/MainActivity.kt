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
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var game: Game2048
    private lateinit var scoreTextView: TextView
    private lateinit var bestScoreTextView: TextView
    private lateinit var boardGridLayout: GridLayout
    private lateinit var gestureDetector: GestureDetector
    private lateinit var mediaPlayer: MediaPlayer
    private val gameHistory = mutableListOf<Game2048>()
    private var undoClickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = MediaPlayer.create(this, R.raw.merge_sound)

        boardGridLayout = findViewById(R.id.boardGridLayout)
        scoreTextView = findViewById(R.id.scoreTextView)
        bestScoreTextView = findViewById(R.id.bestScoreTextView)
        game = Game2048()

        saveGameState()

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

                if (game.isGameOver()) {
                    showGameOverDialog()
                }
                saveGameState()
                return true
            }
        })

        updateUI()

        val menuButton: Button = findViewById(R.id.menuButton)
        menuButton.setOnClickListener {
            showGameOptionsDialog()
        }

        val leaderboardButton: Button = findViewById(R.id.leaderboardButton)
        leaderboardButton.setOnClickListener {
            showLeaderboard()
        }

        val undoButton: Button = findViewById(R.id.undoButton)
        undoButton.setOnClickListener {
            undoMove() 
        }
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
                showGameOptionsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 顯示menu對話框
    private fun showGameOptionsDialog() {
        val options = arrayOf("繼續遊戲", "重新開始", "退出遊戲")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("選項")
        builder.setItems(options) { dialog: DialogInterface, which: Int ->
            when (which) {
                0 -> continueGame() 
                1 -> restartGame() 
                2 -> exitGame() 
                3 -> undoMove() 
            }
        }
        builder.show()
    }

    private fun continueGame() {}

    private fun restartGame() {
        game.resetGame()  
        gameHistory.clear()  
        undoClickCount = 0  
        updateUI() 
        saveGameState() 
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

        if (game.checkFor2048()) {
            showWinGameOverDialog()
        }

        if (game.mergeOccurred) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
            mediaPlayer.start()
        }
    }

    private fun saveGameState() {
        val gameState = game.copy()
        gameHistory.add(gameState) 
        Log.d("GameHistory", "Saved game state: ${gameHistory.size}")
    }

    private fun undoMove() {
        if (gameHistory.isNotEmpty() && undoClickCount < 4) {
            game = gameHistory.removeAt(gameHistory.size - 1)
            undoClickCount++ 
            Log.d("Undo", "Undo move: undoClickCount = $undoClickCount")
            updateUI()
        } else {
            if (gameHistory.isEmpty()) {
                Toast.makeText(this, "沒有步驟可以返回", Toast.LENGTH_SHORT).show()
            } else if (undoClickCount >= 4) {
                Toast.makeText(this, "最多只能返回三步", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("遊戲結束")
        builder.setMessage("遊戲已結束，是否重新開始？")

        val input = EditText(this)
        input.hint = "請輸入名字"
        input.setText("Player")
        builder.setView(input)

        builder.setPositiveButton("重新開始") { _, _ ->
            game.resetGame()
            updateUI()
        }

        builder.setNegativeButton("退出") { _, _ -> finish() }

        builder.setNeutralButton("儲存到排行榜") { _, _ ->
            val playerName = input.text.toString().takeIf { it.isNotBlank() } ?: "Player"
            Log.d("Game", "Saving score for player: $playerName with score: ${game.score}")
            addToLeaderboard(playerName, game.score)
            showLeaderboard()

            game.resetGame()
            updateUI()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showWinGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("遊戲結束")
        builder.setMessage("恭喜您達到2048！是否重新開始？")

        val input = EditText(this)
        input.hint = "請輸入您的名字"
        builder.setView(input) 

        builder.setPositiveButton("重新開始") { _, _ ->
            restartGame()
        }

        builder.setNeutralButton("儲存到排行榜") { _, _ ->
            val playerName = input.text.toString().takeIf { it.isNotBlank() } ?: "Player" 
            Log.d("Game", "Saving score for player: $playerName with score: ${game.score}")
            addToLeaderboard(playerName, game.score)
            showLeaderboard() 
        }

        builder.setNegativeButton("退出") { _, _ ->
            finish()
        }

        builder.setCancelable(false)
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

    @Serializable
    data class Player(val name: String, val score: Int) {
        companion object {
        }
    }

    @Serializable
    data class Leaderboard(var players: MutableList<Player>)

    fun loadLeaderboard(): Leaderboard {
        val file = File(filesDir, "leaderboard.json")
        return try {
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString(jsonString)
            } else {
                Leaderboard(mutableListOf())
            }
        } catch (e: Exception) {
            Log.e("Leaderboard", "Error loading leaderboard: ${e.message}")
            Leaderboard(mutableListOf())
        }
    }

    fun saveLeaderboard(leaderboard: Leaderboard) {
        try {
            val jsonString = Json.encodeToString(leaderboard)
            val file = File(filesDir, "leaderboard.json")

            file.writeText(jsonString)

            Log.d("Leaderboard", "Leaderboard saved: $jsonString")
        } catch (e: Exception) {
            Log.e("Leaderboard", "Error saving leaderboard: ${e.message}")
        }
    }

    private fun addToLeaderboard(playerName: String, score: Int) {
        val leaderboard = loadLeaderboard() 
        leaderboard.players.add(Player(playerName, score))
        leaderboard.players.sortByDescending { it.score }
        if (leaderboard.players.size > 10) {
            leaderboard.players = leaderboard.players.take(10).toMutableList()
        }

        saveLeaderboard(leaderboard)
    }
    fun showLeaderboard() {
        val leaderboard = loadLeaderboard()
        val builder = StringBuilder("=== 排行榜 ===\n")
        leaderboard.players.forEachIndexed { index, player ->
            builder.append("${index + 1}. ${player.name} - ${player.score}\n")
        }

        AlertDialog.Builder(this)
            .setTitle("排行榜")
            .setMessage(builder.toString())
            .setPositiveButton("確定", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
