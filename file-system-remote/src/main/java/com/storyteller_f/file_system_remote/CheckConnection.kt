package com.storyteller_f.file_system_remote

import com.storyteller_f.file_system_remote.instance.FtpInstance
import com.storyteller_f.file_system_remote.instance.FtpsInstance
import com.storyteller_f.file_system_remote.instance.SFtpInstance
import com.storyteller_f.file_system_remote.instance.SmbInstance
import com.storyteller_f.file_system_remote.instance.WebDavInstance

fun RemoteSpec.checkSFtpConnection() {
    SFtpInstance(this)
}

fun ShareSpec.checkSmbConnection() {
    SmbInstance(this).information("/")
}

fun RemoteSpec.checkFtpConnection() {
    FtpInstance(this).open()
}

fun RemoteSpec.checkFtpsConnection() {
    FtpsInstance(this).open()
}

fun RemoteSpec.checkWebDavConnection() {
    WebDavInstance(this).run {
        list(buildRelativePath("/"))
    }
}
