package com.fadhlansulistiyo.mlandroidlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fadhlansulistiyo.mlandroidlab.asclepius.CancerDetectionActivity
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityMainBinding
import com.fadhlansulistiyo.mlandroidlab.mlkit.BarcodeScanningActivity
import com.fadhlansulistiyo.mlandroidlab.mlkit.TextRecognitionActivity
import com.fadhlansulistiyo.mlandroidlab.tensorflowlite.TFLImageClassificationActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAction()


    }

    private fun setupAction() {
        binding.toMlkit.cardTextRecognition.setOnClickListener {
            toTextRecognition()
        }
        binding.toMlkit.cardBarcodeScanning.setOnClickListener {
            toBarcodeScanning()
        }
        binding.toAsclepius.cardAsclepius.setOnClickListener {
            toAsclepius()
        }
        binding.toTflite.cardImageClassification.setOnClickListener {
            toTFLImageClassification()
        }
    }

    private fun toTextRecognition() {
        val intent = Intent(this@MainActivity, TextRecognitionActivity::class.java)
        startActivity(intent)
    }

    private fun toBarcodeScanning() {
        val intent = Intent(this@MainActivity, BarcodeScanningActivity::class.java)
        startActivity(intent)
    }

    private fun toAsclepius() {
        val intent = Intent(this@MainActivity, CancerDetectionActivity::class.java)
        startActivity(intent)
    }

    private fun toTFLImageClassification() {
        val intent = Intent(this@MainActivity, TFLImageClassificationActivity::class.java)
        startActivity(intent)
    }
}