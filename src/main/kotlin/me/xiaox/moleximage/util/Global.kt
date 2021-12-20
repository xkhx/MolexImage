package me.xiaox.moleximage.util

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.data.Aliases
import me.xiaox.moleximage.data.Gallery
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import java.text.DecimalFormat

const val PREFIX = "MolexImage >"

fun Collection<String>.concat(): String = joinToString("\n")

suspend fun CommandSender.notAdmin(): Boolean = (!hasPermission(MolexImage.PERMISSION_ADMIN)).also {
    if (it) {
        sendMessage("$PREFIX 你没有这个命令的权限")
    }
}

suspend fun adaptKeyword(raw: String, by: CommandSender? = null): String? =
    (if (raw in Gallery) raw else Aliases[raw]).also {
        if (it == null) {
            by?.sendMessage("$PREFIX 图库 $raw 不存在")
        }
    }

fun toFileSize(byte: Number): String {
    val size = byte.toInt()
    if (size < 10485) {
        return "${DecimalFormat("0.##").format(size / 1024.0)} KB"
    }
    return "${DecimalFormat("0.##").format(size / 1024.0 / 1024.0)} MB"
}