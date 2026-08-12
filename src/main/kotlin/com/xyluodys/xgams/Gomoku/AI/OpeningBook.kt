package com.xyluodys.xgams.Gomoku.AI

/**
 * 开局库（简化版）
 *
 * 仅在"轮到白方落子"（白子数 = 黑子数 - 1）且处于开局阶段时返回走法
 */
object OpeningBook {

    private const val SIZE = 15

    /**
     * 返回 AI(白方) 的开局应对走法；不在开局范围返回 null。
     */
    fun getMove(board: Array<IntArray>, ai: Int): Pair<Int, Int>? {
        val stones = mutableListOf<Triple<Int, Int, Int>>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val v = board[r][c]
                if (v != 0) stones.add(Triple(r, c, v))
            }
        }

        val blackCount = stones.count { it.third == 1 }
        val whiteCount = stones.count { it.third == 2 }
        // 只在轮到白方落子时（白 = 黑 - 1）
        if (whiteCount != blackCount - 1) return null

        // 白2：棋盘只有黑1
        if (stones.size == 1) {
            val (r, c, _) = stones[0]
            // 候选顺序：斜指紧邻优先，其次直指，再向外扩一格，避免贴边
            val candidates = listOf(
                r + 1 to c + 1, r + 1 to c, r to c + 1,
                r - 1 to c + 1, r + 1 to c - 1,
                r + 2 to c + 1, r + 1 to c + 2, r + 2 to c - 1, r - 1 to c + 2
            )
            for ((rr, cc) in candidates) {
                if (validEmpty(board, rr, cc)) return rr to cc
            }
            return null
        }

        // 白4：黑1、白2、黑3 —— 在黑3 周围 2 格内选最强白点
        if (stones.size == 3) {
            val black3 = stones.filter { it.third == 1 }
                .maxByOrNull { it.first * SIZE + it.second } ?: return null
            var best: Pair<Int, Int>? = null
            var bestScore = Int.MIN_VALUE
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val rr = black3.first + dr
                    val cc = black3.second + dc
                    if (!validEmpty(board, rr, cc)) continue
                    val s = BoardEvaluator.quickEvaluate(board, rr, cc, ai)
                    if (s > bestScore) {
                        bestScore = s
                        best = rr to cc
                    }
                }
            }
            return best
        }

        return null
    }

    private fun validEmpty(board: Array<IntArray>, r: Int, c: Int): Boolean {
        return r in 0 until SIZE && c in 0 until SIZE && board[r][c] == 0
    }
}
