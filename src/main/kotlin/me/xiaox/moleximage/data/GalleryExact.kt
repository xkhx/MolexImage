package me.xiaox.moleximage.data

import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.util.adaptKeyword
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
data class GalleryExact(val identity: String, val force: Boolean = false) {

    init {
        if (identity.lowercase() == "image") {
            error("图库关键词不允许为 $identity")
        }
    }

    val folder = File(Gallery.root, identity).also {
        if (!it.exists()) {
            if (force) {
                it.mkdirs()
            } else {
                error("图库 $identity 不存在")
            }
        }
    }

    val images: Collection<File>
        get() = folder.walk()
            .maxDepth(1)
            .filter { it.isFile && it.extension.lowercase() in Configuration.supports }
            .toSet()

    val amount: Int
        get() = images.size
    val size: Long
        get() = images.sumOf { it.length() }

    val prefixes: Set<String>
        get() = Configuration.prefixes[identity] ?: emptySet()
    val aliases: Set<String>
        get() = Configuration.aliases[identity] ?: emptySet()

    fun addAlias(alias: String) {
        if (adaptKeyword(alias) != null) {
            return
        }
        Configuration.aliases.compute(identity) { _, exist -> ((exist ?: HashSet()) + alias).toMutableSet() }
    }

    fun delAlias(alias: String) {
        Configuration.aliases.compute(identity) { _, exist -> ((exist ?: return@compute null) - alias).toMutableSet() }
    }

    fun addPrefix(prefix: String) {
        Configuration.prefixes.compute(prefix) { _, exist -> ((exist ?: HashSet()) + identity).toMutableSet() }
    }

    fun delPrefix(prefix: String) {
        Configuration.prefixes.compute(prefix) { _, exist -> ((exist ?: return@compute null) - prefix).toMutableSet() }
    }

}
