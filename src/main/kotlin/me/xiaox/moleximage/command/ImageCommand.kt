package me.xiaox.moleximage.command

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.data.Aliases
import me.xiaox.moleximage.data.Batching
import me.xiaox.moleximage.data.Gallery
import me.xiaox.moleximage.data.History
import me.xiaox.moleximage.util.*
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.Image
import java.io.File

object ImageCommand : CompositeCommand(
    MolexImage, "moleximage", "mi",
    description = "莫图插件"
) {

    @SubCommand
    @Description("查询支持图片列表")
    suspend fun CommandSender.list() = sendMessage("$PREFIX 支持列表: \n${Gallery.getKeywords()}")

    @SubCommand
    @Description("查看指定图库的信息")
    suspend fun CommandSender.view(raw: String) {
        val keyword = adaptKeyword(raw, this) ?: return

        val images = Gallery.getImages(keyword)
        sendMessage(
            listOf(
                "$PREFIX 图库 $keyword 的信息: ",
                "- 图库大小: ${images.size} 张图片 (${toFileSize(images.sumOf { it.length() })})",
                "- 图库昵称: ${Gallery.getAliases(keyword).let { if (it == null || it.isEmpty()) "无昵称" else it }}"
            ).concat()
        )
    }

    @SubCommand
    @Description("查询指定群员最后一次请求的图片信息")
    suspend fun CommandSender.lastOf(who: Member) = History.report(this, who)

    @SubCommand
    @Description("查询最后一次请求的图片信息")
    suspend fun CommandSender.last() {
        History.report(this, user ?: return run { sendMessage("$PREFIX 无法获取控制台的图片请求记录") })
    }

    @SubCommand
    @Description("")
    suspend fun CommandSender.rebuildAlias() {
        if (notAdmin()) {
            return
        }

        Aliases.indexing()
        sendMessage("$PREFIX 重建图库昵称缓存成功")
    }

    @SubCommand
    @Description("创建一个图库")
    suspend fun CommandSender.create(keyword: String) {
        if (notAdmin()) {
            return
        }

        adaptKeyword(keyword)?.let { exist ->
            val message = if (exist == keyword) "图库 $exist 已存在" else "$keyword 已被作为图库 $exist 的昵称占用"
            sendMessage("$PREFIX $message")
            return
        }
        sendMessage("$PREFIX 图库 $keyword 创建${if (Gallery.createGallery(keyword)) "成功" else "失败"}")
    }

    @SubCommand
    @Description("向指定图库添加图片")
    suspend fun CommandSender.add(raw: String, image: Image) {
        if (notAdmin()) {
            return
        }

        val keyword = adaptKeyword(raw, this) ?: return
        val exist = File(Gallery.getGallery(keyword), image.imageId)
        if (exist.exists()) {
            sendMessage("$PREFIX 该图片 ID 已被占用 \n#${image.imageId}")
            return
        }

        val file = Gallery.saveTo(keyword, image)
        sendMessage("$PREFIX 成功添加到图库 $keyword \n#${file.nameWithoutExtension}")
    }

    @SubCommand
    @Description("向指定图库批量添加图片")
    suspend fun CommandSender.batchAdd(raw: String) {
        if (notAdmin()) {
            return
        }
        val id = user?.id ?: return

        val keyword = adaptKeyword(raw, this) ?: return
        Batching += id to keyword
        sendMessage("$PREFIX 已进入对图库 $keyword 的批量添加模式, 请私聊发送图片, 私聊任意非图片消息后结束批量添加")
    }

    @SubCommand
    @Description("从指定图库中删除指定图片")
    suspend fun CommandSender.delete(raw: String, name: String) {
        if (notAdmin()) {
            return
        }

        val keyword = adaptKeyword(raw, this) ?: return
        val file = Gallery.getImages(keyword).firstOrNull { it.name.startsWith(name) }
            ?: return run { sendMessage("$PREFIX 指定图片 $name 不存在") }
        file.delete()
        sendMessage("$PREFIX 成功从图库 $keyword 中删除图片 $name")
    }

    @SubCommand
    @Description("为指定图库新增昵称")
    suspend fun CommandSender.addAlias(raw: String, alias: String) {
        if (notAdmin()) {
            return
        }

        val keyword = adaptKeyword(raw, this) ?: return
        Aliases[keyword] = alias
        sendMessage("$PREFIX 成功为图库 $keyword 新增昵称 $alias")
    }

    @SubCommand
    @Description("删除指定的图库昵称")
    suspend fun CommandSender.delAlias(alias: String) {
        if (notAdmin()) {
            return
        }

        val keyword = adaptKeyword(alias, this) ?: return
        Aliases -= alias
        sendMessage("$PREFIX 成功删除图库 $keyword 的昵称 $alias")
    }

}