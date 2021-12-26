package me.xiaox.moleximage.event

import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.feature.Gallery
import me.xiaox.moleximage.feature.History
import me.xiaox.moleximage.util.PREFIX
import me.xiaox.moleximage.util.adaptKeyword
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content

object RequestListener : ListenerHost {

    @EventHandler
    suspend fun MessageEvent.onRequest() {
        val content = message.content
        var pool = Gallery.getKeywords()
        val prefix = Configuration.triggers.firstOrNull { content.startsWith(it) }
            ?: Configuration.prefixes.let { prefixes ->
                prefixes.keys.firstOrNull { content.startsWith(it) }?.also {
                    pool = prefixes[it] ?: HashSet()
                } ?: return
            }
        if (pool.isEmpty()) {
            return
        }

        val gallery = content.substringAfter(prefix, "").trim().let { raw ->
            if (raw.isEmpty()) {
                return
            }
            adaptKeyword(raw)?.also { if (it !in pool) return }
        }?.let { Gallery[it] } ?: return

        with(subject) {
            gallery.images.run {
                if (isEmpty()) {
                    sendMessage("$PREFIX 什么也没有找到哦~")
                    return
                }
                with(random()) {
                    runCatching {
                        sendImage(this)
                    }.onFailure {
                        it.printStackTrace()
                        sendMessage("$PREFIX 发送图片($name)时遇到错误: ${it.message}")
                    }
                    History.record(sender, gallery.identity, this)
                }
            }
        }
    }

    @EventHandler
    suspend fun MessageEvent.onContinue() {
        if (message.content != "再来一张") {
            return
        }
        val history = History[sender.id] ?: return
        val gallery = Gallery[history.first] ?: return

        with(gallery.images.randomOrNull() ?: return) {
            runCatching {
                subject.sendImage(this)
            }.onFailure {
                it.printStackTrace()
                subject.sendMessage("$PREFIX 发送图片($name)时遇到错误: ${it.message}")
            }
            History.record(sender, gallery.identity, this)
        }
    }

}