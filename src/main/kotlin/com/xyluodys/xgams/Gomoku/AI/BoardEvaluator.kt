package com.xyluodys.xgams.Gomoku.AI

/**
 * 模式状态机评估器
 */
object BoardEvaluator {

    private const val SIZE = 15

    // 四个扫描方向
    private val DIRECTIONS = AIScoring.DIRECTIONS

    /**
     * 评估整个棋盘，返回 AI 视角的分数
     */
    fun evaluate(board: Array<IntArray>): Int {
        return evaluateFor(board, 2) - evaluateFor(board, 1)
    }

    /**
     * 评估指定玩家的棋盘总分
     */
    private fun evaluateFor(board: Array<IntArray>, player: Int): Int {
        var score = 0
        for ((dr, dc) in DIRECTIONS) {
            for (line in extractLines(board, dr, dc)) {
                score += scoreLine(line, player)
            }
        }
        return score
    }

    /**
     * 沿指定方向提取棋盘上的所有线
     */
    private fun extractLines(board: Array<IntArray>, dr: Int, dc: Int): List<IntArray> {
        val lines = mutableListOf<IntArray>()

        if (dr == 0) {
            for (r in 0 until SIZE) lines.add(board[r].copyOf())
        } else if (dc == 0) {
            for (c in 0 until SIZE) lines.add(IntArray(SIZE) { r -> board[r][c] })
        } else if (dr == 1 && dc == 1) {
            for (c in 0 until SIZE) {
                val line = mutableListOf<Int>()
                var r = 0; var cc = c
                while (r < SIZE && cc < SIZE) { line.add(board[r][cc]); r++; cc++ }
                lines.add(line.toIntArray())
            }
            for (r in 1 until SIZE) {
                val line = mutableListOf<Int>()
                var rr = r; var c = 0
                while (rr < SIZE && c < SIZE) { line.add(board[rr][c]); rr++; c++ }
                lines.add(line.toIntArray())
            }
        } else {
            for (c in 0 until SIZE) {
                val line = mutableListOf<Int>()
                var r = 0; var cc = c
                while (r < SIZE && cc >= 0) { line.add(board[r][cc]); r++; cc-- }
                lines.add(line.toIntArray())
            }
            for (r in 1 until SIZE) {
                val line = mutableListOf<Int>()
                var rr = r; var c = SIZE - 1
                while (rr < SIZE && c >= 0) { line.add(board[rr][c]); rr++; c-- }
                lines.add(line.toIntArray())
            }
        }

        return lines
    }

    /**
     * 对一条线进行模式匹配，返回指定玩家的得分
     */
    private fun scoreLine(line: IntArray, player: Int): Int {
        val n = line.size
        val chars = CharArray(n + 2)
        chars[0] = '2'
        chars[n + 1] = '2'
        for (i in 0 until n) {
            val v = line[i]
            chars[i + 1] = when {
                v == 0 -> '0'
                v == player -> '1'
                else -> '2'
            }
        }

        var score = 0
        for ((pattern, value) in AIScoring.PATTERNS) {
            val plen = pattern.length
            if (plen > chars.size) continue
            var i = 0
            while (i <= chars.size - plen) {
                var match = true
                for (j in 0 until plen) {
                    if (chars[i + j] != pattern[j]) { match = false; break }
                }
                if (match) {
                    score += value
                    for (j in 0 until plen) chars[i + j] = '.'
                    i += plen
                } else {
                    i++
                }
            }
        }
        return score
    }

    /**
     * 判定已落子点 (row,col) 在某方向上形成的棋型等级
     * 要求调用前该点已被设为 player（中心='1'） 只读，不修改棋盘
     *
     * 提取 11 格线段 [中心±5]，两端虚拟边界 '2'，用 SHAPES 从高到低匹配，
     * 第一个覆盖范围包含中心(索引5)的匹配即返回其等级
     */
    fun directionShape(board: Array<IntArray>, row: Int, col: Int, dr: Int, dc: Int, player: Int): Int {
        val chars = CharArray(11)
        chars[0] = '2'
        chars[10] = '2'
        for (i in -5..5) {
            val r = row + i * dr
            val c = col + i * dc
            chars[i + 5] = when {
                r !in 0 until SIZE || c !in 0 until SIZE -> '2'
                board[r][c] == 0 -> '0'
                board[r][c] == player -> '1'
                else -> '2'
            }
        }
        for ((pattern, level) in AIScoring.SHAPES) {
            val plen = pattern.length
            if (plen > chars.size) continue
            var i = 0
            while (i <= chars.size - plen) {
                var match = true
                for (j in 0 until plen) {
                    if (chars[i + j] != pattern[j]) { match = false; break }
                }
                // 匹配且覆盖范围包含中心(索引5)
                if (match && i <= 5 && i + plen - 1 >= 5) {
                    return level
                }
                i++
            }
        }
        return AIScoring.NONE
    }

    /**
     * 已落子后评估（只读，不修改棋盘）：4 方向组合威胁分析
     * 识别双四 / 四三 / 双三等复合杀，否则累加各方向基础分
     */
    private fun analyzeAfter(board: Array<IntArray>, row: Int, col: Int, player: Int): Int {
        val shapes = IntArray(4)
        for ((idx, dir) in DIRECTIONS.withIndex()) {
            shapes[idx] = directionShape(board, row, col, dir.first, dir.second, player)
        }
        val fives = shapes.count { it == AIScoring.FIVE }
        if (fives > 0) return AIScoring.FIVE

        val openFours = shapes.count { it == AIScoring.OPEN_FOUR }
        val fours = shapes.count { it == AIScoring.FOUR }
        val threes = shapes.count { it == AIScoring.OPEN_THREE }
        val totalFours = openFours + fours

        // 组合威胁（在单形基础上额外加成，识别双杀）
        if (openFours >= 2) return AIScoring.DOUBLE_FOUR
        if (totalFours >= 2) return AIScoring.DOUBLE_FOUR
        if (totalFours >= 1 && threes >= 1) return AIScoring.FOUR_THREE
        if (threes >= 2) return AIScoring.DOUBLE_THREE

        // 单形：累加各方向的形状分（而非只取最大值），
        // 这样「双活二」「多方向威胁」能被正确反映，避免走法排序丢失信息；
        // 活三(1000) 仍远高于单活二(50)，保证搜索与排序都优先延伸最强棋形
        return shapes.sumOf { AIScoring.shapeScore(it) }
    }

    /**
     * 已落子后评估（只读）：供威胁扩展判断使用，不修改棋盘
     */
    fun threatLevel(board: Array<IntArray>, row: Int, col: Int, player: Int): Int {
        return analyzeAfter(board, row, col, player)
    }

    /**
     * 模拟在 (row,col) 落子后的攻防综合价值（用于走法排序）
     *
     * 进攻：模拟己方落子后分析；防守：临时模拟对方落子后分析
     * 取 max(进攻, 防守×权重)
     */
    fun quickEvaluate(board: Array<IntArray>, row: Int, col: Int, player: Int): Int {
        val opponent = if (player == 1) 2 else 1
        // 进攻：模拟己方落子
        board[row][col] = player
        val offense = analyzeAfter(board, row, col, player)
        // 防守：临时模拟对方落子
        board[row][col] = opponent
        val defense = analyzeAfter(board, row, col, opponent)
        // 恢复空格
        board[row][col] = 0
        return maxOf(offense, (defense * AIScoring.DEFENSE_WEIGHT).toInt())
    }
}
