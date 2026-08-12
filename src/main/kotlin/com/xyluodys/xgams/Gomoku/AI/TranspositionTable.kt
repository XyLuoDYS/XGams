package com.xyluodys.xgams.Gomoku.AI

import java.util.Random

/**
 * Zobrist 置换表
 *
 * 1. Zobrist 哈希：每个 (位置, 玩家) 组合对应一个随机 64 位数
 *    棋盘哈希 = 所有已落子位置的随机数 XOR，支持 O(1) 增量更新
 * 2. 开放寻址：使用并行数组（LongArray + IntArray×3）代替 HashMap，
 *    避免对象分配和 GC 压力
 * 3. 替换策略：深度优先——新条目深度 ≥ 旧条目才覆盖
 */
class TranspositionTable(capacity: Int = 65536) {

    companion object {
        const val FLAG_EMPTY = 0
        const val FLAG_EXACT = 1
        const val FLAG_LOWER_BOUND = 2
        const val FLAG_UPPER_BOUND = 3
    }

    private val mask = capacity - 1

    // 并行数组：keys[i], depths[i], scores[i], flags[i] 对应同一个槽位
    private val keys = LongArray(capacity)
    private val depths = IntArray(capacity)
    private val scores = IntArray(capacity)
    private val flags = IntArray(capacity)

    // Zobrist 随机数表：[row][col][player]，player: 1=黑, 2=白
    private val zobrist: Array<Array<LongArray>> = Array(15) {
        Array(15) { LongArray(3) }
    }

    init {
        val random = Random(0x5EED_5EEDL)
        for (r in 0 until 15) {
            for (c in 0 until 15) {
                zobrist[r][c][1] = random.nextLong()
                zobrist[r][c][2] = random.nextLong()
            }
        }
    }

    /**
     * 计算棋盘的 Zobrist 哈希值
     */
    fun computeHash(board: Array<IntArray>): Long {
        var hash = 0L
        for (r in 0 until 15) {
            for (c in 0 until 15) {
                val v = board[r][c]
                if (v != 0) {
                    hash = hash xor zobrist[r][c][v]
                }
            }
        }
        return hash
    }

    /**
     * 增量更新哈希（落子/撤销时调用）
     */
    fun toggleStone(hash: Long, row: Int, col: Int, player: Int): Long {
        return hash xor zobrist[row][col][player]
    }

    /**
     * 查询置换表
     *
     * @return 如果命中且深度足够，返回 (score, flag)，否则返回 null
     */
    fun get(hash: Long, minDepth: Int): Triple<Int, Int, Int>? {
        var idx = (hash.toInt() and Int.MAX_VALUE) and mask
        // 线性探测（最多探测 4 个槽位）
        for (probe in 0 until 4) {
            val i = (idx + probe) and mask
            if (keys[i] == 0L) return null  // 空槽
            if (keys[i] == hash && depths[i] >= minDepth) {
                return Triple(scores[i], depths[i], flags[i])
            }
        }
        return null
    }

    /**
     * 存入置换表
     *
     * 替换策略：空槽直接写入；同 key 更新；深度优先（新 ≥ 旧才覆盖）
     */
    fun put(hash: Long, score: Int, depth: Int, flag: Int) {
        var idx = (hash.toInt() and Int.MAX_VALUE) and mask
        for (probe in 0 until 4) {
            val i = (idx + probe) and mask
            if (keys[i] == 0L || keys[i] == hash) {
                keys[i] = hash
                depths[i] = depth
                scores[i] = score
                flags[i] = flag
                return
            }
            // 深度优先替换
            if (depths[i] < depth) {
                keys[i] = hash
                depths[i] = depth
                scores[i] = score
                flags[i] = flag
                return
            }
        }
        // 所有槽位都被占用且深度更高，覆盖第一个
        keys[idx] = hash
        depths[idx] = depth
        scores[idx] = score
        flags[idx] = flag
    }

    /**
     * 清空置换表
     */
    fun clear() {
        keys.fill(0L)
    }
}
