package com.twowaystyle.distortion

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kmasan.audiosensor.AudioAnalysis
import com.kmasan.audiosensor.AudioSensor
import java.util.stream.IntStream
import kotlin.math.pow
import kotlin.math.sqrt

class AudioSensor(context: Context): AudioSensor.AudioSensorListener {
    companion object {
        const val LOG_NAME: String = "AudioSensor"

        data class AudioData(
            val time: Long,
            val buffer: ShortArray,
            val fft: DoubleArray,
            val fft2: DoubleArray,
            val envelope: DoubleArray
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as AudioData

                if (!buffer.contentEquals(other.buffer)) return false

                return true
            }

            override fun hashCode(): Int {
                return buffer.contentHashCode()
            }
        }
    }

    private val audioSensor = AudioSensor(context, this)
    private val audioAnalysis = AudioAnalysis()
    private val vowelEnvelopeData = VowelEnvelopeData(context)

    var queue: ArrayDeque<AudioData> = ArrayDeque(listOf())
        private set

    var csvRun = false

    var volume = 0 // 現在の音量
        private set
    private var _volumeLiveData = MutableLiveData(volume)
    val volumeLiveData: LiveData<Int> = _volumeLiveData
    var pitch = 0 // 現在の音の高さレベル
        private set

    var vowel = "null" // 現在の母音
        private set
    private var _vowelLiveData = MutableLiveData(vowel)
    val vowelLiveData: LiveData<String> = _vowelLiveData

    fun start(period: Int) = audioSensor.start(period)
    fun stop() = audioSensor.stop()

    override fun onAudioSensorChanged(data: ShortArray) {
        val fft = audioAnalysis.fft(data)
        val fft2 = audioAnalysis.fft(audioAnalysis.toLogSpectrum(fft))
        val envelope = audioAnalysis.toSpectrumEnvelope(fft, 2.0, audioSensor.sampleRate)
        if(csvRun)
            queue.add(AudioData(System.currentTimeMillis(), data.clone(), fft.clone(), fft2.clone(), envelope.clone()))
        volume = audioAnalysis.toDB(data)
//        Log.d(LOG_NAME, "$volume")
        _volumeLiveData.postValue(volume)

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

        // 母音推定
        vowel = vowelAnalysis(envelope)
        _vowelLiveData.postValue(vowel)
    }

    fun vowelAnalysis(envelope: DoubleArray): String{
        // 母音推定
        // コサイン類似度の計算
        val cosineSimilarityA = cosineSimilarity(envelope, vowelEnvelopeData.voiceA)
        val cosineSimilarityI = cosineSimilarity(envelope, vowelEnvelopeData.voiceI)
        val cosineSimilarityU = cosineSimilarity(envelope, vowelEnvelopeData.voiceU)
        val cosineSimilarityE = cosineSimilarity(envelope, vowelEnvelopeData.voiceE)
        val cosineSimilarityO = cosineSimilarity(envelope, vowelEnvelopeData.voiceO)
        val cosineSimilarityList = listOf(
            cosineSimilarityA, cosineSimilarityI, cosineSimilarityU, cosineSimilarityE, cosineSimilarityO
        )
//        Log.d(LOG_NAME, "cosineSimilarity: $cosineSimilarityList")

        // コサイン類似度の比較
        return when(cosineSimilarityList.max()){
            cosineSimilarityA -> "a"
            cosineSimilarityI -> "i"
            cosineSimilarityU -> "u"
            cosineSimilarityE -> "e"
            cosineSimilarityO -> "o"
            else -> "null"
        }
    }

    fun cosineSimilarity(dataA: DoubleArray, dataB: DoubleArray): Double {
        // コサイン類似度を計算
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for ( i in IntStream.range(0, dataA.size)) {
            dotProduct += dataA[i] * dataB[i];
            normA += dataA[i].pow(2.0)
            normB += dataB[i].pow(2.0)
        }
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}