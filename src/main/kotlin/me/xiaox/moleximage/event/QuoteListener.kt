package me.xiaox.moleximage.event

import me.xiaox.moleximage.util.*
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*

object QuoteListener : ListenerHost {

    @EventHandler
    suspend fun MessageEvent.onQuoted() {
        val quote = message[QuoteReply] ?: return
        val content = message.content

        suspend fun original(): MessageChain? {
            val commander = sender.asCommandSender(false)
            if (commander.notAdmin()) {
                return null
            }
            return quote.source.ids.elementAtOrNull(0)?.let { MessageCache[it] } ?: return run {
                subject.sendMessage("$PREFIX 源消息缓存已失效")
                null
            }
        }

        when {
            with(content.lowercase()) { "recall" in this || "快撤回" in this } -> {
                with(original() ?: return) {
                    if (size == 1 && contains(Image)) {
                        quote.recallSource()
                    }
                }
            }
            content.startsWith('#') -> {
                val gallery = adaptKeyword(content.substringAfter('#')) ?: return run {
                    subject.sendMessage("$PREFIX 指定图库不存在")
                }
                with(original() ?: return) {
                    filterIsInstance<Image>().apply {
                        forEach { image ->
                            DownloadQueue.request(gallery, image) {
                                subject.sendMessage(toAddSuccess(gallery, it))
                            }
                        }
                        subject.sendMessage("$PREFIX 成功提交共 $size 个图片添加请求")
                    }
                }
            }
        }
    }

}