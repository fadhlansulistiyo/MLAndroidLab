package com.fadhlansulistiyo.mlandroidlab.tensorflowlite.imageclassification

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.fadhlansulistiyo.mlandroidlab.R
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import org.tensorflow.lite.task.gms.vision.detector.Detection
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector

class ObjectDetectorHelper(
    private var threshold: Float = 0.5f,
    private var maxResults: Int = 5,
    private val modelName: String = "efficientdet_lite0_v1.tflite",
    private val context: Context,
    private val detectorListener: DetectorListener?
) {

    private var objectDetector: ObjectDetector? = null

    init {
        initializeTfLite()
    }

    private fun initializeTfLite() {
        TfLiteGpu.isGpuDelegateAvailable(context)
            .onSuccessTask { gpuAvailable ->
                val optionsBuilder = TfLiteInitializationOptions.builder()
                if (gpuAvailable) {
                    optionsBuilder.setEnableGpuDelegateSupport(true)
                }
                TfLiteVision.initialize(context, optionsBuilder.build())
            }
            .addOnSuccessListener { setupObjectDetector() }
            .addOnFailureListener {
                detectorListener?.onError(context.getString(R.string.tflitevision_is_not_initialized_yet))
            }
    }

    private fun setupObjectDetector() {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)
            .setBaseOptions(getBaseOptions())
            .build()

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                context, modelName, options
            )
        } catch (e: IllegalStateException) {
            handleInitializationError(e)
        }
    }

    private fun getBaseOptions(): BaseOptions {
        val baseOptionsBuilder = BaseOptions.builder()
        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            baseOptionsBuilder.useNnapi()
        } else {
            baseOptionsBuilder.setNumThreads(4) // CPU fallback with 4 threads
        }
        return baseOptionsBuilder.build()
    }

    fun detectObject(image: ImageProxy) {
        if (!isDetectorReady()) return

        val tensorImage = processImage(image)
        val inferenceTime = measureInferenceTime {
            objectDetector?.detect(tensorImage)
        }

        val results = objectDetector?.detect(tensorImage)
        detectorListener?.onResults(results, inferenceTime, tensorImage.height, tensorImage.width)
    }

    private fun isDetectorReady(): Boolean {
        if (!TfLiteVision.isInitialized()) {
            reportError(context.getString(R.string.tflitevision_is_not_initialized_yet))
            return false
        }
        if (objectDetector == null) {
            setupObjectDetector()
        }
        return true
    }

    private fun reportError(message: String) {
        Log.e(TAG, message)
        detectorListener?.onError(message)
    }

    private fun processImage(image: ImageProxy): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(Rot90Op(-image.imageInfo.rotationDegrees / 90))
            .build()
        return imageProcessor.process(TensorImage.fromBitmap(toBitmap(image)))
    }

    private fun measureInferenceTime(action: () -> Unit): Long {
        val startTime = SystemClock.uptimeMillis()
        action()
        return SystemClock.uptimeMillis() - startTime
    }

    private fun toBitmap(image: ImageProxy): Bitmap {
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        image.use { bitmap.copyPixelsFromBuffer(image.planes[0].buffer) }
        image.close()
        return bitmap
    }

    private fun handleInitializationError(e: IllegalStateException) {
        val errorMessage = context.getString(R.string.image_classifier_failed)
        detectorListener?.onError(errorMessage)
        Log.e(TAG, errorMessage, e)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        private const val TAG = "ObjectDetectorHelper"
    }
}
