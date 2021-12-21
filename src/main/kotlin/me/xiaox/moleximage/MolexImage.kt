package me.xiaox.moleximage

import me.xiaox.moleximage.command.ImageCommand
import me.xiaox.moleximage.config.Configuration
import me.xiaox.moleximage.event.Listener
import me.xiaox.moleximage.util.DownloadQueue
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object MolexImage : KotlinPlugin(
    JvmPluginDescription(
        "me.xiaox.molex-image",
        "1.0.0",
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
        Listener.init()
        DownloadQueue
    }

    override fun onDisable() {
        ImageCommand.unregister()
    }

}