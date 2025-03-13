class Game2048(var size: Int = 4) {
    var board: Array<IntArray> = Array(size) { IntArray(size) }
    var score = 0
    var bestScore = 0
    var mergeOccurred = false
    private var previousStates = mutableListOf<GameState>()

    fun checkFor2048(): Boolean {
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j] == 2048) {
                    return true
                }
            }
        }
        return false
    }

    init {
        resetGame()
    }
    fun addNewTile() {
        val emptyTiles = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j] == 0) emptyTiles.add(Pair(i, j))
            }
        }
        if (emptyTiles.isNotEmpty()) {
            val (r, c) = emptyTiles.random()
            board[r][c] = if (Math.random() < 0.9) 2 else 4
        }
    }
    fun resetGame() {
        board = Array(size) { IntArray(size) }
        score = 0
        addRandomTile()
        addRandomTile()
        mergeOccurred = false
        previousStates.clear()
    }

    fun move(direction: String): Boolean {
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
        previousStates.add(GameState(size, board.map { it.clone() }.toTypedArray(), score, mergeOccurred))
        if (previousStates.size > 10) {
            previousStates.removeAt(0)
        }
    }

    fun undo(): Boolean {
        if (previousStates.isNotEmpty()) {
            val previousState = previousStates.removeAt(previousStates.size - 1)
            size = previousState.size
            board = previousState.board
            score = previousState.score
            mergeOccurred = previousState.mergeOccurred
            return true
        }
        return false
    }

    fun copy(): Game2048 {
        val newGame = Game2048(size)
        newGame.board = this.board.map { it.clone() }.toTypedArray()
        newGame.score = this.score
        newGame.bestScore = this.bestScore
        return newGame
    }

    fun isGameOver(): Boolean {
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j] == 0) return false
            }
        }

        for (i in 0 until size) {
            for (j in 0 until size) {
                if (i < size - 1 && board[i][j] == board[i + 1][j]) return false
                if (j < size - 1 && board[i][j] == board[i][j + 1]) return false
            }
        }

        return true
    }

    private fun moveLeft() {
        for (row in board) {
            val filtered = row.filter { it != 0 }.toIntArray()
            val newRow = mergeLeft(filtered)
            for (j in 0 until size) {
                row[j] = if (j < newRow.size) newRow[j] else 0
            }
        }
    }

    private fun moveRight() {
        for (row in board) {
            val filtered = row.filter { it != 0 }.toIntArray()
            val newRow = mergeRight(filtered)
            for (j in 0 until size) {
                row[j] = if (j >= size - newRow.size) newRow[j - (size - newRow.size)] else 0
            }
        }
    }

    private fun moveUp() {
        for (j in 0 until size) {
            val col = IntArray(size) { board[it][j] }.filter { it != 0 }.toIntArray()
            val newCol = mergeLeft(col)
            for (i in 0 until size) {
                board[i][j] = if (i < newCol.size) newCol[i] else 0
            }
        }
    }

    private fun moveDown() {
        for (j in 0 until size) {
            val col = IntArray(size) { board[it][j] }.filter { it != 0 }.toIntArray()
            val newCol = mergeRight(col)
            for (i in 0 until size) {
                board[i][j] = if (i >= size - newCol.size) newCol[i - (size - newCol.size)] else 0
            }
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
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j] == 0) emptyCells.add(Pair(i, j))
            }
        }
        if (emptyCells.isNotEmpty()) {
            val (x, y) = emptyCells.random()
            board[x][y] = if ((0..9).random() < 9) 2 else 4
        }
    }
    data class GameState(val size: Int, val board: Array<IntArray>, val score: Int, val mergeOccurred: Boolean)
}
