package me.xiaox.moleximage.util

import com.google.common.cache.CacheBuilder
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.ListenerHost
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.ids
import net.mamoe.mirai.message.sourceIds
import java.util.concurrent.TimeUnit

@Suppress("MemberVisibilityCanBePrivate")
object MessageCache : ListenerHost {

    private val messageCache = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(200)
        .build<Int, MessageChain>()
    private val receiptCache = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(100)
        .build<Int, MessageReceipt<Contact>>()


    operator fun get(id: Int): MessageChain? = messageCache.getIfPresent(id)

    fun getReceipt(id: Int): MessageReceipt<Contact>? = receiptCache.getIfPresent(id)

    @EventHandler
    fun MessageEvent.onReceive() {
        messageCache.put(message.ids.elementAtOrNull(0) ?: return, message)
    }

    @EventHandler
    fun MessagePostSendEvent<out Contact>.onSend() {
        with(receipt ?: return) {
            val id = sourceIds.elementAtOrNull(0) ?: return
            messageCache.put(id, message)
            receiptCache.put(id, this)
        }
    }

}