package com.example.game2048

import Game2048
import android.content.DialogInterface
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.DisplayMetrics
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
        modeTextView = findViewById(R.id.modeTextView)

        showBoardSizeDialog()

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
            showLeaderboard(game.size) 
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

    private fun showBoardSizeDialog() {
        val options = arrayOf("3x3", "4x4", "5x5", "無限模式")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("選擇遊玩模式")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> initializeGame(3) 
                1 -> initializeGame(4) 
                2 -> initializeGame(5) 
                3 -> showInfiniteModeSizeDialog()
            }
        }
        builder.show()
    }

    private fun initializeGame(size: Int) {
        if (!::game.isInitialized || game.size != size) {
            game = Game2048(size)
            game.resetGame()
        }
        modeTextView.text = "Mode: ${size}x${size}"
        updateUI()
    }

    private fun showInfiniteModeSizeDialog() {
        val options = arrayOf("3x3", "4x4", "5x5")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("選擇無限模式的格子大小")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> startInfiniteMode(3)
                1 -> startInfiniteMode(4)
                2 -> startInfiniteMode(5)
            }
        }
        builder.show()
    }

    private fun startInfiniteMode(size: Int) {
        game = Game2048(size) 
        game.resetGame()
        game.isInfiniteMode = true
        updateUI()
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

    private fun continueGame() {}

    private fun restartGame() {
        game.resetGame()
        gameHistory.clear() 
        undoClickCount = 0 
        updateUI()
        modeTextView.text = "Mode: ${game.size}x${game.size}"

        showBoardSizeDialog()
    }

    private fun exitGame() {
        finish()
    }

    private fun updateUI() {
        scoreTextView.text = " ${game.score}"
        bestScoreTextView.text = " ${game.bestScore}"

        boardGridLayout.removeAllViews()
        boardGridLayout.rowCount = game.size
        boardGridLayout.columnCount = game.size

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val cardSize = ((screenWidth - 8 * (game.size + 1)) / game.size) * 0.9

        for (i in 0 until game.size) {
            for (j in 0 until game.size) {
                val cardView = CardView(this)
                cardView.layoutParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(i)
                    columnSpec = GridLayout.spec(j)
                    width = cardSize.toInt()
                    height = cardSize.toInt()
                    setMargins(4, 4, 4, 4)
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
    }

    private fun undoMove() {
        if (gameHistory.isNotEmpty() && undoClickCount < 4) {
            val previousGameState = gameHistory.removeAt(gameHistory.size - 1)
            val previousSize = previousGameState.size 
            game = previousGameState 
            game.size = previousSize 
            undoClickCount++
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
            addToLeaderboard(playerName, game.score, game.size)
            showLeaderboard(game.size)

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
            addToLeaderboard(playerName, game.score, game.size)
            showLeaderboard(game.size)
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
    data class Player(val name: String, val score: Int)

    @Serializable
    data class Leaderboard(var players: MutableList<Player> = mutableListOf())

    @Serializable
    data class AllLeaderboards(val leaderboards: MutableMap<Int, Leaderboard> = mutableMapOf())

    fun loadLeaderboard(): AllLeaderboards {
        val file = File(filesDir, "leaderboards.json")
        return try {
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString(jsonString)
            } else {
                AllLeaderboards()
            }
        } catch (e: Exception) {
            Log.e("Leaderboard", "Error loading leaderboard: ${e.message}")
            AllLeaderboards()
        }
    }

    fun saveLeaderboard(allLeaderboards: AllLeaderboards) {
        try {
            val jsonString = Json.encodeToString(allLeaderboards)
            val file = File(filesDir, "leaderboards.json")
            file.writeText(jsonString)
            Log.d("Leaderboard", "Leaderboard saved: $jsonString")
        } catch (e: Exception) {
            Log.e("Leaderboard", "Error saving leaderboard: ${e.message}")
        }
    }

    private fun addToLeaderboard(playerName: String, score: Int, size: Int) {
        val allLeaderboards = loadLeaderboard()

        if (!allLeaderboards.leaderboards.containsKey(size)) {
            allLeaderboards.leaderboards[size] = Leaderboard()
        }

        val leaderboard = allLeaderboards.leaderboards[size]!!
        leaderboard.players.add(Player(playerName, score))
        leaderboard.players.sortByDescending { it.score }

        if (leaderboard.players.size > 10) {
            leaderboard.players = leaderboard.players.take(10).toMutableList()
        }

        saveLeaderboard(allLeaderboards)
    }

    fun showLeaderboard(size: Int) {
        val allLeaderboards = loadLeaderboard()
        val leaderboard = allLeaderboards.leaderboards[size]

        val builder = StringBuilder("=== ${size}x${size} 排行榜 ===\n")
        leaderboard?.players?.forEachIndexed { index, player ->
            builder.append("${index + 1}. ${player.name} - ${player.score}\n")
        }

        AlertDialog.Builder(this)
            .setTitle("${size}x${size} 排行榜")
            .setMessage(builder.toString())
            .setPositiveButton("確定", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
