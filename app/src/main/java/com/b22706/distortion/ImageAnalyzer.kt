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

class ImageAnalyzer(val audioSensor: AudioSensor, private val context: Context): ImageAnalysis.Analyzer {

    companion object {
        const val LOG_NAME: String = "ImageAnalyzer"
        const val volumeThreshold: Int = 70
    }

    // 出力する画像
    private val _image =
        MutableLiveData<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    val image: LiveData<Bitmap> = _image
    private var willDistortion: Boolean = true
    private var timeStamp = System.currentTimeMillis()
    private var effectLevelArray: Array<Int> = arrayOf(0,3,5,4,2,1)
    private var whiteEffectNum: Int = 0

    // ここに毎フレーム画像が渡される
    override fun analyze(image: ImageProxy) {
        image.use {
            val mat = image.toMat()
            var rMat = fixMatRotation(mat)
            // 音量によって画像処理，音が一定以下なら何もしない．
            val wasSave: Boolean = shouldDistortImage(rMat.clone())
            if (wasSave || whiteEffectNum != 0) rMat = whiteEffect(rMat)
            val bitmap = rMat.toBitmap()
            _image.postValue(bitmap)
        }
    }

    private fun shouldDistortImage(mat: Mat): Boolean {
        val level = getDistortionLevel(audioSensor.getVolume())
        if (level != 0 && toggleBoolean()){
            thread {
                FileManager.saveImage("noDistort",mat.toBitmap())
                Log.d(LOG_NAME, "save image")
                val dstMat = distortImage(mat, level)
                Log.d(LOG_NAME, "created dstImage")
                FileManager.saveImage(
                    "level${level}",
                    dstMat.toBitmap()
                )
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    Toast.makeText(context, "Pictures/distortionに\n歪みレベル${level}を保存しました", Toast.LENGTH_SHORT).show()
                }
                Log.d(LOG_NAME, "画像を保存")
            }
            return true
        }
        return false
    }

    private fun toggleBoolean(): Boolean {
        val now = System.currentTimeMillis()
        // 一定時間経過したら
        if (now - timeStamp > 1000) {
            willDistortion = true
            timeStamp = now
        }
        return if (willDistortion){
            willDistortion = false
            true
        } else {
            false
        }
    }

    // 画像を保存する時，フラッシュみたいのを表示する．
    private fun whiteEffect(input: Mat): Mat {
        Log.d(LOG_NAME, "effect ${effectLevelArray[whiteEffectNum]}")
        whiteEffectNum++
        if (whiteEffectNum >= 5) {
            whiteEffectNum = 0
            return input
        }
        return whiteOutImageFast(input, effectLevelArray[whiteEffectNum])
    }

    // 画像と数字を入力すると画像を白くする．Intには1~5を入力する．
    private fun whiteOutImageFast(src: Mat, level: Int): Mat {
        val whiteLevel = ((255 / 5) * level).toDouble()
        val dst = Mat(src.rows(), src.cols(), CvType.CV_8UC3)
        val whiteScalar = Scalar(whiteLevel, whiteLevel, whiteLevel)
        Core.max(src, whiteScalar, dst)
        return dst
    }

}