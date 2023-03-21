package com.b22706.distortion

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.b22706.distortion.ImageDistortion.Companion.distortImage
import com.b22706.distortion.ImageDistortion.Companion.getDistortionLevel
import com.b22706.distortion.ImageFormatter.Companion.fixMatRotation
import com.b22706.distortion.ImageFormatter.Companion.toBitmap
import com.b22706.distortion.ImageFormatter.Companion.toMat
import org.opencv.core.*
import kotlin.concurrent.thread
import org.opencv.core.Core

class ImageAnalyzer(val audioSensor: AudioSensor): ImageAnalysis.Analyzer {

    companion object {
        const val LOG_NAME: String = "ImageAnalyzer"
        const val volumeThreshold: Int = 70
    }

    // 出力する画像
    private val _image =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val image: LiveData<Bitmap> = _image

    // ここに毎フレーム画像が渡される
    override fun analyze(image: ImageProxy) {
        image.use {
            val mat = image.toMat()
            var rMat = fixMatRotation(mat)
            // 音量によって画像処理，音が一定以下なら何もしない．
            val level = getDistortionLevel(audioSensor.getVolume())
            if (level > 0) rMat = distortImage(rMat, level)
            val bitmap = rMat.toBitmap()
            _image.postValue(bitmap)
        }
    }
}