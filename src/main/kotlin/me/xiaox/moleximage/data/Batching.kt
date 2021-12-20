package me.xiaox.moleximage.data

import net.mamoe.mirai.message.data.Image
import java.io.File

object Batching {

    private val onBatching = HashMap<Long, Pair<String, MutableSet<File>>>()

    operator fun contains(id: Long): Boolean = id in onBatching

    operator fun get(id: Long): Pair<String, MutableSet<File>>? = onBatching[id]

    // TODO 希望在重复命令导致上一个批处理被强制打断时反馈报告
    operator fun plusAssign(pair: Pair<Long, String>) {
        onBatching[pair.first] = pair.second to HashSet()
    }

    suspend operator fun plus(pair: Pair<Long, Image>): File? {
        val id = pair.first
        val image = pair.second

        val batch = onBatching[id] ?: return null
        val keyword = batch.first
        val added = batch.second
        return Gallery.saveTo(keyword, image).also { added += it }
    }

    operator fun minusAssign(id: Long) {
        onBatching.remove(id)
    }

    fun getBatch(): Map<Long, Pair<String, Set<File>>> = onBatching

}