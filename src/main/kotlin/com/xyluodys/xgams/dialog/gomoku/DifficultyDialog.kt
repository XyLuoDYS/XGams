package com.xyluodys.xgams.dialog.gomoku

import com.xyluodys.xgams.Config.GomokuGUIConfig
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

/**
 * 难度选择对话框
 *
 * 三个难度按钮：简单 / 中等 / 困难
 * 点击后执行 /gomoku difficulty <难度> 开始新棋局
 */
object DifficultyDialog {

    fun open(player: Player) {
        val title = TextParser.parse("&6五子棋 — 选择难度", null, false)

        val body = DialogBody.plainMessage(
            Component.text()
                .append(TextParser.parse("\n&f请选择 AI 难度：\n\n", null, false))
                .append(TextParser.parse("&7━━━━━━━━━━━━━━━━━━━\n\n", null, false))
                .append(TextParser.parse("&a简单 &7— AI 棋力较弱，适合新手\n", null, false))
                .append(TextParser.parse("&e中等 &7— AI 具备一定战术，适合进阶\n", null, false))
                .append(TextParser.parse("&c困难 &7— AI 深度搜索，全力以赴\n\n", null, false))
                .append(TextParser.parse("&7━━━━━━━━━━━━━━━━━━━", null, false))
                .build(),
            400
        )

        val easyButton = DialogBody.plainMessage(
            TextParser.parse("&a[简单]", null, false)
                .clickEvent(ClickEvent.runCommand("/gomoku difficulty easy"))
        )

        val mediumButton = DialogBody.plainMessage(
            TextParser.parse("&e[中等]", null, false)
                .clickEvent(ClickEvent.runCommand("/gomoku difficulty medium"))
        )

        val hardButton = DialogBody.plainMessage(
            TextParser.parse("&c[困难]", null, false)
                .clickEvent(ClickEvent.runCommand("/gomoku difficulty hard"))
        )

        val closeButton = ActionButton.builder(
            TextParser.parse("&7关闭", null, false)
        ).width(40)
            .action(DialogAction.staticAction(ClickEvent.runCommand("/gomoku close")))
            .build()

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(
                    DialogBase.builder(title)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(listOf(body, easyButton, mediumButton, hardButton))
                        .build()
                )
                .type(DialogType.multiAction(listOf(closeButton), null, 1))
        }

        player.showDialog(dialog)
    }
}
