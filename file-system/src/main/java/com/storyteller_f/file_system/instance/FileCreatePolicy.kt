package com.storyteller_f.file_system.instance

sealed interface FileCreatePolicy {
    data object NotCreate : FileCreatePolicy

    class Create(val isFile: Boolean) : FileCreatePolicy
}
