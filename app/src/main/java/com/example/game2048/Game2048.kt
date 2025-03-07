class Game2048 {
    var board = Array(4) { IntArray(4) }
    var score = 0
    var bestScore = 0
    var mergeOccurred = false  // 記錄是否有合併

    init {
        resetGame()
    }

    fun resetGame() {
        board = Array(4) { IntArray(4) }
        score = 0
        addRandomTile()
        addRandomTile()
        mergeOccurred = false  // 重置合併狀態
    }

    fun move(direction: String): Boolean {
        val oldBoard = board.map { it.clone() }.toTypedArray()
        mergeOccurred = false  // 每次移動前都重置 mergeOccurred

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

        // 檢查遊戲是否結束
        return hasChanged
    }

    // 檢查遊戲是否結束
    fun isGameOver(): Boolean {
        // 檢查是否有空位
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (board[i][j] == 0) return false  // 有空位，遊戲未結束
            }
        }

        // 檢查是否有相鄰的數字相同
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                if (i < 3 && board[i][j] == board[i + 1][j]) return false  // 橫向合併
                if (j < 3 && board[i][j] == board[i][j + 1]) return false  // 縱向合併
            }
        }

        return true  // 沒有空位，也沒有相鄰的相同數字，遊戲結束
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
                mergeOccurred = true  // 發生合併
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
                mergeOccurred = true  // 發生合併
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
