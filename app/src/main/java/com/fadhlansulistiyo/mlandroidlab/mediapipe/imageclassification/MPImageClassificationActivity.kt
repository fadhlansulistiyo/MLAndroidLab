package com.fadhlansulistiyo.mlandroidlab.mediapipe.imageclassification

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.MainActivity
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityMpImageClassificationBinding
import com.google.mediapipe.tasks.components.containers.Classifications
import java.text.NumberFormat
import java.util.concurrent.Executors

class MPImageClassificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMpImageClassificationBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var imageClassificationHelper: MPImageClassificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMpImageClassificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUI()
        startCamera()
    }

    override fun onPause() {
        super.onPause()
        imageClassificationHelper.releaseClassifier()
    }

    private fun startCamera() {
        initializeMediaPipeHelper()
        setupCameraProvider()
    }

    private fun initializeMediaPipeHelper() {
        imageClassificationHelper = MPImageClassificationHelper(
            context = this,
            classifierListener = object : MPImageClassificationHelper.ClassifierListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        showToast(error)
                    }
                }

                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    runOnUiThread {
                        updateUIWithResults(results, inferenceTime)
                    }
                }
            }
        )
    }

    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                bindCameraUseCases(cameraProviderFuture.get())
            } catch (exc: Exception) {
                handleError(exc.message.toString())
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val imageAnalyzer = createImageAnalyzer()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.viewFinder.surfaceProvider
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            handleError(exc.message.toString())
        }
    }

    private fun createImageAnalyzer(): ImageAnalysis {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        return ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also {
                it.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                    imageClassificationHelper.classifyImage(image)
                }
            }
    }

    private fun updateUIWithResults(results: List<Classifications>?, inferenceTime: Long) {
        results?.let { it ->
            if (it.isNotEmpty() && it[0].categories().isNotEmpty()) {
                val sortedCategories = it[0].categories().sortedByDescending { it?.score() }
                val displayResult = sortedCategories.joinToString("\n") { category ->
                    "${category.categoryName()} " + NumberFormat.getPercentInstance()
                        .format(category.score()).trim()
                }
                binding.tvResult.text = displayResult
                binding.tvInferenceTime.text = "$inferenceTime ms"
            } else {
                clearResults()
            }
        } ?: clearResults()
    }

    private fun clearResults() {
        binding.tvResult.text = ""
        binding.tvInferenceTime.text = ""
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleError(errorMessage: String) {
        showToast("Failed to start camera: $errorMessage")
        Log.e(TAG, "Error starting camera: $errorMessage")
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@MPImageClassificationActivity, MainActivity::class.java).apply {
            // Add flags to clear the task stack and start the app fresh
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Start the activity and close the current one
        startActivity(intent)
        finish() // Ensure the current activity is finished
        // Optionally kill the process to completely restart the app
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @Suppress("DEPRECATION")
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val TAG = "MPImageClassificationActivity"
    }
}