package com.b22706.distortion

import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.b22706.distortion.imageUtil.ImageDistortion.Companion.distortImage
import com.b22706.distortion.imageUtil.ImageDistortion.Companion.getDistortionLevel
import com.b22706.distortion.imageUtil.ImageFormatter.Companion.fixMatRotation
import com.b22706.distortion.imageUtil.ImageFormatter.Companion.toBitmap
import com.b22706.distortion.imageUtil.ImageFormatter.Companion.toMat

class ImageAnalyzer(val audioSensor: AudioSensor, val context: Context): ImageAnalysis.Analyzer {

    companion object {
        const val LOG_NAME: String = "ImageAnalyzer"
        const val volumeThreshold: Int = 70
    }

    // 出力する画像
    private val _image =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val image: LiveData<Bitmap> = _image

    // ここに毎フレーム画像が渡される
    @RequiresApi(Build.VERSION_CODES.R)
    override fun analyze(image: ImageProxy) {
        image.use {
            val mat = image.toMat()
            var rMat = fixMatRotation(mat, context)
            // 音量によって画像処理，音が一定以下なら何もしない．
            val level = getDistortionLevel(audioSensor.getVolume())
            if (level > 0) rMat = distortImage(rMat, level)
            val bitmap = rMat.toBitmap()
            _image.postValue(bitmap)
        }
    }
}