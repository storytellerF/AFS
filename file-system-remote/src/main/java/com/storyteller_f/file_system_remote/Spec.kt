package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.decodeByBase64
import com.storyteller_f.file_system.encodeByBase64

data class ShareSpec(
    val server: String,
    val port: Int,
    val user: String,
    val password: String,
    val type: String,
    val share: String
) {
    fun toUri(): Uri {
        return Uri.parse("$type://$user:$password@$server:$port/$share")!!
    }

    companion object {
        fun parse(uri: Uri): ShareSpec {
            val authority = uri.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (server, port) = split.last().split(":")
            return ShareSpec(
                server,
                port.toInt(),
                user,
                pass,
                uri.scheme!!,
                uri.path!!.substring(1)
            )
        }
    }
}

data class RemoteSpec(
    val server: String,
    val port: Int,
    val user: String,
    val password: String,
    val type: String
) {
    fun toUri(): Uri {
        val scheme = type
        val encodedServer = server.encodeByBase64()
        return Uri.parse("$scheme://$user:$password@$encodedServer:$port/")!!
    }

    companion object {
        fun parse(parse: Uri): RemoteSpec {
            val scheme = parse.scheme!!
            val authority = parse.authority!!
            val (userConfig, serverConfig) = authority.split("@")
            val (user, pass) = userConfig.split(":")
            val (server, port) = serverConfig.split(":")
            return RemoteSpec(server.decodeByBase64(), port.toInt(), user, pass, type = scheme)
        }
    }
}
