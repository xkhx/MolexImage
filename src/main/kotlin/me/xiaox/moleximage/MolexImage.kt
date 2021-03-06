package me.xiaox.moleximage

import me.xiaox.moleximage.command.ImageCommand
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.event.BatchListener
import me.xiaox.moleximage.event.QuickAddingListener
import me.xiaox.moleximage.event.QuoteListener
import me.xiaox.moleximage.event.RequestListener
import me.xiaox.moleximage.queue.impl.AddManager
import me.xiaox.moleximage.queue.impl.DownloadManager
import me.xiaox.moleximage.util.MessageCache
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.globalEventChannel

object MolexImage : KotlinPlugin(
    JvmPluginDescription(
        "me.xiaox.molex-image",
        "1.3.1",
        "MolexImage"
    )
) {

    val PERMISSION_ADMIN by lazy {
        PermissionService.INSTANCE.register(
            permissionId("molex.image.admin"),
            "莫氏管理员权限"
        )
    }

    override fun onEnable() {
        Configuration.reload()

        ImageCommand.register()
        with(globalEventChannel()) {
            registerListenerHost(BatchListener)
            registerListenerHost(QuoteListener)
            registerListenerHost(QuickAddingListener)
            registerListenerHost(RequestListener)
            registerListenerHost(MessageCache)
        }
        AddManager
        DownloadManager
    }

    override fun onDisable() {
        ImageCommand.unregister()
    }

}