package me.xiaox.moleximage.queue.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.queue.QueueManager
import me.xiaox.moleximage.queue.QueueRequest
import me.xiaox.moleximage.queue.impl.DownloadManager.DownloadRequest
import java.io.File
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

object DownloadManager : QueueManager<File, DownloadRequest>() {

    override val frequency: Duration
        get() = Configuration.downloadFrequency.seconds

    override suspend fun DownloadRequest.handle(): File {
        withContext(Dispatchers.IO) { URL(url).openConnection() }.apply {
            addRequestProperty(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
            )

            with(withContext(Dispatchers.IO) { getInputStream() }) {
                if (contentEncoding == "gzip") GZIPInputStream(this) else this
            }.readBytes().let { to.writeBytes(it) }
        }
        return to
    }

    fun request(url: String, to: File, onComplete: suspend DownloadRequest.(Result<File>) -> Unit): DownloadRequest {
        return DownloadRequest(url, to, onComplete).also { DownloadManager += it }
    }

    data class DownloadRequest(
        val url: String,
        val to: File,
        val callback: suspend DownloadRequest.(Result<File>) -> Unit
    ) : QueueRequest<File>() {
        override val index = DownloadManager.index()

        override suspend fun onComplete(result: Result<File>) = callback(this, result)
    }

}