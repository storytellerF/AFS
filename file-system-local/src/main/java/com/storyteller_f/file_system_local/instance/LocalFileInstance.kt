package com.storyteller_f.file_system_local.instance

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.instance.BaseContextFileInstance

/**
 * 定义接口，方法
 */
abstract class LocalFileInstance(context: Context, uri: Uri) : BaseContextFileInstance(context, uri)
