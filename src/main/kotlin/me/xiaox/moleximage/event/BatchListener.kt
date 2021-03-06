package me.xiaox.moleximage.event

import me.xiaox.moleximage.feature.Batching
import me.xiaox.moleximage.util.PREFIX
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.UserMessageEvent
import net.mamoe.mirai.message.data.Image

object BatchListener : ListenerHost {

    @EventHandler
    suspend fun UserMessageEvent.onBatching() {
        if (sender !in Batching) {
            return
        }

        message.filterIsInstance<Image>().apply {
            if (isEmpty()) {
                return Batching.end(sender)
            }
            Batching.batch(sender, this)
            sender.sendMessage("$PREFIX 成功提交共 $size 个图片的添加请求")
        }
    }

}