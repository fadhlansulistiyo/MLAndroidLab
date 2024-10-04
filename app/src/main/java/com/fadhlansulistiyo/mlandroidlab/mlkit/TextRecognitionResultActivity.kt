package com.fadhlansulistiyo.mlandroidlab.mlkit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fadhlansulistiyo.mlandroidlab.R
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityTextRecognitionResultBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TextRecognitionResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextRecognitionResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTextRecognitionResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        val detectedText = intent.getStringExtra(EXTRA_RESULT)
        binding.tvResult.text = detectedText

        translateText(detectedText)
        setupAction()
    }

    private fun translateText(detectedText: String?) {
        binding.progressBar.visibility = View.VISIBLE

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.INDONESIAN)
            .build()
        val indonesianEnglishTranslator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        indonesianEnglishTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                indonesianEnglishTranslator.translate(detectedText.toString())
                    .addOnSuccessListener { translatedText ->
                        binding.tvTranslate.text = translatedText
                        indonesianEnglishTranslator.close()
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener { exception ->
                        showToast(exception.message.toString())
                        print(exception.stackTrace)
                        indonesianEnglishTranslator.close()
                        binding.progressBar.visibility = View.GONE
                    }
            }
            .addOnFailureListener { _ ->
                showToast(getString(R.string.downloading_model_fail))
                binding.progressBar.visibility = View.GONE
            }
        lifecycle.addObserver(indonesianEnglishTranslator)
    }

    private fun setupAction() {
        binding.ivResultCopy.setOnClickListener {
            copyToClipboard(this@TextRecognitionResultActivity, binding.tvResult.text.toString())
        }
        binding.ivTranslateCopy.setOnClickListener {
            copyToClipboard(this@TextRecognitionResultActivity, binding.tvTranslate.text.toString())
        }
    }

    private fun copyToClipboard(context: Context, textToCopy: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", textToCopy)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, getString(R.string.text_copied_to_clipboard), Toast.LENGTH_SHORT)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = ""
        }
    }

    @Suppress("DEPRECATION")
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_RESULT = "extra_result"
    }
}