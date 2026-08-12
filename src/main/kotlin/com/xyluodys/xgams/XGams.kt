package com.xyluodys.xgams

import com.xyluodys.xgams.Config.ConfigManager
import com.xyluodys.xgams.Gomoku.GomokuPlaceCommand
import com.xyluodys.xgams.dialog.gomoku.DialogRegistry
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.platform.BukkitPlugin

// 本插件由 @XyLuoDYS 开发，配合AI大模型以及工具辅助使用，@DeepSeek v4 pro & @GLM 5.2
object XGams : Plugin() {
    // 插件版本变量定义
    val pluginVersion: String by lazy {
        BukkitPlugin.getInstance().description.version
    }

    override fun onEnable() {
        console().sendMessage("""
            
            §8░░▒▒▓███  §e██   ██  §3 █████    §b████   §6█▄▄▄█  §e█████  §8███▓▒▒░░
            §8░░▒▒▓███   §e█ █ █   §3█        §b█    █  §6██ ██  §e█      §8███▓▒▒░░
            §8░░▒▒▓███    §e███    §3█   ███  §b██████  §6█ █ █  §e█████  §8███▓▒▒░░
            §8░░▒▒▓███   §e█ █ █   §3█     █  §b█    █  §6█   █      §e█  §8███▓▒▒░░
            §8░░▒▒▓███  §e██   ██  §3 █████   §b█    █  §6█   █  §e█████  §8███▓▒▒░░
        
        """)
        console().sendMessage("§7[§bXGams§7] §9插件加载成功！版本: §f${pluginVersion}")

        // 使用 TabooLib 的 submit 调度器
        val intervalMinutes = ConfigManager.config.getInt("cleanup.interval-minutes", 5)
        val intervalTicks = intervalMinutes * 60L * 20L
        submit(delay = intervalTicks, period = intervalTicks) {
            val onlineIds = Bukkit.getOnlinePlayers().map { it.uniqueId }.toSet()
            GomokuPlaceCommand.cleanupOffline(onlineIds)
            DialogRegistry.cleanupOffline(onlineIds)
        }

        if (ConfigManager.config.getBoolean("cleanup.message", false)) {
            return
        }

        console().sendMessage("§7[§bXGams§7] §a定时清理任务已启动，间隔: §f${intervalMinutes}§a 分钟")
    }
}
