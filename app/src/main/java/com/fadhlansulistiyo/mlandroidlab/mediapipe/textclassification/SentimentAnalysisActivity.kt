package com.fadhlansulistiyo.mlandroidlab.mediapipe.textclassification

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fadhlansulistiyo.mlandroidlab.R
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivitySentimentAnalysisBinding
import com.google.mediapipe.tasks.components.containers.Classifications
import java.text.NumberFormat

class SentimentAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySentimentAnalysisBinding
    private lateinit var textClassifierHelper: TextClassifierHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySentimentAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTextClassifierHelper()
        setupButtonClickListener()
    }

    private fun setupTextClassifierHelper() {
        textClassifierHelper = TextClassifierHelper(
            context = this,
            classifierListener = object : TextClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    showError(error)
                }

                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    displayResults(results)
                }
            }
        )
    }

    private fun setupButtonClickListener() {
        binding.btnClassify.setOnClickListener {
            val inputText = binding.edInput.text.toString().trim()
            if (inputText.isNotEmpty()) {
                textClassifierHelper.classify(inputText)
            } else {
                showError(getString(R.string.error_empty_input))
            }
        }
    }

    private fun displayResults(results: List<Classifications>?) {
        runOnUiThread {
            results?.let {
                if (it.isNotEmpty() && it[0].categories().isNotEmpty()) {
                    val sortedCategories = it[0].categories().sortedByDescending { category -> category?.score() }

                    val displayResult = sortedCategories.joinToString("\n") { category ->
                        "${category.categoryName()} " + NumberFormat.getPercentInstance()
                            .format(category.score()).trim()
                    }
                    binding.tvResult.text = displayResult
                } else {
                    binding.tvResult.text = getString(R.string.no_classification_results)
                }
            } ?: run {
                binding.tvResult.text = getString(R.string.no_classification_results)
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        textClassifierHelper.shutdownExecutor() // Final cleanup
    }
}
