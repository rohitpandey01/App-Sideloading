package com.sideloading

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sideloading.Constant.Companion.APP_INSTALL_PATH
import com.sideloading.Constant.Companion.FILE_BASE_PATH
import com.sideloading.Constant.Companion.MIME_TYPE
import com.sideloading.Constant.Companion.PROVIDER_PATH
import com.sideloading.Constant.Companion.REQUEST_CODE
import java.io.File

class DownloadController(private val context: Context, private val url: String, var listener: MainListener) {
    private var activity: Activity? = null
    private lateinit var downloadManager: DownloadManager

    fun enqueueDownload() {
        val fileName = url.substring(url.lastIndexOf('/')+1)
        var destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += fileName

        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        val file = File(destination)
        if (file.exists()) {
            startAppInstallationProcess(context, destination, uri)
        } else {
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadUri = Uri.parse(url)
            val request = DownloadManager.Request(downloadUri)
            request.setMimeType(MIME_TYPE)
            request.setTitle(context.getString(R.string.title_file_download))
            request.setDescription(context.getString(R.string.downloading))

            // set destination
            request.setDestinationUri(uri)

            showInstallOption(destination, uri)
            // Enqueue a new download and same the referenceId
            val downloadId = downloadManager.enqueue(request)
            val toast = Toast.makeText(context, context.getString(R.string.downloading), Toast.LENGTH_LONG)
            toast.show()

            listener.getDownloadId(downloadId)
        }
    }

    private fun showInstallOption(destination: String, uri: Uri) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //startAppInstallationProcess(context, destination, uri)
                context.unregisterReceiver(this)
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun startAppInstallationProcess(context: Context, destination: String, uri: Uri) {
        if (context is Activity)
            activity = context

        val installApp = Intent(Intent.ACTION_VIEW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val contentUri = FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID + PROVIDER_PATH, File(destination))

            installApp.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            installApp.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            installApp.data = contentUri
        } else {
            installApp.setDataAndType(uri, APP_INSTALL_PATH)
        }

        installApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        installApp.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        activity?.startActivityForResult(installApp, REQUEST_CODE)
    }

    fun fetchDownloadProgress(downloadId: Long): Int {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        cursor.moveToFirst()
        val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        cursor.close()

        return bytesDownloaded
    }
}
