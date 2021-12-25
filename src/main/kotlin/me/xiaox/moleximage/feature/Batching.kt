package me.xiaox.moleximage.feature

import me.xiaox.moleximage.queue.impl.AddManager
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.Image

object Batching {

    private val batch = HashMap<Long, Pair<String, MutableSet<Image>>>()

    operator fun contains(id: Long): Boolean = id in batch

    operator fun contains(user: User): Boolean = user.id in batch

    fun start(id: Long, gallery: String) {
        if (id in this) {
            return
        }
        batch[id] = gallery to HashSet()
    }

    suspend fun end(user: User) {
        batch.remove(user.id)?.let { (gallery, images) ->
            AddManager.request(listOf(gallery), images) {
                user.sendMessage(toResult(it))
            }.also { user.sendMessage(it.toRequest()) }
        }
    }

    fun batch(user: User, images: Collection<Image>) = batch[user.id]?.let { (_, exist) -> exist += images }

}