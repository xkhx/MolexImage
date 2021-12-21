package me.xiaox.moleximage.data

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.util.adaptKeyword
import java.io.File

object Gallery {

    val root by lazy {
        File(MolexImage.dataFolder, "image").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    /**
     * 获取图库列表
     *
     * @return 所有图库的关键词集合
     */
    fun getKeywords(): Set<String> {
        return root.walk()
            .maxDepth(1)
            .filter { it.isDirectory && it.name != "image" }
            .map { it.name }
            .toHashSet()
    }

    /**
     * 获取指定的图库
     *
     * @param raw 图库关键词, 支持昵称
     * @return 图库数据对象, 若不存在返回 null
     */
    operator fun get(raw: String): GalleryExact? {
        return try {
            GalleryExact(adaptKeyword(raw) ?: return null)
        } catch (ex: Throwable) {
            null
        }
    }

    /**
     * 判断某个图库是否存在
     *
     * @param keyword 图库关键词
     * @return 指定图库是否存在
     */
    operator fun contains(keyword: String): Boolean = keyword in getKeywords()

}