package com.twowaystyle.distortion

import android.content.Context
import android.graphics.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.distortImageWaveCircle
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.getDistortionLevel
import com.twowaystyle.distortion.imageUtil.ImageFormatter.Companion.fixMatRotation
import com.twowaystyle.distortion.imageUtil.ImageFormatter.Companion.toBitmap
import com.twowaystyle.distortion.imageUtil.ImageFormatter.Companion.toMat
import com.twowaystyle.distortion.ui.CameraViewModel

class ImageAnalyzer(val cameraViewModel: CameraViewModel, val context: Context): ImageAnalysis.Analyzer {

    companion object {
        const val LOG_NAME: String = "ImageAnalyzer"
        const val volumeThreshold: Int = 70
    }

    // 出力する画像
    private val _image =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val image: LiveData<Bitmap> = _image

    private val audioSensor: AudioSensor = cameraViewModel.audioSensor
    var t: Double = 0.0

    // ここに毎フレーム画像が渡される
    @RequiresApi(Build.VERSION_CODES.R)
    override fun analyze(image: ImageProxy) {
        image.use {
            val mat = image.toMat()
            var rMat = fixMatRotation(mat ,context ,cameraViewModel.useBackCamera)
            // 音量によって画像処理，音が一定以下なら何もしない．
            val level = getDistortionLevel(audioSensor.volume)
            if (level > 0) {
                rMat = distortImageWaveCircle(rMat, level, audioSensor.pitch+1, t)
                t+=0.1
                if (t > 360.0) { t = 0.0 }
            }
            val bitmap = rMat.toBitmap()
            _image.postValue(bitmap)
        }
    }
}