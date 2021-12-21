package me.xiaox.moleximage.data

import me.xiaox.moleximage.util.PREFIX
import me.xiaox.moleximage.util.concat
import me.xiaox.moleximage.util.toFileSize
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.data.PlainText
import java.io.File

object History {

    private val history = HashMap<Long, Pair<String, File>>()

    operator fun get(id: Long): Pair<String, File>? = history[id]

    fun record(user: User, gallery: String, file: File) {
        this.history[user.id] = gallery to file
    }

    suspend fun report(to: CommandSender, who: User) {
        val target = if (to.user?.id == who.id) "您" else "群员 ${who.nameCardOrNick} "
        val history = History[who.id] ?: return run { to.sendMessage("$PREFIX ${target}没有最近一次请求记录") }
        history.let { (gallery, image) ->
            to.sendMessage(
                PlainText(
                    listOf(
                        "$PREFIX ${target}最近一次请求的图片:",
                        "- 来自图库: $gallery",
                        "- 图片文件: ${image.name} (${toFileSize(image.length())})",
                        ""
                    ).concat()
                ) + who.uploadImage(image)
            )
        }
    }

}