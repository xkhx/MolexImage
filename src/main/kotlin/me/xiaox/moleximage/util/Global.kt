package me.xiaox.moleximage.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.config.Configuration
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import org.apache.tika.metadata.HttpHeaders
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.imageio.ImageIO

const val PREFIX = "MolexImage >"

val LOGGER: MiraiLogger
    get() = MolexImage.logger

fun Collection<String>.concat(): String = joinToString("\n")

suspend fun CommandSender.permitted(silence: Boolean = false): Boolean =
    hasPermission(MolexImage.PERMISSION_ADMIN).also {
        if (silence || it) {
            return@also
        }
        sendMessage("$PREFIX 你没有这个命令的权限")
    }

suspend fun User.permitted(silence: Boolean = false): Boolean = asCommandSender(false).permitted(silence)

suspend fun URL.downloadTo(to: File): Pair<File, String> {
    var corrected: File = to
    var comment: String = ""
    withContext(Dispatchers.IO) { openConnection() }.apply {
        addRequestProperty(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
        )

        with(withContext(Dispatchers.IO) { getInputStream() }) {
            if (contentEncoding == "gzip") GZIPInputStream(this) else this
        }.use { stream ->
            val bytes = stream.readBytes()
            val extension = to.extension.lowercase()
            if (extension !in Configuration.supports) {
                with(bytes.toInputStream().getMimeType().substringAfter("image/", "")) {
                    if (isEmpty() || extension == this) {
                        return@with
                    }
                    corrected = File(to.parentFile, "${to.nameWithoutExtension}.$this")

                    val image = ImageIO.read(bytes.toInputStream())
                    ImageIO.write(image, this, corrected)
                    LOGGER.info { "自动格式转换: ${to.name} -> .$this" }
                    comment = "[自动格式转换: .$extension -> .$this]"
                    return@use
                }
                error("不受支持的文件类型: .$extension")
            }
            corrected.writeBytes(bytes)
        }
    }
    return corrected to comment
}

fun ByteArray.toInputStream(): InputStream {
    return ByteArrayInputStream(this)
}

fun InputStream.getMimeType(): String {
    return Metadata().also {
        with(AutoDetectParser()) {
            parsers = HashMap<MediaType, Parser>()
            runCatching {
                parse(this@getMimeType, DefaultHandler(), it, ParseContext())
            }.onFailure { it.printStackTrace() }
        }
    }.get(HttpHeaders.CONTENT_TYPE)
}
