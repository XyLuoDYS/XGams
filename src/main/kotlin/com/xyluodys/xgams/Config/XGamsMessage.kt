package com.xyluodys.xgams.Config

import com.xyluodys.xgams.util.TextParser
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.common.platform.Awake
import taboolib.common.LifeCycle

object MessageManager {
    @Config("message.yml")
    lateinit var config: Configuration

    var playerOnly: String = "§c只有玩家可以使用此命令!"
    var noGame: String = "§c你还没有开始游戏!"
    var placeUsage: String = "§c用法: /gomoku place <行> <列>"
    var placeInvalid: String = "§c参数错误: 行和列都必须是数字! 你输入的是 {arg1} {arg2}"
    var placeBounds: String = "§c落子失败: 坐标越界，行列范围 0-14!"
    var placeOccupied: String = "§c落子失败: 该位置已有棋子!"
    var gameOver: String = "§c游戏已结束！使用 /gomoku play 开始新局"
    var win: String = "§e{winner}获胜！使用 /gomoku play 开始新局"
    var draw: String = "§e平局！棋盘已满，使用 /gomoku play 开始新局"
    var reloadSuccess: String = "§a配置文件已重新加载！"
    var defaultDisabled: String = "§c该命令已被禁用！"

    @Awake(LifeCycle.ENABLE)
    fun load() {
        config.onReload { updateVars() }
        updateVars()
    }

    fun reload() {
        config.reload()
    }

    private fun updateVars() {
        playerOnly = TextParser.unescape(config.getString("commands.gomoku.player-only")) ?: "§c只有玩家可以使用此命令!"
        noGame = TextParser.unescape(config.getString("commands.gomoku.no-game")) ?: "§c你还没有开始游戏!"
        placeUsage = TextParser.unescape(config.getString("commands.gomoku.place-usage")) ?: "§c用法: /gomoku place <行> <列>"
        placeInvalid = TextParser.unescape(config.getString("commands.gomoku.place-invalid")) ?: "§c参数错误: 行和列都必须是数字! 你输入的是 {arg1} {arg2}"
        placeBounds = TextParser.unescape(config.getString("commands.gomoku.place-bounds")) ?: "§c落子失败: 坐标越界，行列范围 0-14!"
        placeOccupied = TextParser.unescape(config.getString("commands.gomoku.place-occupied")) ?: "§c落子失败: 该位置已有棋子!"
        gameOver = TextParser.unescape(config.getString("commands.gomoku.game-over")) ?: "§c游戏已结束！使用 /gomoku play 开始新局"
        win = TextParser.unescape(config.getString("commands.gomoku.win")) ?: "§e{winner}获胜！使用 /gomoku play 开始新局"
        draw = TextParser.unescape(config.getString("commands.gomoku.draw")) ?: "§e平局！棋盘已满，使用 /gomoku play 开始新局"
        reloadSuccess = TextParser.unescape(config.getString("commands.gomoku.reload-success")) ?: "§a配置文件已重新加载！"
        defaultDisabled = TextParser.unescape(config.getString("commands.default.disabled")) ?: "§c该命令已被禁用！"
    }
}
