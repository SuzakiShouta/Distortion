package com.twowaystyle.distortion.imageUtil

import android.util.Log
import com.twowaystyle.distortion.ImageAnalyzer
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.sin

class ImageDistortion {

    companion object{

        const val LOG_NAME = "ImageDistortion"

        // 画像を歪ませる 歪ませ度はstrength 1~255まで入るが10前後がいい感じ
        fun distortImage(image: Mat, strength: Int): Mat {
            Log.d(ImageAnalyzer.LOG_NAME, "start distort")
            val result = Mat()
            val mapX = Mat()
            val mapY = Mat()
            val size = Size(image.cols().toDouble(), image.rows().toDouble())

            mapX.create(size, CvType.CV_32FC1)
            mapY.create(size, CvType.CV_32FC1)
            val sinScale = Math.PI * 2 / (image.cols() + image.rows()) * 30
            val strengthScale = strength * ((image.cols() + image.rows()) / 2000.0) + 1
            Log.d(LOG_NAME, "$strengthScale , $strength")

            for (i in 0 until image.rows()) {
                mapX.put(i, 0, FloatArray(image.cols()) { j -> (j + strengthScale * sin(i*sinScale)).toFloat() })
            }
            for (i in 0 until image.rows()) {
                mapY.put(i, 0, FloatArray(image.cols()) { j -> (i + strengthScale * sin(j*sinScale)).toFloat() })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }

        // 音量によって画像の歪ませ度を計算
        fun getDistortionLevel(volume: Int) :Int {
            return if (volume <= ImageAnalyzer.volumeThreshold) { 0 }
            else { ((volume- ImageAnalyzer.volumeThreshold)/2)+4 }
        }

        fun distortImageCircle(image: Mat, strength: Int): Mat {
            val result = Mat()
            val mapX = Mat()
            val mapY = Mat()
            val size = Size(image.cols().toDouble(), image.rows().toDouble())

            mapX.create(size, CvType.CV_32FC1)
            mapY.create(size, CvType.CV_32FC1)

            val startPointX = size.width / 2
            val startPointY = size.height / 2

            val sinScale = Math.PI * 2 / (image.cols() + image.rows()) * 30
            val strengthScale = strength * ((image.cols() + image.rows()) / 2000.0) + 1

            Log.d(LOG_NAME, "$strengthScale , $strength")

            for (i in 0 until image.rows()) {
                mapX.put(i, 0, FloatArray(image.cols()) { j -> (j + strengthScale * sin(i*sinScale)).toFloat() })
            }
            for (i in 0 until image.rows()) {
                mapY.put(i, 0, FloatArray(image.cols()) { j -> (i + strengthScale * sin(j*sinScale)).toFloat() })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }

    }

}