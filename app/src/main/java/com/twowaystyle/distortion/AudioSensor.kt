package com.twowaystyle.distortion

import android.content.Context
import com.kmasan.audiosensor.AudioAnalysis
import com.kmasan.audiosensor.AudioSensor

class AudioSensor(context: Context): AudioSensor.AudioSensorListener {
    companion object {
        const val LOG_NAME: String = "MyAudioSensor"
    }

    private val audioSensor = AudioSensor(context, this)
    private val audioAnalysis = AudioAnalysis()

    var volume = 0 // 現在の音量 db
        private set
    var pitch = 0 // 現在の音の高さレベル 0~5
        private set

    fun start(period: Int) = audioSensor.start(period)
    fun stop() = audioSensor.stop()

    override fun onAudioSensorChanged(data: ShortArray) {
        val fft = audioAnalysis.fft(data)
        volume = audioAnalysis.toDB(data)
//        Log.d(LOG_NAME, "$volume")

        // 最大振幅の周波数
        val maxFrequency: Int = audioAnalysis.toMaxFrequency(fft, audioSensor.sampleRate)

        // 最大振幅の周波数をレベルに変換（0~)
        val levelStage = 5 // レベルの段階数
        val minVoice = 500 // レベル1とする最低周波数
        val maxVoice = 2500 // レベル最大とする最大周波数
        val levelInterval = (maxVoice - minVoice)/(levelStage-1) // レベルの間隔
        val voiceLevel: Int = (maxFrequency - minVoice + levelInterval)/ levelInterval // レベルに変換
        pitch = when{
            voiceLevel < 0 -> 0
            voiceLevel > levelStage -> levelStage
            else -> voiceLevel
        }
    }
}