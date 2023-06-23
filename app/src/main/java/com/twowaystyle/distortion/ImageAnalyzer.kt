package com.twowaystyle.distortion

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.distortImage
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.distortImageFlutter
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.distortImageWave
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.distortImageWaveCircle
import com.twowaystyle.distortion.imageUtil.ImageDistortion.Companion.getDistortionLevel
import com.twowaystyle.distortion.imageUtil.ImageFormatter.Companion.fixMatRotation
import com.twowaystyle.distortion.imageUtil.ImageFormatter.Companion.toBitmap
import com.twowaystyle.distortion.imageUtil.ImageFormatter.Companion.toMat
import com.twowaystyle.distortion.ui.CameraViewModel
import org.opencv.core.Mat

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
            val level = getDistortionLevel(90)
            if (level > 0) {
                rMat = distortionByVoice(rMat, level)
            }
            val bitmap = rMat.toBitmap()
            _image.postValue(bitmap)
        }
    }

    fun distortionByVoice(img: Mat, level: Int): Mat {
        val vowel: String = audioSensor.vowel
        val pitch: Int = audioSensor.pitch + 1
        val pitchThreshold: Int = 1
        Log.d(LOG_NAME, "vowel = $vowel, pitch = $pitch")
        t+=0.1
        if (t > 360.0) { t = 0.0 }

        // ベーシックな歪み方
        return if ( vowel == "a" || vowel == "o" ) {
            // 声が低いと逆方向に歪む
            if (pitch < pitchThreshold) { distortImage(img, level, pitch, -t) }
            else { distortImage(img, level, pitch, t) }
        }
        // 棘の吹き出しのような歪み方
        else if ( vowel == "i" || vowel == "e" ) {
            if (pitch < pitchThreshold) { distortImageWaveCircle(img, level, pitch, -t) }
            else { distortImageWaveCircle(img, level, pitch, t) }
        }
        // 横波のみの歪み方
        else if (vowel == "u") {
            if (pitch < pitchThreshold) { distortImageWave(img, level, pitch, -t) }
            else { distortImageWave(img, level, pitch, t) }
        } else {
            distortImageFlutter(img, level, pitch, t)
        }

    }
}