class Game2048 {
    var board = Array(4) { IntArray(4) }
    var score = 0
    var bestScore = 0
    var mergeOccurred = false
    private var previousStates = mutableListOf<GameState>() // 儲存先前的遊戲狀態


    // 檢查是否達到2048
    fun checkFor2048(): Boolean {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == 2048) {
                    return true  // 如果板上有2048，則返回true
                }
            }
        }
        return false  // 如果沒有2048，則返回false
    }

    init {
        resetGame()
    }

    fun resetGame() {
        board = Array(4) { IntArray(4) }
        score = 0
        addRandomTile()
        addRandomTile()
        mergeOccurred = false
        previousStates.clear() // 清空上一步記錄
    }

    fun move(direction: String): Boolean {
        // 儲存當前遊戲狀態
        saveState()

        val oldBoard = board.map { it.clone() }.toTypedArray()
        mergeOccurred = false

        when (direction) {
            "left" -> moveLeft()
            "right" -> moveRight()
            "up" -> moveUp()
            "down" -> moveDown()
        }

        val hasChanged = oldBoard.indices.any { i -> oldBoard[i].contentEquals(board[i]) == false }

        if (hasChanged) {
            addRandomTile()
            if (score > bestScore) bestScore = score
        }

        return hasChanged
    }

    // 儲存當前遊戲狀態
    private fun saveState() {
        previousStates.add(GameState(board.map { it.clone() }.toTypedArray(), score, mergeOccurred))
        if (previousStates.size > 10) {
            previousStates.removeAt(0) // 保持最多10個狀態記錄
        }
    }

    // 還原至上一步的遊戲狀態
    fun undo(): Boolean {
        if (previousStates.isNotEmpty()) {
            val previousState = previousStates.removeAt(previousStates.size - 1)
            board = previousState.board
            score = previousState.score
            mergeOccurred = previousState.mergeOccurred
            return true
        }
        return false
    }

    // 在 Game2048 類別內部添加這個方法
    fun copy(): Game2048 {
        val newGame = Game2048()
        newGame.board = this.board.map { it.clone() }.toTypedArray()  // 複製棋盤
        newGame.score = this.score
        newGame.bestScore = this.bestScore
        return newGame
    }

    fun isGameOver(): Boolean {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == 0) return false
            }
        }

        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (i < 3 && board[i][j] == board[i + 1][j]) return false
                if (j < 3 && board[i][j] == board[i][j + 1]) return false
            }
        }

        return true
    }

    private fun moveLeft() {
        for (row in board) {
            val filtered = row.filter { it != 0 }.toIntArray()
            val newRow = mergeLeft(filtered)
            for (j in 0 until 4) row[j] = if (j < newRow.size) newRow[j] else 0
        }
    }

    private fun moveRight() {
        for (row in board) {
            val filtered = row.filter { it != 0 }.toIntArray()
            val newRow = mergeRight(filtered)
            for (j in 0 until 4) row[j] = if (j >= 4 - newRow.size) newRow[j - (4 - newRow.size)] else 0
        }
    }

    private fun moveUp() {
        for (j in 0 until 4) {
            val col = IntArray(4) { board[it][j] }.filter { it != 0 }.toIntArray()
            val newCol = mergeLeft(col)
            for (i in 0 until 4) board[i][j] = if (i < newCol.size) newCol[i] else 0
        }
    }

    private fun moveDown() {
        for (j in 0 until 4) {
            val col = IntArray(4) { board[it][j] }.filter { it != 0 }.toIntArray()
            val newCol = mergeRight(col)
            for (i in 0 until 4) board[i][j] = if (i >= 4 - newCol.size) newCol[i - (4 - newCol.size)] else 0
        }
    }

    private fun mergeLeft(row: IntArray): IntArray {
        val newList = mutableListOf<Int>()
        var skip = false
        for (i in row.indices) {
            if (skip) {
                skip = false
                continue
            }
            if (i < row.size - 1 && row[i] == row[i + 1]) {
                newList.add(row[i] * 2)
                score += row[i] * 2
                skip = true
                mergeOccurred = true
            } else {
                newList.add(row[i])
            }
        }
        return newList.toIntArray()
    }

    private fun mergeRight(row: IntArray): IntArray {
        val newList = mutableListOf<Int>()
        var skip = false
        for (i in row.size - 1 downTo 0) {
            if (skip) {
                skip = false
                continue
            }
            if (i > 0 && row[i] == row[i - 1]) {
                newList.add(0, row[i] * 2)
                score += row[i] * 2
                skip = true
                mergeOccurred = true
            } else {
                newList.add(0, row[i])
            }
        }
        return newList.toIntArray()
    }

    private fun addRandomTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == 0) emptyCells.add(Pair(i, j))
            }
        }
        if (emptyCells.isNotEmpty()) {
            val (x, y) = emptyCells.random()
            board[x][y] = if ((0..9).random() < 9) 2 else 4
        }
    }

    // 用來儲存遊戲狀態的資料類別
    data class GameState(val board: Array<IntArray>, val score: Int, val mergeOccurred: Boolean)
}
