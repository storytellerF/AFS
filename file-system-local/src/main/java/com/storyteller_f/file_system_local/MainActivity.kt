package com.storyteller_f.file_system_local

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.compat_ktx.getParcelableCompat
import com.storyteller_f.file_system.getFileSystemPrefix
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        var task: CompletableDeferred<Boolean>? = null
        const val REQUEST_SAF_EMULATED = "REQUEST_CODE_SAF_EMULATED"
        const val REQUEST_SAF_SDCARD = "REQUEST_CODE_SAF_SDCARD"
        const val REQUEST_EMULATED = "REQUEST_CODE_EMULATED"
        const val REQUEST_MANAGE = "REQUEST_MANAGE"
        const val REQUEST_CODE_EMULATED = 3

        fun Intent.putBundle(type: String, uri: Uri) {
            putExtras(
                Bundle().apply {
                    putParcelable("path", uri)
                    putString("permission", type)
                }
            )
        }

        fun Intent.fromBundle() = extras!!.let {
            it.getString("permission")!! to it.getParcelableCompat("path", Uri::class.java)!!
        }

        fun bindWaitResult(t: CompletableDeferred<Boolean>) {
            task = t
        }

        fun unbindWaitResult() {
            task = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val (code, uri) = intent.fromBundle()
        when (code) {
            REQUEST_EMULATED -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_EMULATED
            )

            REQUEST_SAF_EMULATED -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requestForEmulatedSAF(uri)
            }

            REQUEST_SAF_SDCARD -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requestForSdcard(uri)
            }

            REQUEST_MANAGE -> requestForManageFile()
        }
    }

    private fun requestForManageFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    success()
                } else {
                    failure()
                }
            }.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } else {
            throw Exception("错误使用request manage！")
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestForSdcard(path: Uri) {
        val context = this
        lifecycleScope.launch {
            val prefix = getFileSystemPrefix(context, path) as LocalFileSystemPrefix
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (processResult(it)) return@registerForActivityResult
                failure()
            }.launch(
                generateSAFRequestIntent(prefix)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun processResult(it: ActivityResult): Boolean {
        if (it.resultCode == RESULT_OK) {
            val uri = it.data?.data
            if (uri != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                FileSystemUriStore.instance.saveUri(
                    this,
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                    uri,
                    null
                )
                success()
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestForEmulatedSAF(path: Uri) {
        val context = this
        lifecycleScope.launch {
            val prefix = getFileSystemPrefix(context, path) as LocalFileSystemPrefix
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (processResult(
                        it
                    )
                ) {
                    return@registerForActivityResult
                }
                failure()
            }.launch(
                generateSAFRequestIntent(prefix)
            )
        }
    }

    private fun failure() {
        task?.complete(false)
        finish()
    }

    private fun success() {
        Toast.makeText(this, "授予权限成功", Toast.LENGTH_SHORT).show()
        task?.complete(true)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_EMULATED) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                success()
                return
            }
        }
        failure()
    }
}
