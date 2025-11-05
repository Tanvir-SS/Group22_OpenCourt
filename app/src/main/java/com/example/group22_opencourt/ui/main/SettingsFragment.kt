package com.example.group22_opencourt.ui.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.group22_opencourt.R

class SettingsFragment : Fragment() {
    private lateinit var usernameEditText: android.widget.EditText
    private lateinit var clearUsername: android.widget.ImageView
    private lateinit var favLocationEditText: android.widget.EditText
    private lateinit var clearFavLocation: android.widget.ImageView
    private lateinit var otherSettingsEditText: android.widget.EditText
    private lateinit var clearOtherSettings: android.widget.ImageView

    private fun showKeyboard(editText: android.widget.EditText) {
        editText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initViews(view: View) {
        usernameEditText = view.findViewById(R.id.editTextUsername)
        clearUsername = view.findViewById(R.id.clearUsername)
        favLocationEditText = view.findViewById(R.id.editTextFavLocation)
        clearFavLocation = view.findViewById(R.id.clearFavLocation)
        otherSettingsEditText = view.findViewById(R.id.editTextOtherSettings)
        clearOtherSettings = view.findViewById(R.id.clearOtherSettings)

        // Set focus and show keyboard when parent LinearLayout is clicked
        view.findViewById<View>(R.id.layoutSettingsUserName).setOnClickListener {
            showKeyboard(usernameEditText)
        }
        view.findViewById<View>(R.id.layoutSettingsFavLocation).setOnClickListener {
            showKeyboard(favLocationEditText)
        }
        view.findViewById<View>(R.id.layoutSettingsOther).setOnClickListener {
            showKeyboard(otherSettingsEditText)
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
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        clearUsername.setOnClickListener { usernameEditText.text.clear() }
        clearFavLocation.setOnClickListener { favLocationEditText.text.clear() }
        clearOtherSettings.setOnClickListener { otherSettingsEditText.text.clear() }
    }
}