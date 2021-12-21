package me.xiaox.moleximage.util

import com.google.common.cache.CacheBuilder
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.ids
import net.mamoe.mirai.message.sourceIds
import net.mamoe.mirai.utils.info
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanBePrivate")
object MessageCache : ListenerHost {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .maximumSize(100)
        .build<Int, MessageChain>()

    operator fun get(id: Int): MessageChain? = cache.getIfPresent(id)

    @EventHandler
    fun MessageEvent.onReceive() {
        message.ids.elementAtOrNull(0)?.let {
            cache.put(it, message)
        }
    }

    @EventHandler
    fun MessagePostSendEvent<out Contact>.onSend() {
        receipt?.let { receipt ->
            receipt.sourceIds.elementAtOrNull(0)?.let {
                cache.put(it, message)
            }
        }
    }

}