package com.example.group22_opencourt.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.group22_opencourt.LoginActivity
import com.example.group22_opencourt.R
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class SettingsFragment : Fragment() {
    private lateinit var emailEditText: EditText

    private lateinit var changeEmailText : TextView

    private lateinit var signOutButton : Button

    private lateinit var changeEmailLayout : LinearLayout

    private lateinit var layoutSettingsEmail : LinearLayout

    private lateinit var cancelButton : Button
    private lateinit var saveButton : Button
    private fun showKeyboard(editText: android.widget.EditText) {
        editText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun initViews(view: View) {
        changeEmailText = view.findViewById(R.id.textChangeEmail)
        emailEditText = view.findViewById(R.id.editTextEmail)
        signOutButton = view.findViewById(R.id.buttonSignOut)
        changeEmailLayout = view.findViewById(R.id.changeEmailLayout)

        // Set focus and show keyboard when parent LinearLayout is clicked
        layoutSettingsEmail = view.findViewById<LinearLayout>(R.id.layoutSettingsEmail)
        cancelButton = view.findViewById(R.id.cancelButton)
        saveButton = view.findViewById(R.id.saveButton)

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
        showSignOut()
        signOutButton.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            Toast.makeText(requireContext(), "Signed Out", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
        changeEmailText.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.providerData.forEach { d ->
                    Log.d("auth", d.providerId)
                }
            }
            Log.d("auth", "${user?.providerData}")
            if (user != null) {
                var passwordOption = false
                user.providerData.forEach { d ->
                    if (d.providerId == "password") {
                        passwordOption = true
                    }
                }
                if (passwordOption) {
                    showChangeEmail()
                } else {
                    Toast.makeText(requireContext(), "Cannot change email from Google sign in", Toast.LENGTH_SHORT).show()
                }
            }
        }
        cancelButton.setOnClickListener {
            showSignOut()
        }
        saveButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            user?.verifyBeforeUpdateEmail(emailEditText.text.toString())?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Verification email sent to ${emailEditText.text.toString()}",
                        Toast.LENGTH_SHORT).show()
                    showSignOut()
                }
            }
        }

    }

    private fun showSignOut() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            emailEditText.setText(user.email)
        }
        emailEditText.isFocusable = false
        emailEditText.isClickable = false
        layoutSettingsEmail.isClickable = false
        emailEditText.isFocusableInTouchMode = false
        layoutSettingsEmail.setOnClickListener(null)
        layoutSettingsEmail.isFocusableInTouchMode = false
        changeEmailLayout.visibility = View.GONE
        changeEmailText.visibility = View.VISIBLE
        signOutButton.visibility = View.VISIBLE

    }

    private fun showChangeEmail() {
        emailEditText.setText("")
        emailEditText.isFocusable = true
        emailEditText.isClickable = true
        emailEditText.isFocusableInTouchMode = true
        layoutSettingsEmail.isFocusableInTouchMode =true
        layoutSettingsEmail.isClickable = true
        layoutSettingsEmail.setOnClickListener {
            showKeyboard(emailEditText)
        }
        changeEmailLayout.visibility = View.VISIBLE
        changeEmailText.visibility = View.GONE
        signOutButton.visibility = View.GONE
    }
}