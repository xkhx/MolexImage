package me.xiaox.moleximage.command

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.command.ImageCommand.delete
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.data.Batching
import me.xiaox.moleximage.data.Gallery
import me.xiaox.moleximage.data.GalleryExact
import me.xiaox.moleximage.data.History
import me.xiaox.moleximage.util.*
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.Image
import java.io.File

// TODO 命令需要优化
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
        with(Gallery.getBy(raw, this) ?: return) {
            sendMessage(
                listOf(
                    "$PREFIX 图库 $identity 的信息: ",
                    "- 图库大小: $amount 张图片 (${toFileSize(size)})",
                    "- 图库昵称: ${aliases.ifEmpty { "无昵称" }}",
                    "- 图库前缀: ${Configuration.prefixes.filterValues { identity in it }.keys.ifEmpty { "无特殊前缀" }}"
                ).concat()
            )
        }
    }

    @SubCommand
    @Description("查询指定群员最后一次请求的图片信息")
    suspend fun CommandSender.last(who: User? = user) = who?.let { History.report(this, it) }

    @SubCommand
    @Description("创建一个图库")
    suspend fun CommandSender.create(gallery: String) {
        if (notAdmin()) {
            return
        }

        // TODO 提取
        adaptKeyword(gallery)?.let { exist ->
            val message = if (exist == gallery) "图库 $exist 已存在" else "$gallery 已被作为图库 $exist 的昵称占用"
            sendMessage("$PREFIX $message")
            return
        }
        try {
            GalleryExact(gallery, true)
            sendMessage("$PREFIX 图库 $gallery 创建成功")
        } catch (ex: Throwable) {
            sendMessage("$PREFIX 图库 $gallery 创建失败: ${ex.message}")
        }
    }

    @SubCommand
    @Description("向指定图库添加图片")
    suspend fun CommandSender.add(raw: String, vararg images: Image) {
        if (notAdmin()) {
            return
        }
        with(Gallery.getBy(raw, this) ?: return) {
            images.forEach { image ->
                with(File(folder, image.imageId)) {
                    if (exists()) {
                        sendMessage("$PREFIX 该图片 ID 已被占用: $name")
                        return@forEach
                    }
                }
                DownloadQueue.request(identity, image) {
                    sendMessage(toAddSuccess(identity, it))
                }
            }
            sendMessage("$PREFIX 成功处理 ${images.size} 个图片的批量添加请求")
        }
    }

    @SubCommand
    @Description("向指定图库批量添加图片")
    suspend fun CommandSender.batchAdd(raw: String) {
        if (notAdmin()) {
            return
        }
        with(Gallery.getBy(raw, this) ?: return) {
            Batching.start(user?.id ?: return, identity)
            sendMessage("$PREFIX 已进入对图库 $identity 的批量添加模式, 请私聊发送图片, 私聊任意非图片消息后结束批量添加")
        }
    }

    @SubCommand
    @Description("从指定图库中删除指定图片")
    suspend fun CommandSender.delete(raw: String, name: String) {
        if (notAdmin()) {
            return
        }
        with(Gallery.getBy(raw, this) ?: return) {
            val file = images.firstOrNull {
                it.nameWithoutExtension == name || it.name == name
            } ?: return run { sendMessage("$PREFIX 未找到指定图片: $name") }
            file.delete()
            sendMessage("$PREFIX 成功从图库 $identity 中删除图片 ${file.name}")
        }
    }

    @SubCommand
    @Description("为指定图库新增昵称")
    suspend fun CommandSender.addAlias(raw: String, alias: String) {
        if (notAdmin()) {
            return
        }
        // TODO 所有的 with 都可以提取
        with(Gallery.getBy(raw, this) ?: return) {
            adaptKeyword(alias)?.let { exist ->
                val message = if (exist == alias) "图库 $exist 已存在" else "$alias 已被作为图库 $exist 的昵称占用"
                sendMessage("$PREFIX $message")
                return
            }
            addAlias(alias)
            sendMessage("$PREFIX 成功为图库 $identity 新增昵称 $alias")
        }
    }

    @SubCommand
    @Description("删除指定的图库昵称")
    suspend fun CommandSender.delAlias(alias: String) {
        if (notAdmin()) {
            return
        }

        if (alias in Gallery.getKeywords()) {
            sendMessage("$PREFIX 无法删除图库关键词 $alias")
            return
        }
        with(Gallery.getBy(alias, this) ?: return) {
            delAlias(alias)
            sendMessage("$PREFIX 成功删除图库 $identity 的昵称 $alias")
        }
    }

}