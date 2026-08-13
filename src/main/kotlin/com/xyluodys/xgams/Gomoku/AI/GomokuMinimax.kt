package com.xyluodys.xgams.Gomoku.AI

import kotlin.math.abs

/**
 * 搜索引擎
 *
 * 开局库(查表) → VCT前置检测 → MTD(f)搜索 → 精确模式状态机评估
 *                ↓
 *          置换表(开放寻址) + 威胁扩展 + 候选优化
 */
class GomokuMinimax(
    private val aiPlayer: Int = 2,                        // AI（白方）
    private val humanPlayer: Int = 1,                    // 人类（黑方）
    private val maxDepth: Int = 8,                      // 最大搜索深度
    private val timeLimitMs: Long = 600,               // 时间硬截断（毫秒）
    private val maxCandidates: Int = 12,              // 每层最多展开的候选数
    private val useThreatExtension: Boolean = true,  // 威胁扩展
    private val useVCT: Boolean = true,             // VCT 前置检测
    private val useOpeningBook: Boolean = true     // 开局库
) {

    private val size = 15
    private val vctDepth = 6            // VCT 攻击步数上限
    private var startTime = 0L
    private val tt = TranspositionTable()

    private companion object {
        const val INF = Int.MAX_VALUE / 2
        const val MAX_EXTENSION_BUDGET = 12   // 单分支累计威胁扩展深度上限
    }

    /**
     * 寻找最佳落子位置
     */
    fun findBestMove(board: Array<IntArray>): Pair<Int, Int>? {
        startTime = System.currentTimeMillis()

        // 棋盘空时下中心
        if (isEmpty(board)) return size / 2 to size / 2

        // === 即胜 / 必挡（最高优先级） ===
        // 顺序：AI成五 > 对方成五 > AI活四(必胜) > 对方活三(活四发展点)
        // 必须先于开局库/VCT/搜索，否则进攻或搜索会压过防守而漏堵
        findImmediateWin(board, aiPlayer)?.let { return it }
        findImmediateWin(board, humanPlayer)?.let { return it }
        // AI 一手造活四（下一步必胜），优先于堵对方活三
        findOpenFourMove(board, aiPlayer)?.let { return it }
        // 对方已有活三/跳三：其发展点落子即成活四(必胜)，必须封堵
        GomokuDefense.findBlockOpenFour(board, humanPlayer)?.let { return it }

        // === 开局库（仅开局前几手，此时不存在一手成五/活四威胁） ===
        if (useOpeningBook) {
            OpeningBook.getMove(board, aiPlayer)?.let { return it }
        }

        // === VCT 前置检测（仅在没有被对方一手成五/活四威胁时才有意义） ===
        if (useVCT) {
            VCTSolver.attack(board, aiPlayer, humanPlayer, vctDepth)?.let { return it }
        }

        // === 连贯进攻：无必胜路线时，优先延伸己方活二/活三，保持进攻连续性 ===
        // 前面已处理双方成五/活四威胁与开局库，此处安全；
        // 避免出现「自己有活二却不去活三、反而跑去开新活二」的进攻中断
        GomokuAttack.findExtendThree(board, aiPlayer)?.let { return it }

        // === 迭代加深 + MTD(f) ===
        val rootMoves = getOrderedCandidates(board, aiPlayer, null)
        if (rootMoves.isEmpty()) return size / 2 to size / 2

        var best: Pair<Int, Int> = rootMoves[0]
        var prevBest: Pair<Int, Int> = best
        var guess = 0

        for (depth in 2..maxDepth) {
            if (timeUp()) break
            val (score, move) = mtd_f(board, guess, depth, prevBest)
            if (move != null) {
                best = move
                prevBest = move
            }
            guess = score
            // 找到必胜（或必败）则停止迭代
            if (abs(score) >= AIScoring.WIN - 1000) break
            if (timeUp()) break
        }

        tt.clear()
        return best
    }

    private fun mtd_f(
        board: Array<IntArray>,
        firstGuess: Int,
        depth: Int,
        prevBest: Pair<Int, Int>
    ): Pair<Int, Pair<Int, Int>?> {
        var guess = firstGuess
        var lower = -INF
        var upper = INF
        var bestMove: Pair<Int, Int>? = prevBest
        var iteration = 0

        while (lower < upper) {
            if (timeUp() || iteration++ > 50) break
            val beta = if (guess == lower) guess + 1 else guess
            val (score, move) = pvsRoot(board, depth, beta - 1, beta, bestMove, MAX_EXTENSION_BUDGET)
            if (move != null) bestMove = move
            if (score < beta) upper = score else { lower = score; guess = score }
        }

        return guess to bestMove
    }

    /**
     * PVS 根节点：返回 (评分, 最佳走法)
     */
    private fun pvsRoot(
        board: Array<IntArray>,
        depth: Int,
        alpha: Int,
        beta: Int,
        prevBest: Pair<Int, Int>?,
        extensionBudget: Int
    ): Pair<Int, Pair<Int, Int>?> {
        val candidates = getOrderedCandidates(board, aiPlayer, prevBest)
        if (candidates.isEmpty()) return 0 to null

        val rootHash = tt.computeHash(board)
        var a = alpha
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null

        for ((i, move) in candidates.withIndex()) {
            val (r, c) = move
            board[r][c] = aiPlayer
            val newHash = tt.toggleStone(rootHash, r, c, aiPlayer)

            // 快速检测胜利
            if (hasFive(board, r, c, aiPlayer)) {
                board[r][c] = 0
                return AIScoring.WIN to move
            }

            // 威胁扩展（受单分支累计预算限制）
            val ext = if (useThreatExtension) {
                getThreatExtension(board, r, c, aiPlayer, extensionBudget)
            } else 0
            val searchDepth = depth - 1 + ext
            val newBudget = extensionBudget - ext

            // PVS：第一个走法全窗口，其余零窗口
            val score = if (i == 0) {
                -pvs(board, searchDepth, -beta, -a, false, newHash, newBudget)
            } else {
                val nullScore = -pvs(board, searchDepth, -a - 1, -a, false, newHash, newBudget)
                if (nullScore > a && nullScore < beta) {
                    -pvs(board, searchDepth, -beta, -nullScore, false, newHash, newBudget)
                } else {
                    nullScore
                }
            }

            board[r][c] = 0

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > a) a = score
            if (a >= beta) break
            if (timeUp()) break
        }

        return bestScore to bestMove
    }

    /**
     * PVS 递归搜索
     * @param isMaximizing true=AI回合(最大化), false=人类回合(最小化)
     * @param hash 当前棋盘状态的增量 Zobrist 哈希
     * @param extensionBudget 当前分支剩余威胁扩展预算
     */
    private fun pvs(
        board: Array<IntArray>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        hash: Long,
        extensionBudget: Int
    ): Int {
        val alphaOrig = alpha

        val player = if (isMaximizing) aiPlayer else humanPlayer

        val entry = tt.get(hash, depth)
        if (entry != null) {
            val (score, _, flag) = entry
            when (flag) {
                TranspositionTable.FLAG_EXACT -> return score
                TranspositionTable.FLAG_LOWER_BOUND -> if (score >= beta) return score
                TranspositionTable.FLAG_UPPER_BOUND -> if (score <= alpha) return score
            }
        }

        if (depth <= 0 || timeUp()) {
            // negamax 约定：评估必须从「轮到谁走」的视角返回，
            // 否则攻防符号会错位（表现为只进攻不防守）
            val e = BoardEvaluator.evaluate(board)
            return if (player == aiPlayer) e else -e
        }

        val candidates = getOrderedCandidates(board, player, null)
        if (candidates.isEmpty()) {
            val e = BoardEvaluator.evaluate(board)
            return if (player == aiPlayer) e else -e
        }

        var a = alpha
        var bestScore = Int.MIN_VALUE

        for ((i, move) in candidates.withIndex()) {
            val (r, c) = move
            board[r][c] = player
            val newHash = tt.toggleStone(hash, r, c, player)

            // 快速检测胜利
            if (hasFive(board, r, c, player)) {
                // negamax 约定：从落子方视角，自己连成即获胜（+WIN）
                // 父节点会用负号翻转成对手视角，从而正确区分攻防
                val winScore = AIScoring.WIN - (maxDepth - depth)
                tt.put(newHash, winScore, depth, TranspositionTable.FLAG_EXACT)
                board[r][c] = 0
                return winScore
            }

            // 威胁扩展（受单分支累计预算限制）
            val ext = if (useThreatExtension) {
                getThreatExtension(board, r, c, player, extensionBudget)
            } else 0
            val searchDepth = depth - 1 + ext
            val newBudget = extensionBudget - ext

            // PVS：第一个走法全窗口，其余零窗口
            val score = if (i == 0) {
                -pvs(board, searchDepth, -beta, -a, !isMaximizing, newHash, newBudget)
            } else {
                val nullScore = -pvs(board, searchDepth, -a - 1, -a, !isMaximizing, newHash, newBudget)
                if (nullScore > a && nullScore < beta) {
                    -pvs(board, searchDepth, -beta, -nullScore, !isMaximizing, newHash, newBudget)
                } else {
                    nullScore
                }
            }

            board[r][c] = 0

            if (score > bestScore) bestScore = score
            if (score > a) a = score
            if (a >= beta) break   // Beta 剪枝
            if (timeUp()) break
        }

        // === 存入置换表 ===
        val flag = when {
            bestScore <= alphaOrig -> TranspositionTable.FLAG_UPPER_BOUND
            bestScore >= beta -> TranspositionTable.FLAG_LOWER_BOUND
            else -> TranspositionTable.FLAG_EXACT
        }
        tt.put(hash, bestScore, depth, flag)

        return bestScore
    }

    /** 检测指定玩家是否有一步获胜的走法 */
    private fun findImmediateWin(board: Array<IntArray>, player: Int): Pair<Int, Int>? {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] != 0) continue
                if (!isNearStones(board, r, c)) continue
                board[r][c] = player
                val win = hasFive(board, r, c, player)
                board[r][c] = 0
                if (win) return r to c
            }
        }
        return null
    }

    /** 检测 AI 是否有一步制造活四的走法（下一步必胜） */
    private fun findOpenFourMove(board: Array<IntArray>, player: Int): Pair<Int, Int>? {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] != 0) continue
                if (!isNearStones(board, r, c)) continue
                board[r][c] = player
                if (createsOpenFour(board, r, c, player)) {
                    board[r][c] = 0
                    return r to c
                }
                board[r][c] = 0
            }
        }
        return null
    }

    // 检测落子后是否形成活四
    private fun createsOpenFour(board: Array<IntArray>, r: Int, c: Int, player: Int): Boolean {
        for (dir in AIScoring.DIRECTIONS) {
            val chars = CharArray(11)
            chars[0] = '2'
            chars[10] = '2'
            for (i in -5..5) {
                val rr = r + i * dir.first
                val cc = c + i * dir.second
                chars[i + 5] = when {
                    rr !in 0 until size || cc !in 0 until size -> '2'
                    board[rr][cc] == 0 -> '0'
                    board[rr][cc] == player -> '1'
                    else -> '2'
                }
            }
            if ("011110" in chars.concatToString()) return true
        }
        return false
    }

    /** 收集候选位置：已有棋子周围 2 格内的空位（论文 3.1 Candidates Selection） */
    private fun getCandidates(board: Array<IntArray>): List<Pair<Int, Int>> {
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
        return candidates.toList()
    }

    /**
     * 获取排序后的候选走法：置换表最佳走法优先，其余按 quickEvaluate 降序，只取前 maxCandidates 个
     */
    private fun getOrderedCandidates(
        board: Array<IntArray>,
        player: Int,
        ttBestMove: Pair<Int, Int>?
    ): List<Pair<Int, Int>> {
        val candidates = getCandidates(board)
        if (candidates.isEmpty()) return emptyList()

        val scored = candidates.map { pos ->
            pos to BoardEvaluator.quickEvaluate(board, pos.first, pos.second, player)
        }.sortedByDescending { it.second }

        val result = mutableListOf<Pair<Int, Int>>()
        if (ttBestMove != null && ttBestMove in candidates) {
            result.add(ttBestMove)
        }
        for ((pos, _) in scored) {
            if (pos != ttBestMove) result.add(pos)
            if (result.size >= maxCandidates) break
        }
        return result
    }

    /**
     * 判断落子后是否产生威胁（活三/冲四/活四），返回扩展深度
     * 注意：调用前该点已被设为 player，用只读的 threatLevel 评估，避免破坏棋盘
     *
     * @param budget 单分支剩余扩展预算；扩展后不能超过总上限
     */
    private fun getThreatExtension(board: Array<IntArray>, r: Int, c: Int, player: Int, budget: Int): Int {
        if (budget <= 0) return 0
        val threat = BoardEvaluator.threatLevel(board, r, c, player)
        val raw = when {
            threat >= AIScoring.OPEN_FOUR -> 2   // 活四 → +2
            threat >= AIScoring.FOUR -> 1        // 冲四 → +1
            threat >= AIScoring.OPEN_THREE -> 1  // 活三 → +1
            else -> 0
        }
        return raw.coerceAtMost(budget)
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

    /** 检查位置是否靠近已有棋子（2 格内） */
    private fun isNearStones(board: Array<IntArray>, r: Int, c: Int): Boolean {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val rr = r + dr; val cc = c + dc
                if (rr in 0 until size && cc in 0 until size && board[rr][cc] != 0) return true
            }
        }
        return false
    }

    /** 棋盘是否为空 */
    private fun isEmpty(board: Array<IntArray>): Boolean {
        for (row in board) {
            for (v in row) {
                if (v != 0) return false
            }
        }
        return true
    }

    /** 是否超时 */
    private fun timeUp(): Boolean {
        return System.currentTimeMillis() - startTime >= timeLimitMs
    }
}
