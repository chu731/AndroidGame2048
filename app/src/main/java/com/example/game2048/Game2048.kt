class Game2048 {
    var board = Array(4) { IntArray(4) }
    var score = 0
    var bestScore = 0
    var mergeOccurred = false

    init {
        resetGame()
    }

    fun resetGame() {
        board = Array(4) { IntArray(4) }
        score = 0
        addRandomTile()
        addRandomTile()
        mergeOccurred = false
    }

    fun move(direction: String): Boolean {
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
            val filtered = row.filter { it  != 0 }.toIntArray()
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
}
