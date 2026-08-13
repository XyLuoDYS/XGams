package com.xyluodys.xgams.Gomoku.AI

/**
 * 连贯进攻工具
 *
 * 解决 AI「自己有活二，却不去活三、反而跑去开新活二」的进攻中断问题：
 * 当 AI 已有活二 / 跳活二时，优先寻找能把它「延伸成活三 / 跳活三」的点，
 * 沿同一条进攻线继续推进，而不是另起炉灶。
 *
 * 调用约定（见 [GomokuMinimax.findBestMove]）：
 *   即胜/必挡 > AI 活四 > 堵对方活三 > 开局库 > VCT 必胜 > **连贯进攻** > 搜索兜底
 * 即：双方无立即成五 / 活四威胁、且无强制必胜路线时，优先做连贯延伸。
 */
object GomokuAttack {

    private const val SIZE = 15
    private val DIRECTIONS = AIScoring.DIRECTIONS

    /**
     * 寻找「延伸己方活二 / 跳活二 → 活三 / 跳活三」的连贯进攻点。
     * 优先返回更强的走法（双活三及以上组合威胁最先），其次单活三（连贯延伸），
     * 且只返回安全的点（不会一手送给对方成五或活四）。
     * 没有任何可延伸点则返回 null。
     */
    fun findExtendThree(board: Array<IntArray>, player: Int): Pair<Int, Int>? {
        val candidates = candidatesNear(board)

        // high：落子后形成双活三 / 四三 / 双四等组合杀（比单活三更优）
        val high = mutableListOf<Pair<Int, Int>>()
        // mid：落子后形成单活三 / 跳活三（即把活二延伸成活三，连贯进攻）
        val mid = mutableListOf<Pair<Int, Int>>()

        for ((r, c) in candidates) {
            board[r][c] = player
            val level = BoardEvaluator.threatLevel(board, r, c, player)
            board[r][c] = 0

            when {
                level >= AIScoring.DOUBLE_THREE -> high.add(r to c)
                level >= AIScoring.OPEN_THREE -> mid.add(r to c)
            }
        }

        // 安全优先：先返回高威胁组里安全的点，再返回单活三组里安全的点
        for (group in listOf(high, mid)) {
            for ((r, c) in group) {
                if (isSafe(board, r, c, player)) return r to c
            }
        }
        return null
    }

    /**
     * 安全校验：在 (r,c) 落子 player 是否可接受。
     * 排除两种明显送杀的情形：
     *   1. 该点本身就是对方一手成五的点（说明漏挡了对方的冲四，正常情况下前面已处理）
     *   2. 该点本身就是对方一手形成活四的点（对方下一步必胜）
     */
    private fun isSafe(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        val opponent = if (player == 1) 2 else 1
        // 模拟对方在此落子能否成五
        board[r][c] = opponent
        val oppFive = hasFive(board, r, c, opponent)
        board[r][c] = player
        if (oppFive) return false
        // 模拟对方在此落子能否形成活四
        val oppOpenFour = createsOpenFour(board, r, c, opponent)
        board[r][c] = 0
        return !oppOpenFour
    }

    /** 收集已有棋子周围 2 格内的空位作为候选 */
    private fun candidatesNear(board: Array<IntArray>): List<Pair<Int, Int>> {
        val set = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != 0) {
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val rr = r + dr
                            val cc = c + dc
                            if (rr in 0 until SIZE && cc in 0 until SIZE && board[rr][cc] == 0) {
                                set.add(rr to cc)
                            }
                        }
                    }
                }
            }
        }
        return set.toList()
    }

    /** 检测 (r,c) 落子 player 后是否形成五连 */
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

    /** 检测 (r,c) 落子 player 后，4 方向是否出现活四（_XXXX_） */
    private fun createsOpenFour(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in DIRECTIONS) {
            val chars = CharArray(11)
            chars[0] = '2'
            chars[10] = '2'
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
}
