package com.xyluodys.xgams.Gomoku.AI

/**
 * VCT / VCF 求解器（OR-AND 树）
 *
 * 威胁生成：只生成能形成活三 / 冲四 / 活四的点（分支因子从 ~30 压到 3-8）
 *
 * 与主搜索交互：作为前置过滤器（论文第五章），发现必胜直接返回，
 * 避免把算力浪费在搜索上
 */
object VCTSolver {

    private const val SIZE = 15
    private const val MAX_NODES = 30_000   // 节点预算，超限保守返回 null

    private var nodes = 0

    /**
     * AI 尝试在 maxDepth 个攻击步内必胜
     */
    fun attack(board: Array<IntArray>, ai: Int, human: Int, maxDepth: Int): Pair<Int, Int>? {
        nodes = 0
        return attackRec(board, ai, human, maxDepth)
    }

    // OR 节点
    private fun attackRec(board: Array<IntArray>, ai: Int, human: Int, maxDepth: Int): Pair<Int, Int>? {
        val moves = threatMoves(board, ai)
        if (moves.isEmpty()) return null

        // 威胁排序：活四 > 冲四 > 活三
        val sorted = moves.sortedByDescending { it.level }
        for ((r, c, _) in sorted) {
            if (nodes++ > MAX_NODES) return null
            board[r][c] = ai
            if (hasFive(board, r, c, ai)) {
                board[r][c] = 0
                return r to c
            }
            if (defendRec(board, ai, human, maxDepth - 1)) {
                board[r][c] = 0
                return r to c
            }
            board[r][c] = 0
        }
        return null
    }

    // AND 节点：返回 true 表示攻击方在所有防守下都能赢
    private fun defendRec(board: Array<IntArray>, ai: Int, human: Int, maxDepth: Int): Boolean {
        if (maxDepth <= 0) return false

        val defenses = threatMoves(board, human)
        if (defenses.isEmpty()) {
            // 人类没有直接威胁走法不代表攻击方必胜，威胁链可能已断
            // 继续递归验证攻击方后续是否仍能走出必胜序列
            return attackRec(board, ai, human, maxDepth - 1) != null
        }

        val sorted = defenses.sortedByDescending { it.level }
        for ((r, c, _) in sorted) {
            if (nodes++ > MAX_NODES) return false
            board[r][c] = human
            // 反杀检测：人类落子后若形成活四或五连，攻击方失败
            // （必须同时检测五连——否则人类一手成五时，VCT 仍会误以为攻击方自己连五而判定必胜）
            if (createsOpenFour(board, r, c, human) || hasFive(board, r, c, human)) {
                board[r][c] = 0
                return false
            }
            val win = attackRec(board, ai, human, maxDepth - 1)
            board[r][c] = 0
            if (win == null) return false  // 该防守成功，攻击方无法必胜
        }
        return true
    }

    /**
     * 生成威胁走法：空点中，落子后能形成 活三/冲四/活四/五连 的点
     */
    private fun threatMoves(board: Array<IntArray>, player: Int): List<ThreatMove> {
        val result = mutableListOf<ThreatMove>()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != 0) continue
                if (!isNearStones(board, r, c)) continue
                board[r][c] = player
                val level = maxDirectionShape(board, r, c, player)
                board[r][c] = 0
                if (level >= AIScoring.OPEN_THREE) {
                    result.add(ThreatMove(r, c, level))
                }
            }
        }
        return result
    }

    private fun maxDirectionShape(board: Array<IntArray>, r: Int, c: Int, player: Int): Int {
        var max = AIScoring.NONE
        for (dir in AIScoring.DIRECTIONS) {
            val s = BoardEvaluator.directionShape(board, r, c, dir.first, dir.second, player)
            if (s > max) max = s
        }
        return max
    }

    /**
     * 检测落子后是否形成活四
     */
    private fun createsOpenFour(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in AIScoring.DIRECTIONS) {
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

    /**
     * 快速检查某个位置是否形成五连
     */
    private fun hasFive(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in AIScoring.DIRECTIONS) {
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

    private fun isNearStones(board: Array<IntArray>, r: Int, c: Int): Boolean {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val rr = r + dr; val cc = c + dc
                if (rr in 0 until SIZE && cc in 0 until SIZE && board[rr][cc] != 0) return true
            }
        }
        return false
    }

    // 威胁走法
    private data class ThreatMove(val r: Int, val c: Int, val level: Int)
}
