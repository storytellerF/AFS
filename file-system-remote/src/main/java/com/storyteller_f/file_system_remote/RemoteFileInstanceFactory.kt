package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory2
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_remote.instance.FtpFileInstance
import com.storyteller_f.file_system_remote.instance.FtpsFileInstance
import com.storyteller_f.file_system_remote.instance.HttpFileInstance
import com.storyteller_f.file_system_remote.instance.SFtpFileInstance
import com.storyteller_f.file_system_remote.instance.SmbFileInstance
import com.storyteller_f.file_system_remote.instance.WebDavFileInstance

class RemoteFileInstanceFactory : FileInstanceFactory2 {

    override suspend fun buildInstance(uri: Uri): FileInstance? {
        val scheme = uri.scheme!!
        return when {
            RemoteSchemes.HTTP_PROTOCOL.contains(scheme) -> HttpFileInstance(uri)
            RemoteSchemes.EXCLUDE_HTTP_PROTOCOL.contains(scheme) -> getRemoteInstance(uri)
            else -> null
        }
    }
}

fun getRemoteInstance(uri: Uri): FileInstance {
    return when (uri.scheme) {
        RemoteSchemes.FTP -> FtpFileInstance(uri)
        RemoteSchemes.SMB -> SmbFileInstance(uri)
        RemoteSchemes.SFTP -> SFtpFileInstance(uri)
        RemoteSchemes.FTP_ES, RemoteSchemes.FTPS -> FtpsFileInstance(uri)
        RemoteSchemes.WEB_DAV -> WebDavFileInstance(uri)
        else -> throw Exception(uri.scheme)
    }
}
