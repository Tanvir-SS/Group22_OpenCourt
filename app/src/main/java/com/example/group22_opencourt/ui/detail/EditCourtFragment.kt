package com.example.group22_opencourt.ui.detail

import com.example.group22_opencourt.R
import androidx.fragment.app.Fragment
import android.view.ViewGroup
import android.view.View
import android.view.LayoutInflater
import android.os.Bundle
import com.example.group22_opencourt.MainActivity


class EditCourtFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_court, container, false)
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
        //call this in the onSuccess = true of the modifyCourt
        //navController.popBackStack()

    }
}
