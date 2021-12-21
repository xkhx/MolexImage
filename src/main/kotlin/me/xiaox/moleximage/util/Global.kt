package me.xiaox.moleximage.util

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.data.Gallery
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.utils.MiraiLogger
import java.io.File
import java.text.DecimalFormat

const val PREFIX = "MolexImage >"

val LOGGER: MiraiLogger
    get() = MolexImage.logger

fun Collection<String>.concat(): String = joinToString("\n")

suspend fun CommandSender.notAdmin(): Boolean = (!hasPermission(MolexImage.PERMISSION_ADMIN)).also {
    if (it) {
        sendMessage("$PREFIX 你没有这个命令的权限")
    }
}

fun fuzzyMatch(text: String, match: Collection<String>): Pair<String, Int>? = match
    .mapNotNull { if (text.startsWith(it)) text to it.length else null }
    .maxByOrNull { it.second }

fun adaptKeyword(raw: String): String? {
    fuzzyMatch(raw, Gallery.getKeywords())?.let { return it.first }
    return Configuration.aliases.mapNotNull { (gallery, aliases) ->
        fuzzyMatch(raw, aliases)?.let { gallery to it.second }
    }.maxByOrNull { it.second }?.first
}

fun toFileSize(byte: Number): String {
    val size = byte.toInt()
    if (size < 10485) {
        return "${DecimalFormat("0.##").format(size / 1024.0)} KB"
    }
    return "${DecimalFormat("0.##").format(size / 1024.0 / 1024.0)} MB"
}

fun toAddSuccess(gallery: String, file: File): String = listOf(
    "$PREFIX 成功添加到图库 $gallery",
    "- 图片 ID: ${file.name}",
    "- 图片大小: ${toFileSize(file.length())}"
).concat()