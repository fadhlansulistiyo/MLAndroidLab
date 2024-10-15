package com.fadhlansulistiyo.mlandroidlab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityMainBinding
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivitySentimentAnalysisBinding
import com.fadhlansulistiyo.mlandroidlab.mediapipe.audioclassification.MPAudioClassificationActivity
import com.fadhlansulistiyo.mlandroidlab.mediapipe.imageclassification.MPImageClassificationActivity
import com.fadhlansulistiyo.mlandroidlab.mediapipe.textclassification.SentimentAnalysisActivity
import com.fadhlansulistiyo.mlandroidlab.mlkit.BarcodeScanningActivity
import com.fadhlansulistiyo.mlandroidlab.mlkit.TextRecognitionActivity
import com.fadhlansulistiyo.mlandroidlab.tensorflowlite.imageclassification.TFLImageClassificationActivity
import com.fadhlansulistiyo.mlandroidlab.tensorflowlite.prediction.RicePredictorActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionListeners()

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun setupActionListeners() {
        binding.apply {
            toMlkit.cardTextRecognition.setOnClickListener { navigateTo(TextRecognitionActivity::class.java) }
            toMlkit.cardBarcodeScanning.setOnClickListener { navigateTo(BarcodeScanningActivity::class.java) }
            toTflite.cardImageClassification.setOnClickListener { navigateTo(TFLImageClassificationActivity::class.java) }
            toTflite.cardPrediction.setOnClickListener { navigateTo(RicePredictorActivity::class.java) }
            toMediapipe.cardImageClassification.setOnClickListener { navigateTo(MPImageClassificationActivity::class.java) }
            toMediapipe.cardAudioClassification.setOnClickListener { navigateTo(MPAudioClassificationActivity::class.java) }
            toMediapipe.cardSentimentTextClassification.setOnClickListener { navigateTo(SentimentAnalysisActivity::class.java) }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this@MainActivity, destination)
        startActivity(intent)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        val message = if (allGranted) {
            getString(R.string.permission_request_granted)
        } else {
            getString(R.string.permission_request_denied)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}