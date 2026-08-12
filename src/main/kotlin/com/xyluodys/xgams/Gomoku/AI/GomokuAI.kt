package com.xyluodys.xgams.Gomoku.AI

/**
 * 五子棋 AI 调度器
 * 其实就是个难度选择器，调用不同的 AI 算法 §(*￣▽￣*)§
 *
 * 困难（hard）—— 完整形态：
 *   MTD(f) 深度 8 + 威胁扩展 + VCT 前置 + 开局库 + 置换表
 *   时间硬截断 600ms，棋力：业余顶尖 ~ 弱职业，足以让绝大多数玩家绝望
 *
 * 中等（medium）—— 在困难基础上削弱：
 *   MTD(f) 深度 4 + 威胁扩展 + 置换表
 *   关闭 VCT 求解器、关闭开局库，时间 200ms
 *   棋力：中等水平，能识别基本攻防但算不清深层杀法
 *
 * 简单（easy）—— 再削弱：
 *   贪心评估（只看当前一步的攻防价值，不做搜索树）
 *   仅做即胜/必挡的底线防守，棋力：入门水平，适合新手
 */
class GomokuAI(private val difficulty: String = "medium") {

    private val size = 15

    /**
     * 返回 AI 的最佳落子位置 (row, col)，没有可下位置时返回 null
     */
    fun findBestMove(board: Array<IntArray>): Pair<Int, Int>? {
        return when (difficulty) {
            "hard" -> GomokuMinimax(
                maxDepth = 8,
                timeLimitMs = 600,
                maxCandidates = 12,
                useThreatExtension = true,
                useVCT = true,
                useOpeningBook = true
            ).findBestMove(board)

            "medium" -> GomokuMinimax(
                maxDepth = 4,
                timeLimitMs = 200,
                maxCandidates = 10,
                useThreatExtension = true,
                useVCT = false,
                useOpeningBook = false
            ).findBestMove(board)

            else -> findBestMoveGreedy(board)
        }
    }

    /**
     * 贪心评估（简单模式）
     *
     * 只评估每个候选位置的单步攻防价值，不做搜索树
     * 包含即胜/必挡的底线防守，避免太离谱的送吃
     */
    private fun findBestMoveGreedy(board: Array<IntArray>): Pair<Int, Int>? {
        // 收集候选位置：已有棋子周围 2 格内的空位
        val candidates = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] != 0) {
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val rr = r + dr
                            val cc = c + dc
                            if (rr in 0 until size && cc in 0 until size && board[rr][cc] == 0) {
                                candidates.add(rr to cc)
                            }
                        }
                    }
                }
            }
        }

        // 棋盘空时下中心
        if (candidates.isEmpty()) return size / 2 to size / 2

        // 即胜检测：自己能连五直接走
        for ((r, c) in candidates) {
            board[r][c] = 2
            if (hasFive(board, r, c, 2)) {
                board[r][c] = 0
                return r to c
            }
            board[r][c] = 0
        }
        // 必挡检测：对方能连五必须挡
        for ((r, c) in candidates) {
            board[r][c] = 1
            if (hasFive(board, r, c, 1)) {
                board[r][c] = 0
                return r to c
            }
            board[r][c] = 0
        }

        // 自己一手造活四（必胜）
        GomokuDefense.findOpenFourMove(board, 2)?.let { return it }
        // 对方活三必堵（活四发展点）
        GomokuDefense.findBlockOpenFour(board, 1)?.let { return it }

        // 贪心评估：取攻防综合分最高的走法（并列随机）
        var bestScore = Int.MIN_VALUE
        var bestMoves = mutableListOf<Pair<Int, Int>>()
        for ((r, c) in candidates) {
            val score = BoardEvaluator.quickEvaluate(board, r, c, 2)
            if (score > bestScore) {
                bestScore = score
                bestMoves = mutableListOf(r to c)
            } else if (score == bestScore) {
                bestMoves.add(r to c)
            }
        }

        return bestMoves.randomOrNull()
    }

    /** 快速检查某个位置是否形成五连 */
    private fun hasFive(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in AIScoring.DIRECTIONS) {
            var count = 1
            var rr = r + dir.first; var cc = c + dir.second
            while (rr in 0 until size && cc in 0 until size && board[rr][cc] == player) {
                count++; rr += dir.first; cc += dir.second
            }
            rr = r - dir.first; cc = c - dir.second
            while (rr in 0 until size && cc in 0 until size && board[rr][cc] == player) {
                count++; rr -= dir.first; cc -= dir.second
            }
            if (count >= 5) return true
        }
        return false
    }
}
