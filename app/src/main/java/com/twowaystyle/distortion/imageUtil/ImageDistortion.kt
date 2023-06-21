package com.twowaystyle.distortion.imageUtil

import android.util.Log
import com.twowaystyle.distortion.ImageAnalyzer
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class ImageDistortion {

    companion object{

        const val LOG_NAME = "ImageDistortion"

        // 画像を歪ませる 歪ませ度はstrength 1~255まで入るが10前後がいい感じ
        fun distortImage(image: Mat, strength: Int, pitch: Int, t: Double): Mat {
            Log.d(ImageAnalyzer.LOG_NAME, "start distort, $strength ")
            val result = Mat()
            val mapX = Mat()
            val mapY = Mat()
            val size = Size(image.cols().toDouble(), image.rows().toDouble())

            mapX.create(size, CvType.CV_32FC1)
            mapY.create(size, CvType.CV_32FC1)
            val sinScale = Math.PI * 2.0 / (image.cols() + image.rows()) * 10.0 * pitch
            val strengthScale = strength * ((image.cols() + image.rows()) / 2000.0) + 1

            for (i in 0 until image.rows()) {
                mapX.put(i, 0, FloatArray(image.cols()) { j -> (j + strengthScale * sin((i*sinScale) + t )).toFloat() })
            }
            for (i in 0 until image.rows()) {
                mapY.put(i, 0, FloatArray(image.cols()) { j -> (i + strengthScale * sin((j*sinScale) + t )).toFloat() })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }

        // 音量によって画像の歪ませ度を計算
        fun getDistortionLevel(volume: Int) :Int {
            return if (volume <= ImageAnalyzer.volumeThreshold) { 0 }
            else { ((volume- ImageAnalyzer.volumeThreshold) * 2 ) +1 }
        }

        fun distortImageCircle(image: Mat, strength: Int, pitch: Int, t: Double): Mat {
            Log.d(ImageAnalyzer.LOG_NAME, "start distort")
            val result = Mat()
            val mapX = Mat.zeros(image.size(), CvType.CV_32FC1)
            val mapY = Mat.zeros(image.size(), CvType.CV_32FC1)
            val centerX = image.cols() / 2
            val centerY = image.rows() / 2
            val maxRadius = centerX.coerceAtLeast(centerY).toDouble()
            val sinScale = Math.PI * 2 / maxRadius * 10

            for (y in 0 until image.rows()) {
                mapX.put(y, 0, FloatArray(image.cols()) { x ->
                    val offsetX = (x - centerX).toDouble()
                    val offsetY = (y - centerY).toDouble()
                    val angleRad = atan2(offsetY, offsetX)
                    val r = sqrt(offsetX * offsetX + offsetY * offsetY)
                    val distortion = strength * sin((r * sinScale * pitch /5.0 ) - t )
                    (x + distortion * cos(angleRad + t)).toFloat()
                })
            }

            for (y in 0 until image.rows()) {
                mapY.put(y, 0, FloatArray(image.cols()) { x ->
                    val offsetX = (x - centerX).toDouble()
                    val offsetY = (y - centerY).toDouble()
                    val angleRad = atan2(offsetY, offsetX)
                    val r = sqrt(offsetX * offsetX + offsetY * offsetY)
                    val distortion = strength * sin((r * sinScale * pitch /5.0 ) - t )
                    (y + distortion * sin(angleRad + t)).toFloat()
                })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }

        fun distortImageUp(image: Mat, strength: Int, pitch: Int, t: Double): Mat {
            Log.d(ImageAnalyzer.LOG_NAME, "start distort, $strength ")
            val result = Mat()
            val mapX = Mat()
            val mapY = Mat()
            val size = Size(image.cols().toDouble(), image.rows().toDouble())

            mapX.create(size, CvType.CV_32FC1)
            mapY.create(size, CvType.CV_32FC1)
            val sinScale = Math.PI * 2.0 / (image.cols() + image.rows()) * 10.0
            val strengthScale = strength * ((image.cols() + image.rows()) / 2000.0)

            for (y in 0 until image.rows()) {
                mapX.put(y, 0, FloatArray(image.cols()) { x -> (x + sin(x * sinScale)).toFloat() })
            }
            for (y in 0 until image.rows()) {
                mapY.put(y, 0, FloatArray(image.cols()) { x ->
                    (y + strengthScale * sin(x * sinScale) * sin((y * sinScale * pitch + -t))).toFloat()
                })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }


        fun distortImageFlutter(image: Mat, strength: Int, pitch: Int, t: Double): Mat {
            Log.d(ImageAnalyzer.LOG_NAME, "start distort")
            val result = Mat()
            val mapX = Mat.zeros(image.size(), CvType.CV_32FC1)
            val mapY = Mat.zeros(image.size(), CvType.CV_32FC1)
            val centerX = image.cols() / 2
            val centerY = image.rows() / 2
            val maxRadius = max(centerX, centerY)

            for (y in 0 until image.rows()) {
                mapX.put(y, 0, FloatArray(image.cols()) { x ->
                    val offsetX: Double = (x - centerX).toDouble()
                    val offsetY: Double = (y - centerY).toDouble()
                    val angleRad: Double = atan2(offsetY, offsetX)
                    val r: Double = sqrt(offsetX.pow(2) + offsetY.pow(2))
                    val distortion = 30.0 * sin(r/maxRadius)
                    x + (distortion * cos(angleRad+t)).toFloat()
                })
            }
            for (y in 0 until image.rows()) {
                mapY.put(y, 0, FloatArray(image.cols()) { x ->
                    val offsetX = (x - centerX).toDouble()
                    val offsetY = (y - centerY).toDouble()
                    val angleRad = atan2(offsetY, offsetX)
                    val r = sqrt(offsetX.pow(2) + offsetY.pow(2))
                    val distortion = 30.0 * cos(r/maxRadius)
                    y + (distortion * sin(angleRad+t)).toFloat()
                })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }

        fun distortImageWaveCircle(image: Mat, strength: Int, pitch: Int, t: Double): Mat {
            Log.d(ImageAnalyzer.LOG_NAME, "start distort")
            val pitch = 1
            val result = Mat()
            val mapX = Mat.zeros(image.size(), CvType.CV_32FC1)
            val mapY = Mat.zeros(image.size(), CvType.CV_32FC1)
            val centerX = image.cols() / 2
            val centerY = image.rows() / 2
            val maxRadius = centerX.coerceAtLeast(centerY).toDouble()
            val sinScale = Math.PI * 2 / maxRadius * 10

            for (y in 0 until image.rows()) {
                mapX.put(y, 0, FloatArray(image.cols()) { x ->
                    val offsetX = (x - centerX).toDouble()
                    val offsetY = (y - centerY).toDouble()
                    val angleRad = atan2(offsetY, offsetX)
                    val r = sqrt(offsetX * offsetX + offsetY * offsetY)
                    val waveR = r + ((cos (angleRad * 16.0) * 10) * r / 100.0)
                    val distortion = strength * sin((waveR * sinScale * pitch - t)) /2
                    (x + distortion * cos(angleRad)).toFloat()
                })
            }

            for (y in 0 until image.rows()) {
                mapY.put(y, 0, FloatArray(image.cols()) { x ->
                    val offsetX = (x - centerX).toDouble()
                    val offsetY = (y - centerY).toDouble()
                    val angleRad = atan2(offsetY, offsetX)
                    val r = sqrt(offsetX * offsetX + offsetY * offsetY)
                    val waveR = r + ((sin (angleRad * 16.0) * 10) * r / 100.0)
                    val distortion = strength * sin((waveR * sinScale * pitch - t))/2
                    (y + distortion * sin(angleRad)).toFloat()
                })
            }

            Imgproc.remap(image, result, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar.all(0.0))
            Log.d(ImageAnalyzer.LOG_NAME, "end distort")
            return result
        }

    }

}