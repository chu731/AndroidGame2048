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
    private val gameHistory = mutableListOf<Game2048>()// 用來儲存遊戲的歷史狀態
    private var undoClickCount = 0  // 記錄「回到上一步」的次數

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = MediaPlayer.create(this, R.raw.merge_sound)

        // 初始化 UI 元件
        boardGridLayout = findViewById(R.id.boardGridLayout)
        scoreTextView = findViewById(R.id.scoreTextView)
        bestScoreTextView = findViewById(R.id.bestScoreTextView)
        game = Game2048()

        // 儲存初始遊戲狀態
        saveGameState()

        // 初始化移動偵測
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
                // 檢查遊戲是否結束
                if (game.isGameOver()) {
                    showGameOverDialog()
                }
                // 保存遊戲狀態
                saveGameState()
                return true
            }
        })

        updateUI()

        // 設定Menu按鈕
        val menuButton: Button = findViewById(R.id.menuButton)
        menuButton.setOnClickListener {
            showGameOptionsDialog()
        }

        // 設定排行榜按鈕
        val leaderboardButton: Button = findViewById(R.id.leaderboardButton)
        leaderboardButton.setOnClickListener {
            showLeaderboard()
        }


        // 設定回到上一步的按鈕
        val undoButton: Button = findViewById(R.id.undoButton)
        undoButton.setOnClickListener {
            undoMove()  // 當按下此按鈕時，回到上一步
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let { gestureDetector.onTouchEvent(it) } == true || super.onTouchEvent(event)
    }

    // menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> {
                // 測試點擊menu
                Log.d("Menu", "Menu button clicked!")
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
                0 -> continueGame() // 繼續遊戲
                1 -> restartGame() // 重新開始
                2 -> exitGame() // 退出遊戲
                3 -> undoMove() // 回到上一步
            }
        }
        builder.show()
    }

    // 繼續遊戲
    private fun continueGame() {}

    // 重新開始遊戲
    private fun restartGame() {
        game.resetGame()  // 重置遊戲
        gameHistory.clear()  // 清空遊戲歷史紀錄
        undoClickCount = 0  // 重設回到上一步的次數
        updateUI()  // 更新 UI 顯示
        saveGameState()  // 儲存初始的遊戲狀態
    }

    // 退出遊戲
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

        // 檢查是否達到2048
        if (game.checkFor2048()) {
            showWinGameOverDialog()  // 顯示遊戲結束對話框
        }

        // 只有在合併發生時才播放音效
        if (game.mergeOccurred) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
            mediaPlayer.start()
        }
    }

    private fun saveGameState() {
        val gameState = game.copy()  // 複製遊戲當前狀態
        gameHistory.add(gameState)   // 儲存狀態到歷史紀錄
        Log.d("GameHistory", "Saved game state: ${gameHistory.size}")
    }

    // 回到上一步
    private fun undoMove() {
        // 確保有歷史紀錄且還未超過最大回溯次數
        if (gameHistory.isNotEmpty() && undoClickCount < 4) {
            // 還原遊戲狀態，從歷史紀錄中取出
            game = gameHistory.removeAt(gameHistory.size - 1)
            undoClickCount++  // 增加回到上一步的次數
            Log.d("Undo", "Undo move: undoClickCount = $undoClickCount")
            updateUI()  // 更新 UI 顯示
        } else {
            // 如果沒有更多步驟可以回溯，顯示提示訊息
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

        // 在遊戲結束後顯示一個對話框讓玩家輸入名字並將分數儲存到排行榜
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
            addToLeaderboard(playerName, game.score) // 將分數和玩家名字儲存到排行榜
            showLeaderboard() // 顯示排行榜

            // 直接重新開始遊戲
            game.resetGame()
            updateUI()
        }
        builder.setCancelable(false) // 禁止點擊外部區域關閉對話框
        builder.show()
    }

    private fun showWinGameOverDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("遊戲結束")
        builder.setMessage("恭喜您達到2048！是否重新開始？")

        // 顯示輸入框讓玩家輸入名字
        val input = EditText(this)
        input.hint = "請輸入您的名字"
        builder.setView(input)  // 顯示輸入框

        // 按鈕順序設置
        builder.setPositiveButton("重新開始") { _, _ ->
            restartGame()  // 重新開始遊戲
        }

        builder.setNeutralButton("儲存到排行榜") { _, _ ->
            val playerName = input.text.toString().takeIf { it.isNotBlank() } ?: "Player" // 確保名字不為空
            Log.d("Game", "Saving score for player: $playerName with score: ${game.score}")
            addToLeaderboard(playerName, game.score)  // 儲存到排行榜
            showLeaderboard()  // 顯示排行榜
        }

        builder.setNegativeButton("退出") { _, _ ->
            finish()  // 退出遊戲
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

    // 儲存排行榜資料
    @Serializable
    data class Player(val name: String, val score: Int) {
        // Define a companion object for the serializer method to be generated
        companion object {
            // The serializer() method will be automatically generated by the Kotlin Serialization plugin
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
                Leaderboard(mutableListOf())  // 若文件不存在，返回一個空的排行榜
            }
        } catch (e: Exception) {
            // 如果讀取或解析失敗，打印錯誤信息並返回空的排行榜
            Log.e("Leaderboard", "Error loading leaderboard: ${e.message}")
            Leaderboard(mutableListOf())  // 返回空的排行榜
        }
    }

    fun saveLeaderboard(leaderboard: Leaderboard) {
        try {
            val jsonString = Json.encodeToString(leaderboard)
            val file = File(filesDir, "leaderboard.json")

            // 儲存數據
            file.writeText(jsonString)

            // 確認儲存是否成功
            Log.d("Leaderboard", "Leaderboard saved: $jsonString")
        } catch (e: Exception) {
            // 如果儲存失敗，打印錯誤信息
            Log.e("Leaderboard", "Error saving leaderboard: ${e.message}")
        }
    }

    private fun addToLeaderboard(playerName: String, score: Int) {
        val leaderboard = loadLeaderboard() // 加载排行榜
        leaderboard.players.add(Player(playerName, score)) // 添加玩家和分数
        leaderboard.players.sortByDescending { it.score } // 排序
        if (leaderboard.players.size > 10) {
            leaderboard.players = leaderboard.players.take(10).toMutableList() // 保留前十名
        }

        saveLeaderboard(leaderboard) // 保存排行榜
    }

    // 顯示排行榜
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