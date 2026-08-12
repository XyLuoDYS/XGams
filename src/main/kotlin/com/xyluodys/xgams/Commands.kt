package com.xyluodys.xgams

import com.xyluodys.xgams.Config.ConfigManager
import com.xyluodys.xgams.Config.GomokuGUIConfig
import com.xyluodys.xgams.Config.MessageManager
import com.xyluodys.xgams.Gomoku.GomokuGame
import com.xyluodys.xgams.Gomoku.GomokuPlaceCommand
import com.xyluodys.xgams.Gomoku.GomokuStartCommand
import com.xyluodys.xgams.dialog.gomoku.DialogRegistry
import com.xyluodys.xgams.dialog.gomoku.DifficultyDialog
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit

private val msg get() = MessageManager

// 注册主命令 xgams ， 并定义别名 xg xgs
@CommandHeader(name = "xgams", aliases = ["xg", "xgs"])
object xgamsCommands {

    // reload子命令，需 xgams.admin 权限
    @CommandBody(permission = "xgams.admin")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            ConfigManager.reload()
            MessageManager.reload()
            GomokuGUIConfig.reload()
            sender.sendMessage(msg.reloadSuccess)
        }
    }
}

// 主命令gomoku，子命令gobang
@CommandHeader(name = "gomoku", aliases = ["gobang"])
object gomokuCommands {

    // main子命令，继续游戏（保留已有棋局）
    @CommandBody
    val main = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val player = sender.castSafely<Player>()
            if (player != null) {
                GomokuStartCommand().execute(player, "gomoku", emptyArray())
            } else {
                sender.sendMessage(msg.playerOnly)
            }
        }
    }

    // play子命令，开始新一局 — 先弹出难度选择对话框
    @CommandBody
    val play = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val player = sender.castSafely<Player>()
            if (player != null) {
                DifficultyDialog.open(player)
            } else {
                sender.sendMessage(msg.playerOnly)
            }
        }
    }

    // difficulty子命令，选择难度
    // 用法: /gomoku difficulty <easy|medium|hard>
    @CommandBody
    val difficulty = subCommand {
        execute<ProxyCommandSender> { sender, _, args ->
            val player = sender.castSafely<Player>()
            if (player == null) {
                sender.sendMessage(msg.playerOnly)
                return@execute
            }
            val tokens = args.split(" ").filter { it.isNotEmpty() }
            val diffArg = if (tokens.firstOrNull()?.lowercase() == "difficulty") {
                tokens.drop(1).firstOrNull()?.lowercase()
            } else {
                tokens.firstOrNull()?.lowercase()
            }
            val difficulty = when (diffArg) {
                "easy", "简单" -> "easy"
                "medium", "中等" -> "medium"
                "hard", "困难" -> "hard"
                else -> "medium"
            }
            // 清除旧棋局，用选定的难度创建新棋局
            GomokuPlaceCommand.gameMap.remove(player.uniqueId)
            val game = GomokuPlaceCommand.gameMap.getOrPut(player.uniqueId) { GomokuGame(difficulty) }
            DialogRegistry.openGomokuDialog(player, game)
        }
    }

    // place子命令，落子，用法/gomoku place <行> <列>
    @CommandBody
    val place = subCommand {
        execute<ProxyCommandSender> { sender, _, args ->
            val player = sender.castSafely<Player>()
            if (player == null) {
                sender.sendMessage(msg.playerOnly)
                return@execute
            }
            // subCommand 在 execute 中会把子命令名本身也包含在 args 里
            // 所以需跳过 "place"
            val tokens = args.split(" ").filter { it.isNotEmpty() }
            val placeArgs = if (tokens.firstOrNull()?.lowercase() == "place") {
                tokens.drop(1).toTypedArray()
            } else {
                tokens.toTypedArray()
            }
            GomokuPlaceCommand().execute(player, "gomoku", placeArgs)
        }
    }

    // toggle子命令，切换棋盘大小
    @CommandBody
    val toggle = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val player = sender.castSafely<Player>()
            if (player == null) {
                sender.sendMessage(msg.playerOnly)
                return@execute
            }
            val game = GomokuPlaceCommand.gameMap[player.uniqueId]
            if (game == null) {
                player.sendMessage(msg.noGame)
                return@execute
            }
            DialogRegistry.toggleSize(player)
            DialogRegistry.openGomokuDialog(player, game)
        }
    }

    // close子命令，关闭对话框（仅此核心功能）
    @CommandBody
    val close = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val player = sender.castSafely<Player>()
            if (player == null) {
                sender.sendMessage(msg.playerOnly)
                return@execute
            }
            // 结束当前棋局会话：移除记录，避免 AI 残留任务又把对话框弹回
            GomokuPlaceCommand.gameMap.remove(player.uniqueId)
            // 关闭客户端对话框（Paper Dialog API 的清除命令须在全局主线程派发，
            // 故用 submit 切回主线程执行，避免 Folia 区域化下的 Dispatching command async）
            submit {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dialog clear ${player.name}")
            }
        }
    }
}
/**
 * AI不让我说脏话
 * cccccccccccccccc
 */