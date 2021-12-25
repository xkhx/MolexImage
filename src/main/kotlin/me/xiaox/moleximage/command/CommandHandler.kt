package me.xiaox.moleximage.command

import me.xiaox.moleximage.MolexImage
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.message.data.MessageChain

object CommandHandler : RawCommand(
    MolexImage, "moleximage", "mi",
    description = "莫图插件"
) {

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        TODO("Not yet implemented")
    }

    abstract class SubCommand(val keyword: String, vararg val alias: String) {
        abstract fun CommandSender.onCommand(args: MessageChain)
    }

}