package com.storyteller_f.file_system_remote

import android.os.Build
import androidx.annotation.RequiresApi
import com.storyteller_f.file_system.AFSProvider

@RequiresApi(Build.VERSION_CODES.O)
class FtpFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.FTP
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class FtpsFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.FTPS
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class HttpFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.HTTP
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class HttpsFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.HTTPS
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class SFtpFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.SFTP
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class SmbFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.SMB
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class WebDavFileSystemProvider : AFSProvider() {
    override fun getScheme(): String {
        return RemoteSchemes.WEB_DAV
    }
}
