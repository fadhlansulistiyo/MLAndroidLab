package com.fadhlansulistiyo.mlandroidlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fadhlansulistiyo.mlandroidlab.asclepius.CancerDetectionActivity
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityMainBinding
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
    }

    private fun setupActionListeners() {
        binding.apply {
            toMlkit.cardTextRecognition.setOnClickListener { navigateTo(TextRecognitionActivity::class.java) }
            toMlkit.cardBarcodeScanning.setOnClickListener { navigateTo(BarcodeScanningActivity::class.java) }
            toAsclepius.cardAsclepius.setOnClickListener { navigateTo(CancerDetectionActivity::class.java) }
            toTflite.cardImageClassification.setOnClickListener { navigateTo(TFLImageClassificationActivity::class.java) }
            toTflite.cardPrediction.setOnClickListener { navigateTo(RicePredictorActivity::class.java) }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this@MainActivity, destination)
        startActivity(intent)
    }
}