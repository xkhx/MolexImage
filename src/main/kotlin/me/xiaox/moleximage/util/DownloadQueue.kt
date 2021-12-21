package me.xiaox.moleximage.util

import kotlinx.coroutines.*
import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.data.Gallery
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@Suppress("MemberVisibilityCanBePrivate")
@OptIn(ExperimentalTime::class)
object DownloadQueue {

    private val frequency: Duration
        get() = Duration.seconds(Configuration.queueFrequency)

    private val queue = ConcurrentLinkedQueue<DownloadRequest>()
    private val job: Job = MolexImage.launch worker@{
        LOGGER.info("下载请求处理速度: ${frequency.inWholeSeconds}s/个")
        while (true) {
            queue.poll()?.handle()
            delay(frequency)
        }
    }

    val size: Int
        get() = queue.size

    operator fun plusAssign(request: DownloadRequest) {
        if (queue.any { it.uniqueId == request.uniqueId }) {
            LOGGER.warning { "尝试向队列添加重复 UUID 的下载请求: $request" }
            return
        }
        queue.offer(request)
        LOGGER.info("新增下载请求: $request")
    }

    fun request(
        gallery: String,
        url: String,
        filename: String,
        callback: suspend (File) -> Unit = {}
    ): DownloadRequest = DownloadRequest(gallery, url, filename, callback).also { DownloadQueue += it }

    suspend fun request(gallery: String, image: Image, callback: suspend (File) -> Unit = {}): DownloadRequest =
        request(gallery, image.queryUrl(), image.imageId, callback)

    data class DownloadRequest(
        val gallery: String,
        val url: String,
        val filename: String,
        val callback: suspend (File) -> Unit
    ) {
        val uniqueId: UUID = UUID.randomUUID()
        var completed: Boolean = false
            set(value) {
                if (cancelled || field) {
                    return
                }
                field = value
                if (field) {
                    LOGGER.info { "下载请求 $uniqueId 已完成" }
                }
            }
        var cancelled: Boolean = false
            set(value) {
                if (completed || field) {
                    return
                }
                field = value
                if (field) {
                    LOGGER.info { "下载请求 $uniqueId 已被取消" }
                }
            }

        fun cancel(): Boolean =
            completed || cancelled || queue.removeIf { it.uniqueId == uniqueId }.also { cancelled = it }

        suspend fun handle() {
            LOGGER.info("处理下载请求: $uniqueId")
            val gallery = Gallery[gallery] ?: return run {
                LOGGER.warning { "下载请求 $uniqueId 指向了一个不存在的图库 $gallery" }
            }

            val connection = withContext(Dispatchers.IO) { URL(url).openConnection() }.apply {
                addRequestProperty(
                    "user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
                )
            }
            val bytes = with(withContext(Dispatchers.IO) { connection.getInputStream() }) {
                if (connection.contentEncoding == "gzip") GZIPInputStream(this) else this
            }.readBytes()

            callback(File(gallery.folder, filename).apply { writeBytes(bytes) })
            completed = true
        }

        override fun toString(): String = "$filename@$url -> $gallery($uniqueId)"
    }

}