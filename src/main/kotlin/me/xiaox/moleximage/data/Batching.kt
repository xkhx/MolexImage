package me.xiaox.moleximage.data

import me.xiaox.moleximage.util.DownloadQueue
import me.xiaox.moleximage.util.PREFIX
import me.xiaox.moleximage.util.toAddSuccess
import me.xiaox.moleximage.util.toFileSize
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.Image
import java.io.File

object Batching {

    private val batch = HashMap<Long, Pair<String, MutableSet<File>>>()

    operator fun get(id: Long): Pair<String, MutableSet<File>>? = batch[id]

    operator fun contains(id: Long): Boolean = id in batch

    operator fun contains(user: User): Boolean = user.id in batch

    fun start(id: Long, gallery: String) {
        if (id in this) {
            return
        }
        batch[id] = gallery to HashSet()
    }

    suspend fun end(user: User) {
        batch.remove(user.id)?.let { (gallery, added) ->
            val size = added.sumOf { it.length() }
            val result =
                if (added.isEmpty()) "没有新增任何图片" else "新增 ${added.size} 个图片 (${toFileSize(size)})"
            user.sendMessage("$PREFIX 您对图库 $gallery 的批量添加已结束: $result")
        }
    }

    suspend fun batch(user: User, image: Image) {
        this[user.id]?.let { (gallery, added) ->
            DownloadQueue.request(gallery, image) {
                added += it
                user.sendMessage(toAddSuccess(gallery, it))
            }
        }
    }

}