package com.twowaystyle.distortion

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager {
    companion object {
        const val LOG_NAME = "FileManager"
        fun saveImage(imageName: String, image: Bitmap) {
            Log.d(LOG_NAME,"save Image")
            Log.d(LOG_NAME,imageName)
            val appName = "ぐにゃぁ"
            val pictureDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                appName
            )
            pictureDir.mkdirs()

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val file = File(pictureDir, "${currentDate}${imageName}.jpg")

            val out = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        }
    }
}