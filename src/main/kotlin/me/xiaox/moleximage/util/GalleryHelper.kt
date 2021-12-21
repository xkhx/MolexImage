package me.xiaox.moleximage.util

import me.xiaox.moleximage.data.Gallery
import me.xiaox.moleximage.data.GalleryExact
import net.mamoe.mirai.console.command.CommandSender

suspend fun Gallery.getBy(raw: String, by: CommandSender): GalleryExact? = Gallery[raw].also {
    it ?: by.sendMessage("$PREFIX 指定图库 $raw 不存在哦~")
}

suspend fun CommandSender.withGallery(raw: String, block: suspend GalleryExact.() -> Unit) =
    Gallery.getBy(raw, this)?.block()