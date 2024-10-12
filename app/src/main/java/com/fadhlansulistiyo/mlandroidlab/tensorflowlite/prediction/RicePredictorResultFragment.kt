package com.fadhlansulistiyo.mlandroidlab.tensorflowlite.prediction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fadhlansulistiyo.mlandroidlab.databinding.FragmentRicePredictorResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RicePredictorResultFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentRicePredictorResultBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRicePredictorResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val result = arguments?.getString(ARG_RESULT) ?: ""

        binding.predictResult.text = result

        binding.closeIcon.setOnClickListener {
            dismiss() // Dismiss the bottom sheet
        }
    }

    companion object {
        private const val ARG_RESULT = "result"

        fun newInstance(result: String): RicePredictorResultFragment {
            val fragment = RicePredictorResultFragment()
            val args = Bundle().apply {
                putString(ARG_RESULT, result)
            }
            fragment.arguments = args
            return fragment
        }
    }
}