package com.xyluodys.xgams.Config

import com.xyluodys.xgams.util.TextParser
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import taboolib.common.platform.event.SubscribeEvent

object DisabledCommandsMessageManager {

    /** 命令别名 → 主命令名映射，确保别名也能被禁用检查覆盖 */
    private val aliasMap = mapOf(
        "gobang" to "gomoku",
        "xg" to "xgams",
        "xgs" to "xgams"
    )

    /** 将别名解析为主命令名 */
    fun resolveMainCommand(input: String): String {
        return aliasMap[input] ?: input
    }

    /** 从 message.yml 读取 commands.<cmd>.disabled，无配置则用默认值 */
    fun getDisabledMsg(cmd: String): String {
        return TextParser.unescape(
            MessageManager.config.getString("commands.$cmd.disabled")
        ) ?: MessageManager.defaultDisabled
    }
}


@SubscribeEvent
fun onCommand(event: PlayerCommandPreprocessEvent) {
    val parts = event.message.removePrefix("/").lowercase().split(" ")
    if (parts.isEmpty()) return

    // 将别名映射为主命令名，防止别名绕过禁用检查
    val root = parts[0]
    val mainCmd = DisabledCommandsMessageManager.resolveMainCommand(root)

    if (!ConfigManager.config.getBoolean("commands.$mainCmd.enabled", true)) {
        event.player.sendMessage(DisabledCommandsMessageManager.getDisabledMsg(mainCmd))
        event.isCancelled = true
    }
}
