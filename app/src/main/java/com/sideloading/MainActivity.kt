package com.sideloading

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.sideloading.Constant.Companion.APP_PACKAGE_NAME
import com.sideloading.Constant.Companion.APP_URL
import com.sideloading.Constant.Companion.GOOGLE_INSTALLER_PACKAGE_NAME
import com.sideloading.Constant.Companion.PERMISSION_REQUEST_STORAGE
import com.sideloading.Constant.Companion.REQUEST_CODE
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainListener {
    val TAG = MainActivity::class.java.simpleName

    private lateinit var mDownloadController: DownloadController
    private var mDownloadId: Long = 0
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDownloadController = DownloadController(this, APP_URL, this)

        button_download.setOnClickListener {
            // check storage permission granted if yes then start downloading file
            checkStoragePermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Check if the storage permission has been granted.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start downloading
                startDownload()
            } else {
                // Permission request was denied.
                mainLayout.showSnackbar(R.string.permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }

    private fun checkStoragePermission() {
        // Check if the storage permission has been granted
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED) {
            // start downloading
            startDownload()
        } else {
            // Permission is missing and must be requested.
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            mainLayout.showSnackbar(
                R.string.access_required,
                Snackbar.LENGTH_INDEFINITE, R.string.ok) {
                requestPermissionsCompat(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE)
            }
        } else {
            requestPermissionsCompat(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_STORAGE)
        }
    }

    private fun startDownload() {
        if (checkAppIsInstalledOrNot()) {
            launchApp()
        } else {
            mDownloadController.enqueueDownload()
        }
    }

    private fun checkAppIsInstalledOrNot(): Boolean {
        val isAppInstalled = appInstalledOrNot(APP_PACKAGE_NAME)
        if (isAppInstalled) {
            return true
        }
        return false
    }

    private fun appInstalledOrNot(packageName: String): Boolean {
        val packageManager = packageManager
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return false
    }

    private fun checkAppSource() {
        val packageManager = packageManager

        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            if (GOOGLE_INSTALLER_PACKAGE_NAME == packageManager.getInstallerPackageName(applicationInfo.packageName)) {
                val toast = Toast.makeText(this, "App was installed by play store", Toast.LENGTH_LONG)
                toast.show()
            } else {
                val toast = Toast.makeText(this, "App was installed from unknown sources", Toast.LENGTH_LONG)
                toast.show()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE) {
                val toast = Toast.makeText(this, "App installed successfully.", Toast.LENGTH_LONG)
                toast.show()
                launchApp()
            }
        }
    }

    private fun launchApp() {
        val launchApp = packageManager.getLaunchIntentForPackage(APP_PACKAGE_NAME)
        startActivity(launchApp)
    }

    override fun onCallMainActivity(listener: ProgressListener) {
        trackProgress()
    }

    override fun getDownloadId(downloadId: Long) {
        mDownloadId = downloadId
    }

    private fun trackProgress() {
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            val progress = mDownloadController.fetchDownloadProgress(mDownloadId)
            val dlProgress = (progress * 100L) / 24324455 //24324455 is the total app size
            Log.e(TAG, "$dlProgress")

            if (progress == 24324455) {
                Log.e(TAG, "download completed")
                removeHandlerCallbacks()
            } else {
                trackProgress()
            }
        }

        mHandler.postDelayed(mRunnable, 1000)
    }

    private fun removeHandlerCallbacks() {
        mHandler.removeCallbacks(mRunnable)
    }
}