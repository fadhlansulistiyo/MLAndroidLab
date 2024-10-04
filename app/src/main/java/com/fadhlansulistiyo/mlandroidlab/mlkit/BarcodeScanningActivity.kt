package com.fadhlansulistiyo.mlandroidlab.mlkit

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityBarcodeScanningBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class BarcodeScanningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeScanningBinding
    private lateinit var barcodeScanner: BarcodeScanner
    private var firstScan = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        binding = ActivityBarcodeScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    // Initializes the UI and camera for barcode scanning
    private fun setupUI() {
        enableEdgeToEdge()
        hideSystemUI()
    }

    // Configures and starts the camera
    private fun startCamera() {
        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )

        val cameraController = LifecycleCameraController(this)
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            createAnalyzer()
        )
        cameraController.bindToLifecycle(this)
        binding.viewFinder.controller = cameraController
    }

    // Creates and returns the barcode analyzer
    @Suppress("DEPRECATION")
    private fun createAnalyzer(): MlKitAnalyzer {
        return MlKitAnalyzer(
            listOf(barcodeScanner),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            ContextCompat.getMainExecutor(this)
        ) { result -> processScanResult(result) }
    }

    // Processes the result of the barcode scan
    private fun processScanResult(result: MlKitAnalyzer.Result?) {
        if (firstScan) {
            result?.getValue(barcodeScanner)?.firstOrNull()?.let { barcode ->
                firstScan = false
                showResultDialog(barcode)
            }
        }
    }

    // Displays the result of the barcode scan in a dialog
    private fun showResultDialog(barcode: Barcode) {
        val alertDialog = AlertDialog.Builder(this)
            .setMessage(barcode.rawValue)
            .setPositiveButton("Open") { _, _ -> handleBarcodeAction(barcode) }
            .setNegativeButton("Scan Again") { _, _ -> restartScanning() }
            .setCancelable(false)
            .create()
        alertDialog.show()
    }

    // Handles the action based on barcode type
    private fun handleBarcodeAction(barcode: Barcode) {
        firstScan = true
        when (barcode.valueType) {
            Barcode.TYPE_URL -> openUrl(barcode.url?.url)
            else -> showUnsupportedMessage()
        }
    }

    // Opens the scanned URL in a browser
    private fun openUrl(url: String?) {
        url?.let {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
            startActivity(intent)
        }
    }

    // Shows a message when an unsupported barcode type is scanned
    private fun showUnsupportedMessage() {
        Toast.makeText(this, "Unsupported data type", Toast.LENGTH_SHORT).show()
        startCamera()
    }

    // Resets the scanning state
    private fun restartScanning() {
        firstScan = true
    }

    // Hides the system UI for full-screen mode
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
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }
}
