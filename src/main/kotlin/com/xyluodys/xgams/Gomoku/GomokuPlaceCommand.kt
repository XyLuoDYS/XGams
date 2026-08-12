package com.xyluodys.xgams.Gomoku

import com.xyluodys.xgams.Config.GomokuGUIConfig
import com.xyluodys.xgams.Config.MessageManager
import com.xyluodys.xgams.Gomoku.AI.GomokuAI
import com.xyluodys.xgams.dialog.gomoku.DialogRegistry.openGomokuDialog
import com.xyluodys.xgams.util.SoundUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GomokuPlaceCommand {

    fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(MessageManager.playerOnly)
            return true
        }

        // 参数不足时给出明确提示，不再 return false 触发 Bukkit 默认 "错误的参数"
        if (args.size < 2) {
            sender.sendMessage(MessageManager.placeUsage)
            return true
        }

        val player = sender
        val row = args[0].toIntOrNull()
        val col = args[1].toIntOrNull()

        // 非数字参数给出明确提示，不再 return false
        if (row == null || col == null) {
            sender.sendMessage(MessageManager.placeInvalid
                .replace("{arg1}", args[0])
                .replace("{arg2}", args[1]))
            return true
        }

        val game = gameMap.getOrPut(player.uniqueId) { GomokuGame() }

        // 游戏已结束，不允许继续落子
        if (game.gameOver) {
            player.sendMessage(MessageManager.gameOver)
            openGomokuDialog(player, game)
            return true
        }

        // AI 正在思考，忽略落子请求
        if (game.aiThinking) {
            return true
        }

        val success = game.place(row, col)

        if (success) {
            // 先刷新 dialog，让玩家看到自己落子（带标记）
            openGomokuDialog(player, game)
            // 播放落子音效（随机播放，支持随机音调）
            if (GomokuGUIConfig.placeSounds.isNotEmpty()) {
                val pitch = if (GomokuGUIConfig.placePitchEnabled && GomokuGUIConfig.placePitches.isNotEmpty()) {
                    GomokuGUIConfig.placePitches.random()
                } else {
                    1.0f
                }
                SoundUtil.play(player, GomokuGUIConfig.placeSounds.random(), pitch)
            }

            if (game.gameOver) {
                if (game.winner > 0) {
                    // 玩家获胜 — 启动连线标记动画
                    startWinAnimation(player, game)
                } else {
                    // 平局 — showGameOverMessage 已在 place() 中置 true
                    player.sendMessage(MessageManager.draw)
                }
                return true
            }

            // AI 延迟落子，让玩家先看到自己的标记
            game.aiThinking = true
            submit(delay = 20L) {
                if (!player.isOnline || GomokuPlaceCommand.gameMap[player.uniqueId] != game) {
                    game.aiThinking = false
                    return@submit
                }
                val aiMove = GomokuAI(game.difficulty).findBestMove(game.board)
                if (aiMove != null) {
                    game.place(aiMove.first, aiMove.second)
                }
                game.aiThinking = false
                if (GomokuPlaceCommand.gameMap[player.uniqueId] != game) return@submit
                openGomokuDialog(player, game)
                // 播放落子音效（随机播放，支持随机音调）
                if (GomokuGUIConfig.placeSounds.isNotEmpty()) {
                    val pitch = if (GomokuGUIConfig.placePitchEnabled && GomokuGUIConfig.placePitches.isNotEmpty()) {
                        GomokuGUIConfig.placePitches.random()
                    } else {
                        1.0f
                    }
                    SoundUtil.play(player, GomokuGUIConfig.placeSounds.random(), pitch)
                }

                if (game.gameOver) {
                    if (game.winner > 0) {
                        // AI 获胜 — 启动连线标记动画
                        startWinAnimation(player, game)
                    } else {
                        // 平局
                        player.sendMessage(MessageManager.draw)
                    }
                }
            }
        } else {
            openGomokuDialog(player, game)
            if (row !in 0..14 || col !in 0..14) {
                player.sendMessage(MessageManager.placeBounds)
            } else {
                player.sendMessage(MessageManager.placeOccupied)
            }
        }

        return true
    }

    /**
     * 五子连珠获胜动画
     */
    private fun startWinAnimation(player: Player, game: GomokuGame) {
        val winLine = game.winningLine
        if (winLine.isEmpty()) return

        val markDelay = GomokuGUIConfig.winMarkDelay.toLong()
        val messageDelay = GomokuGUIConfig.winMessageDelay.toLong()

        // 逐颗标记连线的棋子
        winLine.forEachIndexed { index, _ ->
            submit(delay = markDelay * (index + 1)) {
                if (!player.isOnline || GomokuPlaceCommand.gameMap[player.uniqueId] != game) return@submit
                game.winMarkProgress = index + 1
                openGomokuDialog(player, game)
                // 播放连线标记音效（仅前5颗，不足则循环播放）
                if (index < 5 && GomokuGUIConfig.winMarkSounds.isNotEmpty()) {
                    val soundIndex = index % GomokuGUIConfig.winMarkSounds.size
                    SoundUtil.play(player, GomokuGUIConfig.winMarkSounds[soundIndex])
                }
            }
        }

        // 全部标记完成后，延迟 messageDelay 显示游戏结束消息
        val totalDelay = markDelay * winLine.size + messageDelay
        submit(delay = totalDelay) {
            if (!player.isOnline || GomokuPlaceCommand.gameMap[player.uniqueId] != game) return@submit
            game.showGameOverMessage = true
            openGomokuDialog(player, game)

            val winnerName = if (game.winner == 1) "您" else "对方"
            player.sendMessage(MessageManager.win.replace("{winner}", winnerName))
        }
    }

    companion object {
        // 每个玩家一局游戏
        val gameMap = ConcurrentHashMap<UUID, GomokuGame>()

        /** 清理离线玩家的游戏数据 */
        fun cleanupOffline(onlineIds: Set<UUID>) {
            gameMap.keys.retainAll(onlineIds)
        }
    }
}
