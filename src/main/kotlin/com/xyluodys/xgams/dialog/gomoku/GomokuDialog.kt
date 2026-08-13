package com.xyluodys.xgams.dialog.gomoku

import com.xyluodys.xgams.Config.GomokuGUIConfig
import com.xyluodys.xgams.Gomoku.GomokuGame
import com.xyluodys.xgams.database.PlayerPrefsDatabase
import com.xyluodys.xgams.util.TextParser
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DialogRegistry {

    // ConcurrentHashMap 保证 Folia 多线程环境下的线程安全
    private val sizeMap = ConcurrentHashMap<UUID, String>()

    fun getSize(player: Player): String = sizeMap.getOrDefault(player.uniqueId, "big")

    // 直接设置某玩家的棋盘尺寸（用于启动时从数据库载入）
    fun setSize(uuid: UUID, size: String) {
        sizeMap[uuid] = size
    }

    // 获取当前所有玩家的棋盘尺寸映射（用于关闭时批量保存）
    fun getAllSizes(): Map<UUID, String> = sizeMap.toMap()

    fun toggleSize(player: Player) {
        val current = getSize(player)
        val actions = GomokuGUIConfig.getActions(current)
        for (action in actions) {
            val parts = action.split(":")
            if (parts.size >= 2 && parts[0].trim() == "change") {
                val next = parts[1].trim()
                sizeMap[player.uniqueId] = next
                // 切换后即时持久化，避免服务器异常退出时丢失本次设置
                submit(async = true) { PlayerPrefsDatabase.setBoardSize(player.uniqueId, next) }
            }
        }
    }

    // 清理离线玩家的尺寸偏好数据
    fun cleanupOffline(onlineIds: Set<UUID>) {
        sizeMap.keys.retainAll(onlineIds)
    }

    // 渲染单个棋盘格子
    private fun renderCell(raw: String, useTexture: Boolean, boardRow: Int, col: Int): Component {
        return TextParser.parse(raw, GomokuGUIConfig.fontNamespace, useTexture)
            .clickEvent(ClickEvent.runCommand("/gomoku place $boardRow $col"))
            .hoverEvent(HoverEvent.showText(
                TextParser.parse(
                    GomokuGUIConfig.cellHoverTemplate
                        .replace("{row}", (boardRow + 1).toString())
                        .replace("{col}", (col + 1).toString()),
                    null, false
                )
            ))
    }

    // 渲染切换按钮
    private fun renderButton(text: String, useTexture: Boolean): Component {
        return TextParser.parse(text, GomokuGUIConfig.fontNamespace, useTexture)
            .clickEvent(ClickEvent.runCommand("/gomoku toggle"))
            .hoverEvent(HoverEvent.showText(
                TextParser.parse(GomokuGUIConfig.toggleHover, null, false)
            ))
    }

    private val colorCodeRegex = Regex("&(#[0-9a-fA-F]{6}|[0-9a-fk-or])")

    /**
     * 棋盘对话框绘制
     */
    fun openGomokuDialog(player: Player, game: GomokuGame, size: String = getSize(player)) {
        sizeMap[player.uniqueId] = size

        val layout = GomokuGUIConfig.getLayout(size)
        val useTexture = GomokuGUIConfig.textureEnabled

        // 构建标记字符 → 区域名称的反查表
        val markerToRegion = GomokuGUIConfig.cellMarkers.entries.associate { (region, marker) -> marker to region }
        val boardComponent = Component.text()

        // 找到第4个非空行在 layout 中的索引（按钮行）
        val buttonDisplayRow = 3
        val buttonLineIndex = layout.withIndex()
            .filter { it.value.isNotEmpty() }
            .elementAtOrNull(buttonDisplayRow)?.index ?: buttonDisplayRow

        // 找到第7个非空行在 layout 中的索引（游戏结束消息行）
        val messageDisplayRow = 6
        val messageLineIndex = layout.withIndex()
            .filter { it.value.isNotEmpty() }
            .elementAtOrNull(messageDisplayRow)?.index ?: messageDisplayRow

        // 预计算按钮宽度，保证所有行右对齐
        val btnText = GomokuGUIConfig.getToggleButtonText(size)
        val btnPlain = btnText.replace(colorCodeRegex, "")
        val btnCharWidth = btnPlain.sumOf { c -> if (c.code > 0x7F) 2 else 1 }
        val btnComp = renderButton(btnText, useTexture)

        // 预计算游戏结束消息宽度（仅在游戏结束消息显示时）
        val msgText = if (game.showGameOverMessage) GomokuGUIConfig.getGameOverMessage(game.winner, size) else ""
        val msgPlain = msgText.replace(colorCodeRegex, "")
        val msgCharWidth = msgPlain.sumOf { c -> if (c.code > 0x7F) 2 else 1 }
        val msgComp = if (game.showGameOverMessage) TextParser.parse(msgText, GomokuGUIConfig.fontNamespace, useTexture) else null

        val row4Gap = " ".repeat(GomokuGUIConfig.getButtonSpacing(size))
        val msgGap = " ".repeat(GomokuGUIConfig.getMessageSpacing(size))
        val otherRowGap = " ".repeat(GomokuGUIConfig.getOtherRowSpacing(size)) + " ".repeat(btnCharWidth)
        val messageRowGap = " ".repeat(GomokuGUIConfig.getMessageRowSpacing(size)) + " ".repeat(btnCharWidth)
        val btnLine = Component.text().append(Component.text(row4Gap)).append(btnComp)
        val msgLine = if (msgComp != null) Component.text().append(Component.text(msgGap)).append(msgComp) else null
        val emptySpacer = Component.text(otherRowGap)
        val messageRowSpacer = Component.text(messageRowGap)

        var boardRow = 0  // 棋盘数据行索引 (0..14)

        for (lineIndex in layout.indices) {
            val template = layout[lineIndex]

            if (template.isEmpty()) {
                boardComponent.append(Component.newline())
                continue
            }

            if (boardRow >= 15) {
                boardComponent.append(Component.newline())
                continue
            }

            val line = Component.text()
            var col = 0  // 棋盘列索引
            val totalCells = template.count { markerToRegion.containsKey(it.toString()) }

            for (i in template.indices) {
                val ch = template[i]
                val region = markerToRegion[ch.toString()]
                when {
                    // cell marker → 渲染棋盘格
                    region != null -> {
                        val value = game.board[boardRow][col]
                        val isLastMove = boardRow == game.lastMoveRow && col == game.lastMoveCol && value != 0
                        // 检查是否在获胜连线上且已被标记
                        val winLineIndex = if (game.winningLine.isNotEmpty()) {
                            game.winningLine.indexOfFirst { it.first == boardRow && it.second == col }
                        } else -1
                        val isWinMarked = winLineIndex >= 0 && winLineIndex < game.winMarkProgress
                        val raw = GomokuGUIConfig.getStoneStyle(size, region, value, isLastMove, isWinMarked, boardRow, col)
                        line.append(renderCell(raw, useTexture, boardRow, col))
                        // 纹理模式：格子之间插入负空格对齐
                        if (col < totalCells - 1 && useTexture) {
                            line.append(TextParser.parse(
                                GomokuGUIConfig.negativeSpace,
                                GomokuGUIConfig.fontNamespace,
                                true
                            ))
                        }
                        col++
                    }
                    // 空格 → 渲染空白
                    ch == ' ' -> {
                        line.append(Component.space())
                    }
                    // 其他字符 → 装饰文字
                    else -> {
                        line.append(TextParser.parse(ch.toString(), GomokuGUIConfig.fontNamespace, useTexture))
                    }
                }
            }

            // 按钮行放按钮，消息行放游戏结束消息，其余行放等宽空白撑对齐
            when {
                lineIndex == buttonLineIndex -> line.append(btnLine.build())
                game.showGameOverMessage && lineIndex == messageLineIndex && msgLine != null -> line.append(msgLine.build())
                lineIndex == messageLineIndex -> line.append(messageRowSpacer)
                else -> line.append(emptySpacer)
            }

            boardComponent.append(line)
            if (lineIndex < layout.size - 1) {
                boardComponent.append(Component.newline())
            }

            boardRow++
        }

        val closeButton = ActionButton.builder(
            TextParser.parse(" ", null, false)
        ).width(1)
            .action(DialogAction.staticAction(ClickEvent.runCommand("/gomoku close")))
            .build()

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(
                    DialogBase.builder(TextParser.parse(GomokuGUIConfig.dialogTitle, null, false))
                        .pause(false)
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(listOf(DialogBody.plainMessage(boardComponent.build(), GomokuGUIConfig.dialogWidth)))
                        .build()
                )
                .type(DialogType.multiAction(listOf(closeButton), null, 1))
        }

        player.showDialog(dialog)
    }
}
