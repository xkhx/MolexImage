package me.xiaox.moleximage.event

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.data.Batching
import me.xiaox.moleximage.data.Gallery
import me.xiaox.moleximage.data.History
import me.xiaox.moleximage.util.PREFIX
import me.xiaox.moleximage.util.adaptKeyword
import me.xiaox.moleximage.util.toFileSize
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.orNull

object Listener {

    var initialized = false
        private set

    fun init() {
        if (initialized) {
            return
        }

        onBatchAdd()
        onRequest()
        initialized = true
    }

    private fun onRequest() {
        suspend fun handle(event: MessageEvent) {
            val message = event.message.content
            // FIXME 支持自定义多关键词配置
            val keyword = message.substringAfter(
                "来张",
                message.substringAfter(
                    "来个",
                    message.substringAfter(
                        "来点",
                        ""
                    )
                )
            ).trim()
                .let {
                    if (it.isEmpty()) {
                        return
                    }
                    adaptKeyword(it)
                } ?: return

            val images = Gallery.getImages(keyword)
            if (images.isEmpty()) {
                event.subject.sendMessage("$PREFIX 什么也没有找到哦~")
                return
            }

            val image = images.random()
            event.subject.sendImage(image)
            History[event.sender.id] = keyword to image
        }
        MolexImage.globalEventChannel().subscribeAlways<GroupMessageEvent> { handle(it) }
        MolexImage.globalEventChannel().subscribeAlways<GroupTempMessageEvent> { handle(it) }
        MolexImage.globalEventChannel().subscribeAlways<FriendMessageEvent> { handle(it) }
        MolexImage.globalEventChannel().subscribeAlways<StrangerMessageEvent> { handle(it) }
    }

    private fun onBatchAdd() {
        suspend fun onReceive(from: User?, message: MessageChain) {
            from ?: return
            val batch = Batching[from.id] ?: return
            val keyword = batch.first

            val image: Image? by message.orNull()
            if (image == null) {
                val added = batch.second
                val result = if (added.isEmpty()) {
                    "没有新增任何图片"
                } else {
                    "新增 ${added.size} 个图片 (${toFileSize(added.sumOf { it.length() })})"
                }
                from.sendMessage("$PREFIX 您对图库 $keyword 的批量添加已结束: $result")
                Batching -= from.id
                return
            }

            val file = Batching + (from.id to image!!)
            if (file == null) {
                from.sendMessage("$PREFIX 添加至图库 $keyword 失败")
                return
            }
            from.sendMessage("$PREFIX 成功添加到图库 $keyword \n#${file.name}")
        }
        MolexImage.globalEventChannel().subscribeAlways<GroupTempMessageEvent> { onReceive(it.sender, it.message) }
        MolexImage.globalEventChannel().subscribeAlways<FriendMessageEvent> { onReceive(it.sender, it.message) }
        MolexImage.globalEventChannel().subscribeAlways<StrangerMessageEvent> { onReceive(it.sender, it.message) }
    }

}