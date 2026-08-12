package com.xyluodys.xgams.Gomoku

import com.xyluodys.xgams.Config.MessageManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import com.xyluodys.xgams.dialog.gomoku.DialogRegistry.openGomokuDialog

class GomokuStartCommand {

    fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(MessageManager.playerOnly)
            return true
        }
        val game = GomokuPlaceCommand.gameMap.getOrPut(sender.uniqueId) { GomokuGame() }
        openGomokuDialog(sender, game)
        return true
    }
}