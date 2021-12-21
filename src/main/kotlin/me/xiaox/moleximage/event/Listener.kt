package me.xiaox.moleximage.event

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.data.Batching
import me.xiaox.moleximage.data.Gallery
import me.xiaox.moleximage.data.History
import me.xiaox.moleximage.util.*
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.*

@Suppress("MemberVisibilityCanBePrivate")
object Listener {

    var initialized = false
        private set

    fun init() {
        if (initialized) {
            return
        }

        onBatchAdd()
        onRequest()
        onQuoteAction()
        initialized = true
    }

    private fun onRequest() {
        suspend fun handle(event: MessageEvent) {
            val message = event.message.content
            var pool = Gallery.getKeywords()
            val prefix = Configuration.triggers.firstOrNull { message.startsWith(it) }
                ?: Configuration.prefixes.let { prefixes ->
                    prefixes.keys.firstOrNull { message.startsWith(it) }?.also {
                        pool = prefixes[it] ?: HashSet()
                    } ?: return
                }
            if (pool.isEmpty()) {
                return
            }

            val gallery = message.substringAfter(prefix, "").trim().let { raw ->
                if (raw.isEmpty()) {
                    return
                }
                adaptKeyword(raw)?.also { if (it !in pool) return }
            } ?: return

            with(event.subject) {
                val images = (Gallery[gallery] ?: return).images
                if (images.isEmpty()) {
                    sendMessage("$PREFIX 什么也没有找到哦~")
                    return
                }
                with(images.random()) {
                    sendImage(this)
                    History.record(event.sender, gallery, this)
                }
            }
        }
        MolexImage.globalEventChannel().subscribeAlways<GroupMessageEvent> { handle(it) }
        MolexImage.globalEventChannel().subscribeAlways<GroupTempMessageEvent> { handle(it) }
        MolexImage.globalEventChannel().subscribeAlways<FriendMessageEvent> { handle(it) }
        MolexImage.globalEventChannel().subscribeAlways<StrangerMessageEvent> { handle(it) }
    }

    private fun onBatchAdd() {
        suspend fun onReceive(from: User, message: MessageChain) {
            val images = message.filterIsInstance<Image>()
            if (images.isEmpty()) {
                Batching.end(from)
                return
            }

            images.forEach { Batching.batch(from, it) }
            with(images.size) {
                if (this > 1) {
                    from.sendMessage("$PREFIX 成功处理 $this 个图片的批量添加请求")
                }
            }
        }
        MolexImage.globalEventChannel().subscribeAlways<GroupTempMessageEvent> { onReceive(it.sender, it.message) }
        MolexImage.globalEventChannel().subscribeAlways<FriendMessageEvent> { onReceive(it.sender, it.message) }
        MolexImage.globalEventChannel().subscribeAlways<StrangerMessageEvent> { onReceive(it.sender, it.message) }
    }

    private fun onQuoteAction() {
        suspend fun onQuoted(event: MessageEvent) {
            val message = event.message
            val quote = message[QuoteReply.Key] ?: return

            val content = message.content
            when {
                with(content.lowercase()) { this == "recall" || this == "快撤回" } -> {
                    if (event.sender.asCommandSender(false).notAdmin()) {
                        return
                    }

                    with(quote.source.originalMessage) {
                        LOGGER.info("收到快撤回命令, ${toString()}")
                        forEach {
                            LOGGER.info("- $it")
                        }

                        if (size == 1 && contains(Image.Key)) {
                            quote.recallSource()
                        }
                    }
                }
                content.startsWith('#') -> {
                    if (event.sender.asCommandSender(false).notAdmin()) {
                        return
                    }

                    val gallery = adaptKeyword(content.substringAfter('#')) ?: return run {
                        event.subject.sendMessage("$PREFIX 指定图库不存在")
                    }
                    with(quote.source.originalMessage) {
                        LOGGER.info("收到快速添加命令, ${toString()}")
                        forEach {
                            LOGGER.info("- $it")
                        }

                        val images = filterIsInstance<Image>()
                        images.forEach { image ->
                            DownloadQueue.request(gallery, image) {
                                event.subject.sendMessage(toAddSuccess(gallery, it))
                            }
                        }
                        event.subject.sendMessage("$PREFIX 成功处理 ${images.size} 个图片的批量添加请求")
                    }
                }
            }
        }
        MolexImage.globalEventChannel().subscribeAlways<GroupMessageEvent> { onQuoted(it) }
        MolexImage.globalEventChannel().subscribeAlways<GroupTempMessageEvent> { onQuoted(it) }
        MolexImage.globalEventChannel().subscribeAlways<FriendMessageEvent> { onQuoted(it) }
        MolexImage.globalEventChannel().subscribeAlways<StrangerMessageEvent> { onQuoted(it) }
    }

}