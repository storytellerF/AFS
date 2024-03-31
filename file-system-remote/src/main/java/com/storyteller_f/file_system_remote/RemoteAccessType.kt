package com.storyteller_f.file_system_remote

object RemoteAccessType {
    const val FTP = "ftp"
    const val SFTP = "sftp"
    const val SMB = "smb"
    const val FTP_ES = "ftp_es"
    const val FTPS = "ftps"
    const val WEB_DAV = "webdav"
    const val HTTP = "http"
    const val HTTPS = "https"

    @Suppress("unused")
    val DEFAULT_PORT = listOf(-1, 22, 22, 22, 22, 80)

    @Suppress("unused")
    val EXCLUDE_HTTP_PROTOCOL = listOf(SMB, SFTP, FTP, FTP_ES, FTPS, WEB_DAV)

    @Suppress("unused")
    val HTTP_PROTOCOL = listOf(HTTP, HTTPS)

    @Suppress("unused")
    val ALL_PROTOCOL = listOf(
        FTP,
        SMB,
        SFTP,
        FTP_ES,
        FTPS,
        WEB_DAV,
        HTTP,
        HTTPS
    )
}
