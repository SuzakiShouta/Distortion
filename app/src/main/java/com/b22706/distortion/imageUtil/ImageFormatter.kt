package com.b22706.distortion.imageUtil

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class ImageFormatter {

    companion object {

        const val LOG_NAME = "ImageFormatter"

        // なんか知らないけど画像が回転するからその補正
        @RequiresApi(Build.VERSION_CODES.R)
        fun fixMatRotation(matOrg: Mat, context: Context): Mat {
            val mat: Mat
            val display = context.display
            when (display?.rotation) {
                Surface.ROTATION_0 -> {
                    mat = Mat(matOrg.cols(), matOrg.rows(), matOrg.type())
                    Core.transpose(matOrg, mat)
                    Core.flip(mat, mat, 1)
                }
                Surface.ROTATION_90 -> mat = matOrg
                Surface.ROTATION_270 -> {
                    mat = matOrg
                    Core.flip(mat, mat, -1)
                }
                else -> {
                    mat = Mat(matOrg.cols(), matOrg.rows(), matOrg.type())
                    Core.transpose(matOrg, mat)
                    Core.flip(mat, mat, 1)
                }
            }
            return mat
        }
        fun ImageProxy.toMat(): Mat {
            val image = this
            val yuvType = Imgproc.COLOR_YUV2BGR_NV21
            val mat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
            val data = ByteArray(image.planes[0].buffer.capacity() + image.planes[1].buffer.capacity())
            image.planes[0].buffer.get(data, 0, image.planes[0].buffer.capacity())
            image.planes[1].buffer.get(data, image.planes[0].buffer.capacity(), image.planes[1].buffer.capacity())
            mat.put(0, 0, data)
            val matRGBA = Mat()
            Imgproc.cvtColor(mat, matRGBA, yuvType)
            return matRGBA
        }

        fun Mat.toBitmap(): Bitmap {
            val bmp = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(this, bmp)
            return bmp
        }
    }
}