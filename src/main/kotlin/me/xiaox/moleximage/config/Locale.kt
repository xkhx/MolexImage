package me.xiaox.moleximage.config

import me.xiaox.moleximage.util.PREFIX
import java.io.File
import java.text.DecimalFormat

object Locale {

    fun toFilesize(byte: Number): String {
        val size = byte.toInt()
        if (size < 10485) {
            return "${DecimalFormat("0.##").format(size / 1024.0)} KB"
        }
        return "${DecimalFormat("0.##").format(size / 1024.0 / 1024.0)} MB"
    }

    fun toFilesize(file: File): String = toFilesize(file.length())

    fun toFilesize(files: Collection<File>): String = toFilesize(files.sumOf { it.length() })

    fun toGalleryNotFound(notFound: Collection<String>): String {
        return "$PREFIX 指定图库 ${notFound.joinToString(", ")} 不存在"
    }

}