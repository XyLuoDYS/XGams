package com.xyluodys.xgams

import com.xyluodys.xgams.Config.ConfigManager
import com.xyluodys.xgams.Gomoku.GomokuPlaceCommand
import com.xyluodys.xgams.database.PlayerPrefsDatabase
import com.xyluodys.xgams.dialog.gomoku.DialogRegistry
import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.module.database.Database
import taboolib.platform.BukkitPlugin

// 本插件由 @XyLuoDYS 开发，配合AI大模型以及工具辅助使用，@DeepSeek v4 pro & @GLM 5.2
object XGams : Plugin() {

    // 非常简洁的主类文件，打开的第一眼就很舒服 ~o( =∩ω∩= )m
    override fun onEnable() {
        console().sendMessage("""

            §8░░▒▒▓███  §e██   ██  §3 █████    §b████   §6█▄▄▄█  §e█████  §8███▓▒▒░░
            §8░░▒▒▓███   §e█ █ █   §3█        §b█    █  §6██ ██  §e█      §8███▓▒▒░░
            §8░░▒▒▓███    §e███    §3█   ███  §b██████  §6█ █ █  §e█████  §8███▓▒▒░░
            §8░░▒▒▓███   §e█ █ █   §3█     █  §b█    █  §6█   █      §e█  §8███▓▒▒░░
            §8░░▒▒▓███  §e██   ██  §3 █████   §b█    █  §6█   █  §e█████  §8███▓▒▒░░

        """)
        console().sendMessage("§7[§bXGams§7] §9插件加载成功！版本: §f${BukkitPlugin.getInstance().description.version}")

        initServices()
    }























    // 初始化插件各项服务：玩家偏好数据库（SQLite）的初始化与加载、
    //定时清理离线玩家数据的任务等 由 [onEnable] 在打印启动横幅后调用
    private fun initServices() {
        // 初始化玩家偏好数据库（SQLite），并载入已保存的棋盘尺寸
        try {
            PlayerPrefsDatabase.init(BukkitPlugin.getInstance().dataFolder)
            PlayerPrefsDatabase.loadAll().forEach { (uuid, size) ->
                DialogRegistry.setSize(uuid, size)
            }
            // 注册关闭钩子：插件关闭（DISABLE）且连接池释放前，自动保存当前所有玩家的棋盘尺寸
            Database.prepareClose {
                PlayerPrefsDatabase.saveAll(DialogRegistry.getAllSizes())
            }
            console().sendMessage("§7[§bXGams§7] §a玩家偏好数据库已就绪")
        } catch (e: Throwable) {
            console().sendMessage("§7[§bXGams§7] §c玩家偏好数据库初始化失败，偏好将无法持久化: §f${e.message}")
        }

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
