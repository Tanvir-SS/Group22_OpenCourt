package com.example.group22_opencourt.ui.main

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.group22_opencourt.R

class SimpleTextFragment : Fragment(R.layout.fragment_simple_text) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val label = arguments?.getString(ARG_LABEL).orEmpty()
        view.findViewById<TextView>(R.id.textView).text = label
    }

    companion object {
        private const val ARG_LABEL = "label"
        fun newInstance(label: String) = SimpleTextFragment().apply {
            arguments = Bundle().apply { putString(ARG_LABEL, label) }
        }
    }
}
