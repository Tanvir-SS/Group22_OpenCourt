package com.example.group22_opencourt.ui.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.group22_opencourt.R




class AddCourtFragment : Fragment() {
    private lateinit var courtNameEditText: android.widget.EditText
    private lateinit var clearCourtName: android.widget.ImageView
    private lateinit var courtTypeEditText: android.widget.EditText
    private lateinit var clearCourtType: android.widget.ImageView
    private lateinit var addressEditText: android.widget.EditText
    private lateinit var clearAddress: android.widget.ImageView
    private lateinit var numCourtsEditText: android.widget.EditText
    private lateinit var clearNumCourts: android.widget.ImageView

    private fun showKeyboard(editText: android.widget.EditText) {
        editText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initViews(view: View) {
        courtNameEditText = view.findViewById(R.id.editTextCourtName)
        clearCourtName = view.findViewById(R.id.clearCourtName)
        courtTypeEditText = view.findViewById(R.id.editTextCourtType)
        clearCourtType = view.findViewById(R.id.clearCourtType)
        addressEditText = view.findViewById(R.id.editTextAddress)
        clearAddress = view.findViewById(R.id.clearAddress)
        numCourtsEditText = view.findViewById(R.id.editTextNumCourts)
        clearNumCourts = view.findViewById(R.id.clearNumCourts)

        // Set focus and show keyboard when parent LinearLayout is clicked
        view.findViewById<View>(R.id.layoutCourtName).setOnClickListener {
            showKeyboard(courtNameEditText)
        }
        view.findViewById<View>(R.id.layoutCourtType).setOnClickListener {
            showKeyboard(courtTypeEditText)
        }
        view.findViewById<View>(R.id.layoutAddress).setOnClickListener {
            showKeyboard(addressEditText)
        }
        view.findViewById<View>(R.id.layoutNumCourts).setOnClickListener {
            showKeyboard(numCourtsEditText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_court, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        clearCourtName.setOnClickListener { courtNameEditText.text.clear() }
        clearCourtType.setOnClickListener { courtTypeEditText.text.clear() }
        clearAddress.setOnClickListener { addressEditText.text.clear() }
        clearNumCourts.setOnClickListener { numCourtsEditText.text.clear() }
    }

}