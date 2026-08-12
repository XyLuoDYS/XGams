package com.xyluodys.xgams.Gomoku.AI

/**
 * 通用防守工具：所有难度的「底线必堵 / 必胜」检查都走这里，保证逻辑单一来源。
 *
 * 关键防守语义：
 *  - 活四(_XXXX_)是必胜形，两端都能成五，堵一端没用，因此必须在对方「活三/跳三」阶段
 *    就封堵其发展点（即对方落子能形成活四的那个空位）。
 *  - 冲四(XXXX_/_XXXX)的成五点由 findImmediateWin 兜底，本类不重复处理。
 */
object GomokuDefense {

    private const val SIZE = 15
    private val DIRECTIONS = AIScoring.DIRECTIONS

    /** player 一手连五的获胜点（自己赢 / 对方一手成五必堵） */
    fun findImmediateWin(board: Array<IntArray>, player: Int): Pair<Int, Int>? {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != 0) continue
                if (!near(board, r, c)) continue
                board[r][c] = player
                val win = hasFive(board, r, c, player)
                board[r][c] = 0
                if (win) return r to c
            }
        }
        return null
    }

    /** player 落子后能形成活四的点（一手造活四，必胜） */
    fun findOpenFourMove(board: Array<IntArray>, player: Int): Pair<Int, Int>? {
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != 0) continue
                if (!near(board, r, c)) continue
                board[r][c] = player
                val ok = createsOpenFour(board, r, c, player)
                board[r][c] = 0
                if (ok) return r to c
            }
        }
        return null
    }

    /** 封堵：对方落子能形成活四的点（即对方活三/跳三的发展点） */
    fun findBlockOpenFour(board: Array<IntArray>, player: Int): Pair<Int, Int>? {
        return findOpenFourMove(board, player)
    }

    /** 检测 (r,c) 落子 player 后，4 方向是否出现活四（_XXXX_） */
    private fun createsOpenFour(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in DIRECTIONS) {
            val chars = CharArray(11) { '2' }
            for (i in -5..5) {
                val rr = r + i * dir.first
                val cc = c + i * dir.second
                chars[i + 5] = when {
                    rr !in 0 until SIZE || cc !in 0 until SIZE -> '2'
                    board[rr][cc] == 0 -> '0'
                    board[rr][cc] == player -> '1'
                    else -> '2'
                }
            }
            if ("011110" in chars.concatToString()) return true
        }
        return false
    }

    private fun hasFive(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in DIRECTIONS) {
            var count = 1
            var rr = r + dir.first; var cc = c + dir.second
            while (rr in 0 until SIZE && cc in 0 until SIZE && board[rr][cc] == player) {
                count++; rr += dir.first; cc += dir.second
            }
            rr = r - dir.first; cc = c - dir.second
            while (rr in 0 until SIZE && cc in 0 until SIZE && board[rr][cc] == player) {
                count++; rr -= dir.first; cc -= dir.second
            }
            if (count >= 5) return true
        }
        return false
    }

    private fun near(board: Array<IntArray>, r: Int, c: Int): Boolean {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val rr = r + dr; val cc = c + dc
                if (rr in 0 until SIZE && cc in 0 until SIZE && board[rr][cc] != 0) return true
            }
        }
        return false
    }
}
