package com.example.group22_opencourt.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R

class CheckInFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_check_in, container, false)
        // Get document ID from arguments
        val documentId = arguments?.getString("document_id") ?: "1EYahQs7n7ZUF6qPPAjT"

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.showBackButton()
        }
    }
}