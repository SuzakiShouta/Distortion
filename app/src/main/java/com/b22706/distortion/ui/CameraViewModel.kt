package com.b22706.distortion.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.b22706.distortion.AudioSensor
import com.b22706.distortion.ImageAnalyzer
import com.b22706.distortion.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraViewModel(@field:SuppressLint("StaticFieldLeak") val activity: MainActivity) : ViewModel() {

    companion object {
        const val LOG_NAME: String = "CameraViewModel"
    }
    val audioSensor: AudioSensor = AudioSensor(activity)
    val imageAnalyzer: ImageAnalyzer = ImageAnalyzer(this, activity)
    var useBackCamera: Boolean = true

    fun startAudio() {
        audioSensor.start(10, AudioSensor.RECORDING_DB)
    }

    fun startCamera(fragment: Fragment, backCamera: Boolean) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(activity)
        val context: Context = activity
        useBackCamera = backCamera

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                cameraProvider.unbindAll()

                // 各フレームを解析できるAnalysis
                val imageAnalysis = ImageAnalysis.Builder()
                    // RGBA出力が必要な場合は、以下の行を有効にしてください
                    // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    // .setTargetResolution(Size(1920, 1920))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
                // distortionに画像を送りなさい．
                imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer)

                // 背面カメラを使用する場合
                val cameraSelector = if (backCamera) {
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                }
                // フロントカメラを使用する場合
                else {
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
                }

                // これらの設定を使ってLifecycle化
                val camera = cameraProvider.bindToLifecycle(
                    (fragment as LifecycleOwner),
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(LOG_NAME, "[startCamera] Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}