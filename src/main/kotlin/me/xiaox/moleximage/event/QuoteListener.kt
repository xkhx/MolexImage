package me.xiaox.moleximage.event

import me.xiaox.moleximage.config.Locale
import me.xiaox.moleximage.queue.impl.AddManager
import me.xiaox.moleximage.util.MessageCache
import me.xiaox.moleximage.util.PREFIX
import me.xiaox.moleximage.util.filterTags
import me.xiaox.moleximage.util.permitted
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.content

object QuoteListener : ListenerHost {

    @EventHandler
    suspend fun MessageEvent.onQuoted() {
        val quote = message[QuoteReply] ?: return
        val id = quote.source.ids.elementAtOrNull(0) ?: return run {
            subject.sendMessage("$PREFIX 无法获取到源消息 ID")
        }
        val content = message.content

        suspend fun original(): MessageChain? {
            return MessageCache[id] ?: return run {
                subject.sendMessage("$PREFIX 源消息缓存已失效")
                null
            }
        }

        when {
            "快撤回" in content || content.contains("recall", true) -> {
                if (!sender.permitted()) {
                    return
                }
                with(original() ?: return) {
                    if (size >= 1 || !contains(Image)) {
                        return
                    }
                    MessageCache.getReceipt(id)?.recall() ?: return run {
                        subject.sendMessage("$PREFIX 源消息回执缓存已失效")
                    }
                }
            }
            '#' in content -> {
                filterTags(content).let { (galleries, notFound) ->
                    if (galleries.isEmpty() || !sender.permitted()) {
                        return
                    }
                    with(original() ?: return) {
                        val images = filterIsInstance<Image>()
                        if (images.isEmpty()) {
                            subject.sendMessage("$PREFIX 源消息不包含图片")
                            return
                        }

                        if (notFound.isNotEmpty()) {
                            subject.sendMessage(Locale.toGalleryNotFound(notFound))
                        }
                        AddManager.request(galleries, images) {
                            subject.sendMessage(toResult(it))
                        }.also { subject.sendMessage(it.toRequest()) }
                    }
                }
            }
        }
    }

}