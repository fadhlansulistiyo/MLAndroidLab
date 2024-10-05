package com.fadhlansulistiyo.mlandroidlab.asclepius

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fadhlansulistiyo.mlandroidlab.R
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityCancerDetectionBinding
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File

class CancerDetectionActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener {

    private lateinit var binding: ActivityCancerDetectionBinding
    private var currentImageUri: Uri? = null
    private var imageClassifierHelper: ImageClassifierHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCancerDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = this@CancerDetectionActivity
        )

        setupAction()
    }

    private fun setupAction() {
        // select image from gallery
        binding.galleryButton.setOnClickListener {
            selectImageFromGallery()
        }

        // analyze image
        binding.analyzeButton.setOnClickListener {
            showProgressIndicator(true)

            currentImageUri?.let {
                analyzeImage(it)
            } ?: run {
                showProgressIndicator(false)
                showToast(getString(R.string.empty_image_warning))
            }
        }

        // edit image
        binding.editButton.setOnClickListener {
            currentImageUri?.let {
                startCrop(it)
            } ?: run {
                showToast(getString(R.string.please_select_an_image_first))
            }
        }
    }

    private fun selectImageFromGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun analyzeImage(uri: Uri) {
        imageClassifierHelper?.classifyStaticImage(uri)
    }

    override fun onResults(results: List<Classifications>?) {
        showProgressIndicator(false)

        if (!results.isNullOrEmpty()) {
            val output = results[0].categories
            if (output.isNotEmpty()) {
                val cancerCategory = output[0]
                val isCancer = cancerCategory.label == "Cancer"
                val confidence = cancerCategory.score

                moveToResult(
                    success = true,
                    isCancer = isCancer,
                    confidence = confidence,
                    error = null
                )
            }
        } else {
            moveToResult(
                success = false,
                isCancer = false,
                confidence = 0.0f,
                error = "No results found."
            )
        }
    }

    override fun onError(error: String) {
        showProgressIndicator(false)
        moveToResult(success = false, isCancer = false, confidence = 0.0f, error = error)
    }

    private fun moveToResult(
        success: Boolean,
        isCancer: Boolean,
        confidence: Float,
        error: String?
    ) {
        val intent = Intent(this, CancerDetectionResultActivity::class.java)
        intent.putExtra(CancerDetectionResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
        intent.putExtra(CancerDetectionResultActivity.EXTRA_SUCCESS, success)
        intent.putExtra(CancerDetectionResultActivity.EXTRA_IS_CANCER, isCancer)
        intent.putExtra(CancerDetectionResultActivity.EXTRA_CONFIDENCE, confidence)
        intent.putExtra(CancerDetectionResultActivity.EXTRA_ERROR, error)
        startActivity(intent)
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

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            showToast(getString(R.string.no_media_selected))
        }
    }

    private fun startCrop(imageUri: Uri) {
        val destinationUri =
            Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))

        val uCrop = UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)

        cropActivityResultLauncher.launch(uCrop.getIntent(this))
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEditButton(show: Boolean) {
        binding.editButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showProgressIndicator(show: Boolean) {
        binding.progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    @Suppress("DEPRECATION")
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}