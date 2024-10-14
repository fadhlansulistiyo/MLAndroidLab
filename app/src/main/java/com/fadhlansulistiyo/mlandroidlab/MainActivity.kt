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
import com.fadhlansulistiyo.mlandroidlab.mediapipe.MediaPipeActivity
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
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
    }

    private fun setupActionListeners() {
        binding.apply {
            toMlkit.cardTextRecognition.setOnClickListener { navigateTo(TextRecognitionActivity::class.java) }
            toMlkit.cardBarcodeScanning.setOnClickListener { navigateTo(BarcodeScanningActivity::class.java) }
            toTflite.cardImageClassification.setOnClickListener { navigateTo(TFLImageClassificationActivity::class.java) }
            toTflite.cardPrediction.setOnClickListener { navigateTo(RicePredictorActivity::class.java) }
            toMediapipe.cardImageClassification.setOnClickListener { navigateTo(MediaPipeActivity::class.java) }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this@MainActivity, destination)
        startActivity(intent)
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
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}