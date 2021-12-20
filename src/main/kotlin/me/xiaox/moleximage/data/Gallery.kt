package me.xiaox.moleximage.data

import me.xiaox.moleximage.MolexImage
import me.xiaox.moleximage.config.Configuration
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import java.io.File
import java.net.URL
import java.util.zip.GZIPInputStream

object Gallery {

    private val rootFolder by lazy {
        File(MolexImage.dataFolder, "image").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    private val supported = listOf("jpg", "png", "gif")

    /**
     * 获取图库列表
     *
     * @return 所有图库的关键词集合
     */
    fun getKeywords(): Set<String> {
        return rootFolder.walk()
            .maxDepth(1)
            .filter { it.isDirectory && it.name != "image" }
            .map { it.name }
            .toHashSet()
    }

    /**
     * 获取图库内容
     *
     * @param keyword 图库关键词
     * @return 图库中的图片文件列表
     */
    fun getImages(keyword: String): Collection<File> {
        return getGallery(keyword)
            .walk()
            .maxDepth(1)
            .filter { it.isFile && it.extension in supported }
            .toList()
    }

    /**
     * 获取一个图库的所有昵称
     *
     * @param keyword 图库关键词
     * @return 指定图库的所有昵称
     */
    fun getAliases(keyword: String): MutableSet<String>? = Configuration.aliases[keyword]

    /**
     * 创建一个图库
     *
     * @param keyword 图库关键词
     * @return 是否创建成功, 若图库已存在会返回 `false`
     */
    fun createGallery(keyword: String): Boolean = keyword != "image" && getGallery(keyword).mkdirs()

    /**
     * 获取一个图片文件
     *
     * @param path 路径
     * @return 基于图库根数据文件夹相对路径下的一个文件
     */
    fun getGallery(path: String): File = File(rootFolder, path)

    /**
     * 获取一个图片文件
     *
     * @param path 路径
     * @return 基于图库根数据文件夹相对路径下的一个文件
     */
    operator fun get(path: String): File = getGallery(path)

    /**
     * 判断某个图库是否存在
     *
     * @param keyword 图库关键词
     * @return 指定图库是否存在
     */
    fun hasGallery(keyword: String): Boolean = keyword in getKeywords()

    /**
     * 判断某个图库是否存在
     *
     * @param keyword 图库关键词
     * @return 指定图库是否存在
     */
    operator fun contains(keyword: String): Boolean = hasGallery(keyword)

    /**
     * 从指定 URL 下载图片以指定 ID 保存至指定图库
     *
     * @param keyword 图库关键词
     * @param url 图片路径
     * @param id 图片 ID
     * @return 保存的图片文件
     */
    fun downloadTo(keyword: String, url: String, id: String): File {
        val connection = URL(url).openConnection()
        connection.addRequestProperty(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
        )
        val bytes = if (connection.contentEncoding == "gzip") {
            GZIPInputStream(connection.getInputStream()).readBytes()
        } else {
            connection.getInputStream().readBytes()
        }
        return File(getGallery(keyword), id).apply {
            writeBytes(bytes)
        }
    }

    /**
     * 保存指定 Image 到指定图库
     *
     * @param keyword 图库关键词
     * @param image 图片对象
     * @return 保存的图片文件
     */
    suspend fun saveTo(keyword: String, image: Image): File = downloadTo(keyword, image.queryUrl(), image.imageId)

}