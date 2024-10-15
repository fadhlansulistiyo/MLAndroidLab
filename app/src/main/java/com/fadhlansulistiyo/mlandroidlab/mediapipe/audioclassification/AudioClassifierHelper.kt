package com.fadhlansulistiyo.mlandroidlab.mediapipe.audioclassification

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import com.fadhlansulistiyo.mlandroidlab.R
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.AudioData.AudioDataFormat
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.core.BaseOptions
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AudioClassifierHelper(
    private val threshold: Float = 0.1f,
    private val maxResults: Int = 3,
    private val modelName: String = "yamnet.tflite",
    private val runningMode: RunningMode = RunningMode.AUDIO_STREAM,
    private val overlap: Float = 0.5f,
    private var classifierListener: ClassifierListener? = null,
    private val context: Context,
) {

    private var audioClassifier: AudioClassifier? = null
    private var recorder: AudioRecord? = null
    private var executor: ScheduledThreadPoolExecutor? = null

    init {
        initClassifier()
    }

    private fun initClassifier() {
        try {
            val baseOptions = BaseOptions.builder().setModelAssetPath(modelName).build()
            val options = AudioClassifier.AudioClassifierOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .setRunningMode(runningMode)
                .setBaseOptions(baseOptions)
                .apply {
                    if (runningMode == RunningMode.AUDIO_STREAM) {
                        setResultListener(::streamAudioResultListener)
                        setErrorListener(::streamAudioErrorListener)
                    }
                }
                .build()

            audioClassifier = AudioClassifier.createFromOptions(context, options)

            if (runningMode == RunningMode.AUDIO_STREAM) {
                recorder = audioClassifier?.createAudioRecord(
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    SAMPLING_RATE_IN_HZ,
                    BUFFER_SIZE_IN_BYTES.toInt()
                )
            }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    fun startAudioClassification() {
        if (audioClassifier == null) initClassifier()

        recorder?.takeIf { it.recordingState != AudioRecord.RECORDSTATE_RECORDING }?.apply {
            startRecording()
            executor = ScheduledThreadPoolExecutor(1)

            val classifyRunnable = Runnable {
                classifyAudioAsync(this)
            }

            val interval = calculateInterval()

            executor?.scheduleWithFixedDelay(classifyRunnable, 0, interval, TimeUnit.MILLISECONDS)
        }
    }

    private fun classifyAudioAsync(audioRecord: AudioRecord) {
        val audioData = AudioData.create(
            AudioDataFormat.create(recorder?.format), SAMPLING_RATE_IN_HZ
        ).apply { load(audioRecord) }

        audioClassifier?.classifyAsync(audioData, SystemClock.uptimeMillis())
    }

    fun stopAudioClassification() {
        executor?.shutdownNow()
        recorder?.stop()
        audioClassifier?.close()
        audioClassifier = null
    }

    private fun streamAudioResultListener(result: AudioClassifierResult) {
        classifierListener?.onResults(
            result.classificationResults().first().classifications(),
            result.timestampMs()
        )
    }

    private fun streamAudioErrorListener(e: RuntimeException) {
        classifierListener?.onError(e.message.toString())
    }

    private fun handleError(e: Exception) {
        classifierListener?.onError(context.getString(R.string.audio_classifier_failed))
        Log.e(TAG, "AudioClassifier initialization failed: ${e.message}")
    }

    private fun calculateInterval(): Long {
        val lengthInMilliSeconds = ((REQUIRE_INPUT_BUFFER_SIZE * 1.0f) / SAMPLING_RATE_IN_HZ) * 1000
        return (lengthInMilliSeconds * (1 - overlap)).toLong()
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(results: List<Classifications>, inferenceTime: Long)
    }

    companion object {
        private const val TAG = "AudioClassifierHelper"
        private const val SAMPLING_RATE_IN_HZ = 16000
        private const val EXPECTED_INPUT_LENGTH = 0.975f
        private const val REQUIRE_INPUT_BUFFER_SIZE = SAMPLING_RATE_IN_HZ * EXPECTED_INPUT_LENGTH
        private const val BUFFER_SIZE_FACTOR = 2
        private const val BUFFER_SIZE_IN_BYTES =
            REQUIRE_INPUT_BUFFER_SIZE * Float.SIZE_BYTES * BUFFER_SIZE_FACTOR
    }
}
