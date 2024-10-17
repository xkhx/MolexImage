package me.xiaox.moleximage.queue.impl

import kotlinx.coroutines.delay
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.config.Locale
import me.xiaox.moleximage.feature.Gallery
import me.xiaox.moleximage.queue.QueueManager
import me.xiaox.moleximage.queue.QueueRequest
import me.xiaox.moleximage.queue.impl.AddManager.AddReceipt
import me.xiaox.moleximage.queue.impl.AddManager.AddRequest
import me.xiaox.moleximage.util.PREFIX
import me.xiaox.moleximage.util.concat
import me.xiaox.moleximage.util.downloadTo
import me.xiaox.moleximage.util.snapshotAmount
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.collections.HashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object AddManager : QueueManager<Collection<AddReceipt>, AddRequest>() {

    override val frequency: Duration
        get() = Configuration.addFrequency.seconds

    override suspend fun AddRequest.handle(): Collection<AddReceipt> {
        handled = System.currentTimeMillis()
        val result = HashMap<String, AddReceipt>() // Filename to Receipt

        val downloaded = HashMap<String, File>() // URL to File
        galleries.forEach saver@{ identity ->
            val gallery = Gallery[identity] ?: return@saver
            sources.forEach downloader@{ (url, filename) ->
                var finalFilename: String = filename
                val target = gallery[finalFilename]

                downloaded[url]?.let { exist ->
                    result.compute(finalFilename) { _, receipt ->
                        (receipt ?: AddReceipt(
                            finalFilename,
                            Result.success(exist.copyTo(target, overwrite = true)),
                            "从图库 ${exist.parentFile.name} 中复制"
                        )).also { it.galleries[identity] = gallery.amount + 1 }
                    }
                    return@downloader
                }

                runCatching { URL(url).downloadTo(target) }
                    .onFailure { it.printStackTrace() }
                    .onSuccess { (file, _) ->
                        downloaded[url] = file
                        sources[url] = file.name
                        finalFilename = file.name
                    }
                    .also { raw ->
                        var comment = ""
                        val fileResult = raw.map { (file, rawComment) ->
                            comment = rawComment
                            file
                        }
                        result.compute(finalFilename) { _, receipt ->
                            (receipt ?: AddReceipt(finalFilename, fileResult, comment))
                                .also { it.galleries[identity] = gallery.amount }
                        }
                    }
                delay(Configuration.downloadFrequency.seconds)
            }
        }
        completed = System.currentTimeMillis()
        return result.values
    }

    suspend fun request(
        galleries: Collection<String>,
        images: Collection<Image>,
        onComplete: suspend AddRequest.(Result<Collection<AddReceipt>>) -> Unit
    ): AddRequest {
        return AddRequest(
            galleries,
            ConcurrentHashMap<String, String>().apply {
                putAll(images
                    .associateBy { it.queryUrl() }
                    .mapValues { (_, image) ->
                        val uniqueId = UUID.randomUUID().toString()
                        val extension = File(image.imageId).extension
                        return@mapValues "${uniqueId}.${extension}"
                    })
            },
            onComplete
        ).also { AddManager += it }
    }

    data class AddRequest(
        val galleries: Collection<String>,
        val sources: ConcurrentMap<String, String>, // URL to Filename
        val callback: suspend AddRequest.(Result<Collection<AddReceipt>>) -> Unit
    ) : QueueRequest<Collection<AddReceipt>>() {
        override val index = AddManager.index()

        private val snapshotBefore = galleries.snapshotAmount()
        private val createTime: Long = System.currentTimeMillis()
        var handled: Long? = null
        var completed: Long? = null

        override suspend fun onComplete(result: Result<Collection<AddReceipt>>) = callback(this, result)

        fun toResult(result: Result<Collection<AddReceipt>>): String {
            with(result) {
                if (isFailure) {
                    return "$PREFIX 处理添加请求(#$index)时遇到错误: ${exceptionOrNull()}"
                }

                val snapshotAfter = galleries.snapshotAmount()
                return mutableListOf("$PREFIX 添加请求 #$index 已处理完毕:").apply {
                    add("时间信息:")
                    add("- 创建于 ${dateFormat.format(createTime)}")
                    add("- 受理于 ${runCatching { dateFormat.format(handled!!) }.getOrElse { "[无法获取受理时间]" }}")
                    add("- 完成于 ${runCatching { dateFormat.format(completed!!) }.getOrElse { "[无法获取完成时间]" }}")
                    add(runCatching { "- 耗时 ${completed!! - handled!!}ms" }.getOrElse { "[无法获取耗时]" })
                    add("图库变化:")
                    snapshotAfter.toSortedMap().forEach { (identity, after) ->
                        val before = snapshotBefore[identity]
                        val delta = after - (before ?: 0)
                        add("- $identity: ${before ?: "-"} -> $after (${"%+d".format(delta)})")
                    }
                    add("文件列表:")
                    with(getOrNull() ?: return "$PREFIX 添加请求(#$index)的处理结果为 null") {
                        forEachIndexed { index, receipt -> add("${index + 1}. ${receipt.format()}") }
                        add("共 ${Locale.toFilesize(mapNotNull { it.result.getOrNull() })}")
                    }
                }.concat()
            }
        }

        fun toRequest(): String {
            return "$PREFIX 创建添加请求 #$index 成功: ${sources.size} 个图片 -> 图库 ${galleries.joinToString(", ")}"
        }

        companion object {
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class AddReceipt(val filename: String, val result: Result<File>, val comment: String = "") {
        val galleries = HashMap<String, Int>()

        fun format(): String {
            return with(result) {
                if (isFailure) {
                    "[下载失败] $filename (${result.exceptionOrNull()}) $comment"
                } else {
                    val size = getOrNull()?.let { Locale.toFilesize(it) } ?: "无法获取文件对象"
                    val indexes = galleries.toSortedMap().values.joinToString("|") { "#$it" }
                    "[$indexes] $filename ($size) $comment"
                }
            }
        }
    }

}