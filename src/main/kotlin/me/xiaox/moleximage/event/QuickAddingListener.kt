package me.xiaox.moleximage.event

import me.xiaox.moleximage.config.Locale
import me.xiaox.moleximage.queue.impl.AddManager
import me.xiaox.moleximage.util.filterTags
import me.xiaox.moleximage.util.permitted
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.toMessageChain

object QuickAddingListener : ListenerHost {

    @EventHandler
    suspend fun MessageEvent.onQuickAdding() {
        val images = message.filterIsInstance<Image>()
        if (images.isEmpty()) {
            return
        }

        val rawText = message.filterIsInstance<PlainText>().toMessageChain().content.replace('\n', ' ')
        filterTags(rawText).let { (galleries, notFound) ->
            if (galleries.isEmpty() || !sender.permitted()) {
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