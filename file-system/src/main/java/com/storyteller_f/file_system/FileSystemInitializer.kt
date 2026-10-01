package com.storyteller_f.file_system

import android.content.Context
import androidx.startup.Initializer

/** Supplies the application context used by file instance factories. */
class FileSystemInitializer : Initializer<Context> {
    override fun create(context: Context): Context = context.applicationContext.also {
        storedContext = it
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    companion object {
        @Volatile
        private var storedContext: Context? = null

        internal val applicationContext: Context
            get() = checkNotNull(storedContext) {
                "AFS is not initialized. Enable FileSystemInitializer in AndroidX Startup " +
                    "or initialize it using AppInitializer before accessing files."
            }
    }
}
