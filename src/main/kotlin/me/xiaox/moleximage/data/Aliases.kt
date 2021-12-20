package me.xiaox.moleximage.data

import me.xiaox.moleximage.config.Configuration

@Suppress("MemberVisibilityCanBePrivate")
object Aliases {

    var initialized: Boolean = false
        private set(value) {
            if (field && value) {
                return
            }
            if (!field && value) {
                indexing()
            }
            field = value
        }
    private val mapped = HashMap<String, String>()

    fun indexing() {
        mapped.clear()
        Configuration.aliases.forEach { (keyword, aliases) ->
            aliases.forEach { alias ->
                mapped[alias] = keyword
            }
        }
    }

    operator fun get(alias: String): String? {
        initialized = true
        return mapped[alias]
    }

    operator fun set(keyword: String, alias: String) {
        initialized = true
        mapped[alias] = keyword

        Configuration.aliases.compute(keyword) { _, exist ->
            ((exist ?: HashSet()) + alias).toMutableSet()
        }
    }

    operator fun minusAssign(alias: String) {
        initialized = true
        val keyword = mapped.remove(alias) ?: return

        Configuration.aliases.compute(keyword) { _, exist ->
            ((exist ?: return@compute null) - alias).toMutableSet()
        }
    }

}