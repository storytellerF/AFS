package com.storyteller_f.file_system_local

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.FileSystemPrefix
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_local.LocalFileSystemPaths.DATA
import com.storyteller_f.file_system_local.LocalFileSystemPaths.DATA_SUB_DATA
import com.storyteller_f.file_system_local.LocalFileSystemPaths.ROOT
import com.storyteller_f.file_system_local.LocalFileSystemPaths.SDCARD
import com.storyteller_f.file_system_local.LocalFileSystemPaths.SELF_PRIMARY
import com.storyteller_f.file_system_local.LocalFileSystemPaths.USER_APP
import com.storyteller_f.file_system_local.LocalFileSystemPaths.USER_DATA_FRONT_PATH
import com.storyteller_f.file_system_local.LocalFileSystemPaths.USER_EMULATED_FRONT_PATH
import com.storyteller_f.file_system_local.instance.DocumentLocalFileInstance
import com.storyteller_f.file_system_local.instance.RegularLocalFileInstance
import com.storyteller_f.file_system_local.instance.fake.AppLocalFileInstance
import com.storyteller_f.file_system_local.instance.fake.FakeLocalFileInstance

object LocalFileSystemPaths {
    const val ROOT = "/"

    @SuppressLint("SdCardPath")
    const val SDCARD = "/sdcard"

    const val STORAGE_PATH = "/storage"
    const val EMULATED_ROOT_PATH = "/storage/emulated"

    const val DATA = "/data"
    const val USER_DATA = "/data/user"

    @SuppressLint("SdCardPath")
    const val USER_DATA_FRONT_PATH = "/data/user/"
    const val USER_APP = "/data/app"
    const val USER_EMULATED_FRONT_PATH = "/storage/emulated/"
    const val DATA_SUB_DATA = "/data/data"

    const val ROOT_USER_EMULATED_PATH = "/storage/emulated/0"
    const val CURRENT_EMULATED_PATH = "/storage/self"
    const val SELF_PRIMARY = "/storage/self/primary"

    val publicPath = listOf("/system", "/mnt")
}

sealed class LocalFileSystemPrefix(open val key: String) : FileSystemPrefix {

    /**
     * 公共目录，没有权限限制
     */
    data object Public : LocalFileSystemPrefix("public")

    /**
     * /sdcard 本身以及所有的子文件
     */
    data object SdCard : LocalFileSystemPrefix(SDCARD)

    /**
     * /storage/self 本身
     */
    data object Self : LocalFileSystemPrefix(LocalFileSystemPaths.CURRENT_EMULATED_PATH)

    /**
     * /storage/self/primary 本身以及所有的子文件
     */
    data object SelfPrimary : LocalFileSystemPrefix(SELF_PRIMARY)

    /**
     * /storage/emulated/0 本身以及所有的子文件
     */
    data class SelfEmulated(val uid: Long) :
        LocalFileSystemPrefix("${USER_EMULATED_FRONT_PATH}$uid")

    /**
     * /storage/emulated 本身
     */
    data object EmulatedRoot : LocalFileSystemPrefix(LocalFileSystemPaths.EMULATED_ROOT_PATH)

    /**
     * /storage 本身
     */
    data object Storage : LocalFileSystemPrefix(LocalFileSystemPaths.STORAGE_PATH)

    /**
     * 外接存储设备，目录应该是/storage/emulated/XX44-XX55 类似的目录
     */
    data class Mounted(override val key: String) : LocalFileSystemPrefix(key)

    /**
     * app 沙盒目录
     */
    data class AppData(override val key: String) : LocalFileSystemPrefix(key)

    /**
     * 用户安装的app 路径/data/app
     */
    data object InstalledApps : LocalFileSystemPrefix(USER_APP)

    /**
     * 根目录本身
     */
    data object Root : LocalFileSystemPrefix(ROOT)

    /**
     * /data 本身
     */
    data object Data : LocalFileSystemPrefix(DATA)

    /**
     * /data/data 本身
     */
    data object Data2 : LocalFileSystemPrefix(DATA_SUB_DATA)

    /**
     * /data/user 本身
     */
    data object DataUser : LocalFileSystemPrefix(LocalFileSystemPaths.USER_DATA)

    /**
     * /data/user/uid 本身
     */
    data class SelfDataRoot(val uid: Long) : LocalFileSystemPrefix("${USER_DATA_FRONT_PATH}$uid")

