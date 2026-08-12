package com.xyluodys.xgams.Gomoku

class GomokuGame(var difficulty: String = "medium") {
    val board = Array(15) { IntArray(15) { 0 } }
    var currentPlayer = 1  // 1=黑方(玩家)  2=白方(AI)
    var gameOver = false
    var winner = 0  // 0=无(平局或未结束)  1=黑方  2=白方

    // 最后落子位置（-1 表示尚无落子）
    var lastMoveRow = -1
    var lastMoveCol = -1

    // AI 是否正在思考（防止卡bug）
    var aiThinking = false

    // 五子连珠的获胜连线坐标
    var winningLine: List<Pair<Int, Int>> = emptyList()

    // 连线标记动画进度
    var winMarkProgress = 0

    // 是否在 Dialog 第七行显示游戏结束消息
    var showGameOverMessage = false

    fun place(row: Int, col: Int): Boolean {
        if (gameOver) return false
        if (row !in 0..14 || col !in 0..14) return false
        if (board[row][col] != 0) return false
        board[row][col] = currentPlayer
        lastMoveRow = row
        lastMoveCol = col

        // 检查五子连珠
        val line = findWinningLine(row, col)
        if (line != null) {
            gameOver = true
            winner = currentPlayer
            winningLine = line
            return true
        }

        // 检查平局
        if (isBoardFull()) {
            gameOver = true
            winner = 0
            showGameOverMessage = true  // 平局直接显示消息，无需动画
            return true
        }

        currentPlayer = if (currentPlayer == 1) 2 else 1
        return true
    }

    /**
     * 从 (row, col) 出发，检查四个方向是否有五子连珠
     * 方向：横向、纵向、斜线↘、斜线↙
     * 返回连线中所有棋子的坐标（按从一端到另一端的顺序），无连线则返回 null
     */
    private fun findWinningLine(row: Int, col: Int): List<Pair<Int, Int>>? {
        val stone = board[row][col]
        if (stone == 0) return null

        val directions = listOf(
            0 to 1,   // 横向 →
            1 to 0,   // 纵向 ↓
            1 to 1,   // 斜线 ↘
            1 to -1   // 斜线 ↙
        )

        for ((dr, dc) in directions) {
            // 收集该方向上所有连续同色棋子（包含落子点）
            val stones = mutableListOf(row to col)

            // 正方向计数
            var r = row + dr
            var c = col + dc
            while (r in 0..14 && c in 0..14 && board[r][c] == stone) {
                stones.add(r to c)
                r += dr
                c += dc
            }

            // 反方向计数（插入到列表头部，保持从一端到另一端的顺序）
            r = row - dr
            c = col - dc
            while (r in 0..14 && c in 0..14 && board[r][c] == stone) {
                stones.add(0, r to c)
                r -= dr
                c -= dc
            }

            if (stones.size >= 5) return stones
        }

        return null
    }

    /**
     * 检查棋盘是否已满（平局）
     */
    private fun isBoardFull(): Boolean {
        return board.all { row -> row.all { it != 0 } }
    }
}
