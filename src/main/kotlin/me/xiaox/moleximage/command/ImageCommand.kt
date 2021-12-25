package me.xiaox.moleximage.command

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.config.Locale
import me.xiaox.moleximage.feature.Batching
import me.xiaox.moleximage.feature.Gallery
import me.xiaox.moleximage.data.GalleryExact
import me.xiaox.moleximage.feature.History
import me.xiaox.moleximage.util.*
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.contact.User

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
        withGallery(raw) {
            sendMessage(
                listOf(
                    "$PREFIX 图库 $identity 的信息: ",
                    "- 图库大小: $amount 张图片 (${Locale.toFilesize(size)})",
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
        if (!permitted()) {
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
    @Description("向指定图库批量添加图片")
    suspend fun UserCommandSender.batchAdd(raw: String) {
        if (!permitted()) {
            return
        }
        withGallery(raw) {
            Batching.start(user.id, identity)
            sendMessage("$PREFIX 已进入对图库 $identity 的批量添加模式, 请私聊发送图片, 私聊任意非图片消息后结束批量添加")
        }
    }

    @SubCommand
    @Description("从指定图库中删除指定图片")
    suspend fun CommandSender.delete(raw: String, name: String) {
        if (!permitted()) {
            return
        }
        withGallery(raw) {
            val file = images.firstOrNull {
                it.nameWithoutExtension == name || it.name == name
            } ?: return@withGallery run { sendMessage("$PREFIX 未找到指定图片: $name") }
            file.delete()
            sendMessage("$PREFIX 成功从图库 $identity 中删除图片 ${file.name}")
        }
    }

    @SubCommand
    @Description("为指定图库新增昵称")
    suspend fun CommandSender.addAlias(raw: String, alias: String) {
        if (!permitted()) {
            return
        }
        withGallery(raw) {
            adaptKeyword(alias)?.let { exist ->
                val message = if (exist == alias) "图库 $exist 已存在" else "$alias 已被作为图库 $exist 的昵称占用"
                sendMessage("$PREFIX $message")
                return@withGallery
            }
            addAlias(alias)
            sendMessage("$PREFIX 成功为图库 $identity 新增昵称 $alias")
        }
    }

    @SubCommand
    @Description("删除指定的图库昵称")
    suspend fun CommandSender.delAlias(alias: String) {
        if (!permitted()) {
            return
        }

        if (alias in Gallery.getKeywords()) {
            sendMessage("$PREFIX 无法删除图库关键词 $alias")
            return
        }
        withGallery(alias) {
            delAlias(alias)
            sendMessage("$PREFIX 成功删除图库 $identity 的昵称 $alias")
        }
    }

    @SubCommand
    @Description("删除指定的图库中的无效图片文件")
    suspend fun CommandSender.deleteInvalid(raw: String) {
        if (!permitted()) {
            return
        }
        withGallery(raw) {
            val invalid = folder.walk()
                .maxDepth(1)
                .filter { it.isFile && it.extension.lowercase() !in Configuration.supports }
                .toSet()
            invalid.forEach { it.delete() }
            sendMessage("$PREFIX 共删除 ${invalid.size} 个文件")
        }
    }

}