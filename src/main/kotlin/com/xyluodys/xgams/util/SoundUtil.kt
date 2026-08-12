package com.xyluodys.xgams.util

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound as AdventureSound
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * 音效播放工具 喵 (。・ω・。)
 *
 * 两种音效名称格式：
 * 1. 原版 Sound 枚举名（如 "BLOCK_STONE_PLACE"、"block.stone.place"）
 * 2. 自定义资源包音效（如 "xgams:gomoku.place"、"minecraft:block.note_block.pling"）
 */
object SoundUtil {

    fun play(player: Player, soundName: String, pitch: Float = 1.0f) {
        // 尝试作为原版 Sound 枚举播放（将 . 和 - 替换为 _ 并大写）
        val bukkitSound = runCatching {
            Sound.valueOf(soundName.uppercase().replace(".", "_").replace("-", "_"))
        }.getOrNull()

        if (bukkitSound != null) {
            player.playSound(AdventureSound.sound(bukkitSound, AdventureSound.Source.MASTER, 1.0f, pitch))
            return
        }

        // 作为自定义音效键播放（资源包音效）
        runCatching {
            val key = Key.key(soundName)
            player.playSound(AdventureSound.sound(key, AdventureSound.Source.MASTER, 1.0f, pitch))
        }
    }
}
