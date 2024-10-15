package com.fadhlansulistiyo.mlandroidlab.mediapipe.textclassification

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.fadhlansulistiyo.mlandroidlab.R
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import java.util.concurrent.ScheduledThreadPoolExecutor

class TextClassifierHelper(
    private val modelName: String = "bert_classifier.tflite",
    private var classifierListener: ClassifierListener? = null,
    val context: Context,
) {

    private var textClassifier: TextClassifier? = null
    private var executor: ScheduledThreadPoolExecutor? = null

    init {
        initClassifier()
    }

    private fun initClassifier() {
        try {
            val optionsBuilder = TextClassifier.TextClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelName).build())

            textClassifier = TextClassifier.createFromOptions(context, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            handleError(context.getString(R.string.text_classifier_failed), e)
        }
    }

    fun classify(inputText: String) {
        if (textClassifier == null) {
            initClassifier()
        }

        if (executor == null || executor?.isShutdown == true) {
            executor = ScheduledThreadPoolExecutor(1)
        }

        executor?.execute {
            val inferenceStartTime = SystemClock.uptimeMillis()
            try {
                val results = textClassifier?.classify(inputText)
                val inferenceTime = SystemClock.uptimeMillis() - inferenceStartTime
                classifierListener?.onResults(results?.classificationResult()?.classifications(), inferenceTime)
            } catch (e: Exception) {
                handleError(context.getString(R.string.classification_failed), e)
            }
        }
    }

    private fun handleError(errorMsg: String, exception: Exception) {
        Log.e(TAG, errorMsg, exception)
        classifierListener?.onError(errorMsg)
    }

    fun shutdownExecutor() {
        executor?.shutdownNow()
        executor = null
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object {
        private const val TAG = "TextClassifierHelper"
    }
}