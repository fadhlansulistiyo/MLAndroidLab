package com.fadhlansulistiyo.mlandroidlab.tensorflowlite.imageclassification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.R
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityTflimageClassificationBinding
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.text.NumberFormat
import java.util.concurrent.Executors

class TFLImageClassificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTflimageClassificationBinding
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUI()
        startImageClassification()
    }

    private fun setupUI() {
        enableEdgeToEdge()
        binding = ActivityTflimageClassificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun startImageClassification() {
        objectDetectorHelper = createObjectDetectorHelper()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val imageAnalyzer = createImageAnalyzer()
            bindCamera(cameraProviderFuture.get(), imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createObjectDetectorHelper(): ObjectDetectorHelper {
        return ObjectDetectorHelper(
            context = this,
            detectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        showToast(error)
                    }
                }

                override fun onResults(
                    results: MutableList<Detection>?,
                    inferenceTime: Long,
                    imageHeight: Int,
                    imageWidth: Int
                ) {
                    processResults(results, inferenceTime, imageHeight, imageWidth)
                }
            }
        )
    }

    private fun createImageAnalyzer(): ImageAnalysis {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        // Ensure viewFinder's display is not null
        val rotation = binding.viewFinder.display?.rotation ?: Surface.ROTATION_0

        return ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation) // Use fallback if viewFinder's display is null
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().apply {
                setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                    objectDetectorHelper.detectObject(image)
                }
            }
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider, imageAnalyzer: ImageAnalysis) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.viewFinder.surfaceProvider
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            showToast("Failed to bind camera: ${exc.message}")
            Log.e(TAG, "bindCamera: ${exc.message}", exc)
        }
    }

    private fun processResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        runOnUiThread {
            results?.let {
                if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                    binding.overlay.setResults(results, imageHeight, imageWidth)
                    displayResults(it, inferenceTime)
                } else {
                    clearResults()
                }
            }
            binding.overlay.invalidate() // Force a redraw
        }
    }

    private fun displayResults(results: MutableList<Detection>, inferenceTime: Long) {
        val resultText = buildString {
            for (result in results) {
                val label = result.categories[0].label
                val confidence = NumberFormat.getPercentInstance().format(result.categories[0].score).trim()
                append("$label $confidence \n")
            }
        }

        binding.tvResult.text = resultText
        binding.tvInferenceTime.text = "$inferenceTime ms"
    }

    private fun clearResults() {
        binding.overlay.clear()
        binding.tvResult.text = ""
        binding.tvInferenceTime.text = ""
    }

    private fun showToast(message: String) {
        Toast.makeText(this@TFLImageClassificationActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.permission_request_granted), Toast.LENGTH_LONG)
                .show()
        } else {
            Toast.makeText(this, getString(R.string.permission_request_denied), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED


    companion object {
        private const val TAG = "TFLImageClassificationActivity"
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}
