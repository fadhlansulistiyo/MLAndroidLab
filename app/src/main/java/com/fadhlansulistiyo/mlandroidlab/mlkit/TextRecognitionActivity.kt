package com.fadhlansulistiyo.mlandroidlab.mlkit

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.R
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityTextRecognitionBinding
import com.fadhlansulistiyo.mlandroidlab.utils.getImageUri
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import java.io.File

class TextRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextRecognitionBinding
    private var currentImageUri: Uri? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTextRecognitionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        setupAction()
    }

    private fun takePicture() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri!!)
    }

    private fun selectImageFromGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun analyzeImage(uri: Uri) {
        binding.progressIndicator.visibility = View.VISIBLE

        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage: InputImage = InputImage.fromFilePath(this, uri)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText: Text ->
                val detectedText: String = visionText.text

                if (detectedText.isNotBlank()) {
                    binding.progressIndicator.visibility = View.GONE

                    val intent = Intent(this, TextRecognitionResultActivity::class.java)
                    intent.putExtra(TextRecognitionResultActivity.EXTRA_RESULT, detectedText)
                    startActivity(intent)
                } else {
                    binding.progressIndicator.visibility = View.GONE
                    showToast(getString(R.string.no_text_recognized))
                }
            }
            .addOnFailureListener {
                binding.progressIndicator.visibility = View.GONE
                showToast(it.message.toString())
            }
    }

    /*camera setting*/
    private val launcherIntentCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                showImage()
            } else {
                showSnackbar(getString(R.string.no_photos_were_captured))
                currentImageUri = null
            }
        }

    /*gallery setting*/
    private val launcherGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                currentImageUri = it
                showImage()
            } ?: showSnackbar(getString(R.string.no_media_selected))
        }

    private val cropActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultUri = data?.let { UCrop.getOutput(it) }
                if (resultUri != null) {
                    currentImageUri = resultUri
                    showImage()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = result.data?.let { UCrop.getError(it) }
                cropError?.let {
                    showToast("Crop error: ${it.message}")
                }
            }
        }

    private fun editImage(imageUri: Uri) {
        val destinationUri =
            Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))

        val uCrop = UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)

        cropActivityResultLauncher.launch(uCrop.getIntent(this))
    }

    private fun showImage() {
        currentImageUri?.let {
            binding.previewImageView.setImageURI(it)
            showEditButton(true)
        } ?: {
            showToast(getString(R.string.something_went_wrong))
            showEditButton(false)
        }
    }

    private fun setupAction() {
        binding.btnCamera.setOnClickListener {
            takePicture()
        }

        binding.btnGallery.setOnClickListener {
            selectImageFromGallery()
        }

        binding.editButton.setOnClickListener {
            currentImageUri?.let {
                editImage(it)
            } ?: run {
                showToast(getString(R.string.please_select_an_image_first))
            }
        }

        binding.btnTranslate.setOnClickListener {
            currentImageUri?.let {
                analyzeImage(it)
            } ?: run {
                showToast(getString(R.string.empty_image_warning))
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun showEditButton(show: Boolean) {
        binding.editButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val REQUIRED_PERMISSION = android.Manifest.permission.CAMERA
    }
}