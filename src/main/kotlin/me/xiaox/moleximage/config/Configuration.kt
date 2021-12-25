package me.xiaox.moleximage.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Configuration : AutoSavePluginConfig("config") {

    val addFrequency: Long by value(1L)

    val downloadFrequency: Long by value(1L)

    val supports: MutableSet<String> by value(hashSetOf("jpg", "png", "gif", "jpeg"))

    val aliases: MutableMap<String, MutableSet<String>> by value(
        hashMapOf(
            "莫图" to mutableSetOf("莫老"),
            "贺兰" to mutableSetOf("荷兰")
        )
    )

    val prefixes: MutableMap<String, MutableSet<String>> by value(
        hashMapOf(
            "来只" to mutableSetOf("仓鼠")
        )
    )

    val triggers: MutableSet<String> by value(
        hashSetOf("来张", "来个", "来点")
    )

}