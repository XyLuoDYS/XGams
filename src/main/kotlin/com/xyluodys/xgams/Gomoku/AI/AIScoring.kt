package com.xyluodys.xgams.Gomoku.AI

/**
 * 棋型分数与模式定义
 */
object AIScoring {

    const val FIVE = 1_000_000           // 五连
    const val WIN = FIVE                 // 别名：获胜分值
    const val OPEN_FOUR = 100_000        // 活四（两端开放，下一步必胜）
    const val FOUR = 10_000              // 冲四（含跳冲四）
    const val OPEN_THREE = 1_000         // 活三（含跳活三）
    const val THREE = 100                // 眠三
    const val OPEN_TWO = 100             // 活二（含跳活二）
    const val TWO = 10                   // 眠二
    const val ONE = 1                    // 单子
    const val NONE = 0                   // 无棋型

    // ===== 复合威胁（文档第三章第三步：组合威胁扫描） =====
    const val DOUBLE_THREE = 2_000       // 双活三
    const val FOUR_THREE = 50_000        // 四三杀（冲四+活三）
    const val DOUBLE_FOUR = 200_000      // 双冲四 / 双四（必胜等效）

    // 防守权重 < 1.0，保证 AI 优先自己赢 > 阻止对方赢
    const val DEFENSE_WEIGHT = 0.9

    // 四个评估方向：横向、纵向、右下斜、左下斜
    val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    /**
     * 逐线评估模式（按分数降序）
     */
    val PATTERNS: List<Pair<String, Int>> = listOf(
        "11111" to FIVE,
        "011110" to OPEN_FOUR,
        "211110" to FOUR, "011112" to FOUR, "10111" to FOUR, "11011" to FOUR, "11101" to FOUR,
        "011100" to OPEN_THREE, "001110" to OPEN_THREE, "010110" to OPEN_THREE, "011010" to OPEN_THREE,
        "211100" to THREE, "001112" to THREE, "210110" to THREE, "011012" to THREE, "211010" to THREE, "010112" to THREE,
        "001100" to OPEN_TWO, "010100" to OPEN_TWO, "001010" to OPEN_TWO, "010010" to OPEN_TWO,
        "211000" to TWO, "000112" to TWO,
    )

    /**
     * 方向棋型判定模式（按等级降序）
     * 用于 [BoardEvaluator.directionShape]：给定一个以中心(窗口索引5)为核心的 11 格线段，
     * 若某模式匹配且其覆盖范围包含中心，则返回对应等级
     */
    val SHAPES: List<Pair<String, Int>> = listOf(
        "11111" to FIVE,
        "011110" to OPEN_FOUR,
        "211110" to FOUR, "011112" to FOUR, "10111" to FOUR, "11011" to FOUR, "11101" to FOUR,
        "011100" to OPEN_THREE, "001110" to OPEN_THREE, "010110" to OPEN_THREE, "011010" to OPEN_THREE,
        "211100" to THREE, "001112" to THREE, "210110" to THREE, "011012" to THREE, "211010" to THREE, "010112" to THREE,
        "001100" to OPEN_TWO, "010100" to OPEN_TWO, "001010" to OPEN_TWO, "010010" to OPEN_TWO,
        "211000" to TWO, "000112" to TWO,
    )

    /**
     * 棋型等级 -> 单形分值（用于组合威胁扫描的基础分）
     * 关键：单个活四 / 冲四 / 五连 也必须给出对应高分，
     * 否则防守时会把"对方落子成四"误判为 0 分而漏防
     */
    fun shapeScore(level: Int): Int = when (level) {
        FIVE -> FIVE
        OPEN_FOUR -> 100_000
        FOUR -> 10_000
        OPEN_THREE -> 1_000
        THREE -> 100
        OPEN_TWO -> 50
        TWO -> 10
        ONE -> 1
        else -> 0
    }
}
