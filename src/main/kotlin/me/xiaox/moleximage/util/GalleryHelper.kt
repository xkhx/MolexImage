package me.xiaox.moleximage.util

import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.data.GalleryExact
import me.xiaox.moleximage.feature.Gallery
import net.mamoe.mirai.console.command.CommandSender

fun Collection<String>.snapshotAmount(): Map<String, Int> =
    mapNotNull { Gallery[it] }.associate { it.identity to it.amount }

suspend fun Gallery.getBy(raw: String, by: CommandSender): GalleryExact? = Gallery[raw].also {
    it ?: by.sendMessage("$PREFIX 指定图库 $raw 不存在哦~")
}

suspend fun CommandSender.withGallery(raw: String, block: suspend GalleryExact.() -> Unit) =
    Gallery.getBy(raw, this)?.block()

fun fuzzyMatch(text: String, match: Collection<String>): Pair<String, Int>? = match
    .mapNotNull { if (text.startsWith(it)) it to it.length else null }
    .maxByOrNull { it.second }

fun adaptKeyword(raw: String, restrict: Boolean = false): String? {
    if (restrict) {
        return if (raw in Gallery.getKeywords()) raw else {
            Configuration.aliases.entries.firstOrNull { (_, aliases) -> raw in aliases }?.key
        }
    }

    fuzzyMatch(raw, Gallery.getKeywords())?.let { return it.first }
    return Configuration.aliases.mapNotNull { (gallery, aliases) ->
        fuzzyMatch(raw, aliases)?.let { gallery to it.second }
    }.maxByOrNull { it.second }?.first
}

/**
 * 过滤文本中的标签指定
 *
 * @param text 需要过滤的文本
 * @return Pair<有效图库标签, 无效图库标签>
 */
fun filterTags(text: String): Pair<List<String>, Collection<String>> {
    val notFound = HashSet<String>()
    val galleries = text.split(' ')
        .filter { it.startsWith('#') }
        .mapNotNull { raw ->
            val identity = raw.substringAfter('#')
            adaptKeyword(identity, true).also {
                if (it == null) {
                    notFound += identity
                }
            }
        }
        .toHashSet()
        .toList()
    return galleries to notFound
}