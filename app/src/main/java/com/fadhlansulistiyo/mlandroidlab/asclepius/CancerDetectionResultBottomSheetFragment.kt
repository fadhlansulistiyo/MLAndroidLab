package com.fadhlansulistiyo.mlandroidlab.asclepius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fadhlansulistiyo.mlandroidlab.R
import com.fadhlansulistiyo.mlandroidlab.databinding.FragmentCancerDetectionResultBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CancerDetectionResultBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCancerDetectionResultBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCancerDetectionResultBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isCancer = arguments?.getBoolean(ARG_IS_CANCER) ?: false
        val confidence = arguments?.getFloat(ARG_CONFIDENCE) ?: 0.0f

        binding.labelResult.text = if (isCancer) "Cancer Detected" else "No Cancer"
        binding.confidenceResult.text = "Confidence: ${String.format("%.2f", confidence * 100)}%"

        if (isCancer) {
            binding.resultImage.setImageResource(R.drawable.cancer_detected)
        } else {
            binding.resultImage.setImageResource(R.drawable.healthy)
        }

        binding.closeIcon.setOnClickListener {
            dismiss() // Dismiss the bottom sheet
        }
    }

    companion object {
        private const val ARG_IS_CANCER = "is_cancer"
        private const val ARG_CONFIDENCE = "confidence"

        fun newInstance(isCancer: Boolean, confidence: Float): CancerDetectionResultBottomSheetFragment {
            val fragment = CancerDetectionResultBottomSheetFragment()
            val args = Bundle().apply {
                putBoolean(ARG_IS_CANCER, isCancer)
                putFloat(ARG_CONFIDENCE, confidence)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
