package com.fadhlansulistiyo.mlandroidlab.tensorflowlite.prediction

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.fadhlansulistiyo.mlandroidlab.R
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.GpuDelegateFactory
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PredictionHelper(
    private val modelName: String = "rice_stock.tflite",
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var interpreter: InterpreterApi? = null
    private var isGPUSupported: Boolean = false

    init {
        initializeTFLite()
    }

    private fun initializeTFLite() {
        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable ->
            val optionsBuilder = TfLiteInitializationOptions.builder()
            if (gpuAvailable) {
                optionsBuilder.setEnableGpuDelegateSupport(true)
                isGPUSupported = true
            }
            TfLite.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            loadLocalModel()
        }.addOnFailureListener {
            onError(context.getString(R.string.tflite_is_not_initialized_yet))
        }
    }

    private fun loadLocalModel() {
        try {
            loadModelFile(context.assets, modelName)?.let {
                initializeInterpreter(it)
            } ?: run {
                onError(context.getString(R.string.model_loading_error))
            }
        } catch (e: IOException) {
            onError(e.message.toString())
            Log.e(TAG, e.message.toString())
        }
    }

    private fun initializeInterpreter(model: ByteBuffer) {
        interpreter?.close()
        try {
            val options = InterpreterApi.Options()
                .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)

            if (isGPUSupported) options.addDelegateFactory(GpuDelegateFactory())

            interpreter = InterpreterApi.create(model, options)
        } catch (e: Exception) {
            onError(e.message.toString())
            Log.e(TAG, e.message.toString())
        }
    }

    fun predict(inputString: String) {
        interpreter?.let {
            val inputArray = floatArrayOf(inputString.toFloat())
            val outputArray = Array(1) { FloatArray(1) }
            try {
                it.run(inputArray, outputArray)
                onResult(outputArray[0][0].toString())
            } catch (e: Exception) {
                handlePredictionError(e)
            }
        } ?: onError(context.getString(R.string.no_tflite_interpreter_loaded))
    }

    private fun handlePredictionError(e: Exception) {
        onError(context.getString(R.string.prediction_error))
        Log.e(TAG, e.message.toString())
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer? {
        return try {
            assetManager.openFd(modelPath).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel = inputStream.channel
                    fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileDescriptor.startOffset,
                        fileDescriptor.declaredLength
                    )
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    fun close() {
        interpreter?.close()
    }

    companion object {
        private const val TAG = "PredictionHelper"
    }
}
