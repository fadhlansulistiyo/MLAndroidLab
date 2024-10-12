package com.fadhlansulistiyo.mlandroidlab.tensorflowlite.prediction

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fadhlansulistiyo.mlandroidlab.asclepius.CancerDetectionResultBottomSheetFragment
import com.fadhlansulistiyo.mlandroidlab.databinding.ActivityRicePredictorBinding

class RicePredictorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRicePredictorBinding
    private lateinit var predictionHelper: PredictionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRicePredictorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        predictionHelper = PredictionHelper(
            context = this,
            onResult = { result ->
                moveToResult(result, null)
            },
            onError = { errorMessage ->
                moveToResult(null, errorMessage)
            }
        )

        binding.btnPredict.setOnClickListener {
            val input = binding.edSales.text.toString()
            predictionHelper.predict(input)
        }
    }

    private fun moveToResult(
        result: String?,
        error: String?
    ) {
        if (result != null) {
            val bottomSheetFragment = RicePredictorResultFragment.newInstance(result)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        } else {
            showToast(error ?: "An unknown error occurred")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        predictionHelper.close()
    }
}