    data class SelfPackage(val uid: Long, val packageName: String) :
        LocalFileSystemPrefix("${USER_DATA_FRONT_PATH}$uid/$packageName")
}

fun getLocalFileSystemPrefix(context: Context, path: String): LocalFileSystemPrefix =
    when {
        LocalFileSystemPaths.publicPath.any { path.startsWith(it) } -> LocalFileSystemPrefix.Public
        path.startsWith(LocalFileSystemPrefix.SdCard.key) -> LocalFileSystemPrefix.SdCard
        path.startsWith(context.appDataDir()) -> LocalFileSystemPrefix.AppData(context.appDataDir())
        path.startsWith(
            "${USER_DATA_FRONT_PATH}${context.getMyId()}/${context.packageName}"
        ) -> LocalFileSystemPrefix.SelfPackage(
            context.getMyId(),
            context.packageName
        )

        path.startsWith(USER_EMULATED_FRONT_PATH) -> LocalFileSystemPrefix.SelfEmulated(
            path.substring(
                USER_EMULATED_FRONT_PATH.length
            ).substringBefore("/").toLong()
        )

        path == LocalFileSystemPaths.CURRENT_EMULATED_PATH -> LocalFileSystemPrefix.Self
        path.startsWith(LocalFileSystemPaths.CURRENT_EMULATED_PATH) -> LocalFileSystemPrefix.SelfPrimary
        path == LocalFileSystemPrefix.EmulatedRoot.key -> LocalFileSystemPrefix.EmulatedRoot
        path == LocalFileSystemPrefix.Storage.key -> LocalFileSystemPrefix.Storage
        path.startsWith(LocalFileSystemPaths.STORAGE_PATH) -> LocalFileSystemPrefix.Mounted(
            extractSdPath(
                path
            )
        )

        path == LocalFileSystemPrefix.Root.key -> LocalFileSystemPrefix.Root
        path == LocalFileSystemPrefix.Data.key -> LocalFileSystemPrefix.Data
        path == LocalFileSystemPrefix.Data2.key -> LocalFileSystemPrefix.Data2
        path == LocalFileSystemPrefix.DataUser.key -> LocalFileSystemPrefix.DataUser
        path == "${USER_DATA_FRONT_PATH}${context.getMyId()}" -> LocalFileSystemPrefix.SelfDataRoot(
            context.getMyId()
        )

        path.startsWith(LocalFileSystemPrefix.InstalledApps.key) -> LocalFileSystemPrefix.InstalledApps
        else -> throw Exception("unrecognized path $path")
    }

fun getLocalFileSystemInstance(context: Context, uri: Uri): FileInstance {
    assert(uri.scheme == ContentResolver.SCHEME_FILE) {
        "only permit local system $uri"
    }

    return when (val prefix = getLocalFileSystemPrefix(context, uri.path!!)) {
        is LocalFileSystemPrefix.AppData -> RegularLocalFileInstance(context, uri)

        LocalFileSystemPrefix.Data -> FakeLocalFileInstance(context, uri)

        LocalFileSystemPrefix.Data2 -> FakeLocalFileInstance(context, uri)

        is LocalFileSystemPrefix.SelfDataRoot -> FakeLocalFileInstance(context, uri)

        is LocalFileSystemPrefix.SelfPackage -> RegularLocalFileInstance(context, uri)

        LocalFileSystemPrefix.DataUser -> FakeLocalFileInstance(context, uri)

        LocalFileSystemPrefix.EmulatedRoot -> FakeLocalFileInstance(context, uri)

        LocalFileSystemPrefix.InstalledApps -> AppLocalFileInstance(context, uri)

        is LocalFileSystemPrefix.Mounted -> when {
            // 外接sd卡
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> RegularLocalFileInstance(
                context,
                uri
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(
                context,
                uri,
                prefix.key
            )

            Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 -> RegularLocalFileInstance(
                context,
                uri
            )

            else -> RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.Public -> RegularLocalFileInstance(context, uri)

        LocalFileSystemPrefix.Root -> FakeLocalFileInstance(context, uri)

        is LocalFileSystemPrefix.SelfEmulated -> when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(
                context,
                uri,
                prefix.key
            )

            else -> RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.SdCard -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
            DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
        } else {
            RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.Self -> when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> FakeLocalFileInstance(context, uri)

            else -> RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.SelfPrimary -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
            DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
        } else {
            RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.Storage -> FakeLocalFileInstance(context, uri)
    }
}
