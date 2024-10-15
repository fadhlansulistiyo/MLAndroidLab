package com.fadhlansulistiyo.mlandroidlab.mediapipe.audioclassification

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.MainActivity
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityMpaudioClassificationBinding
import com.google.mediapipe.tasks.components.containers.Classifications
import java.text.NumberFormat

class MPAudioClassificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMpaudioClassificationBinding
    private lateinit var audioClassifierHelper: AudioClassifierHelper
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMpaudioClassificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeAudioClassifierHelper()
        setClickListeners()
        updateButtonStates()
        requestPermissionsIfNeeded()
    }

    private fun initializeAudioClassifierHelper() {
        audioClassifierHelper = AudioClassifierHelper(
            context = this,
            classifierListener = object : AudioClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    showToast(error)
                }

                override fun onResults(results: List<Classifications>, inferenceTime: Long) {
                    runOnUiThread {
                        displayResults(results)
                    }
                }
            }
        )
    }

    private fun setClickListeners() {
        binding.apply {
            btnStart.setOnClickListener {
                startAudioClassification()
            }
            btnStop.setOnClickListener {
                stopAudioClassification()
            }
        }
    }

    private fun startAudioClassification() {
        audioClassifierHelper.startAudioClassification()
        isRecording = true
        updateButtonStates()
    }

    private fun stopAudioClassification() {
        audioClassifierHelper.stopAudioClassification()
        isRecording = false
        updateButtonStates()
    }

    private fun updateButtonStates() {
        binding.apply {
            btnStart.isEnabled = !isRecording
            btnStop.isEnabled = isRecording
        }
    }

    private fun displayResults(results: List<Classifications>) {
        if (results.isNotEmpty() && results[0].categories().isNotEmpty()) {
            val sortedCategories = results[0].categories().sortedByDescending { it?.score() }
            val displayResult = sortedCategories.joinToString("\n") {
                "${it.categoryName()} ${NumberFormat.getPercentInstance().format(it.score()).trim()}"
            }
            binding.tvResult.text = displayResult
        } else {
            binding.tvResult.text = ""
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        if (isRecording) {
            audioClassifierHelper.startAudioClassification()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::audioClassifierHelper.isInitialized) {
            audioClassifierHelper.stopAudioClassification()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioClassifierHelper.isInitialized) {
            audioClassifierHelper.stopAudioClassification()
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, REQUIRED_PERMISSION
    ) == PackageManager.PERMISSION_GRANTED

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            showToast(if (isGranted) "Permission granted" else "Permission denied")
        }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@MPAudioClassificationActivity, MainActivity::class.java).apply {
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
        private const val REQUIRED_PERMISSION = Manifest.permission.RECORD_AUDIO
    }
}