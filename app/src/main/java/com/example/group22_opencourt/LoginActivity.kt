package com.example.group22_opencourt

import android.content.Intent
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.group22_opencourt.databinding.ActivityLoginBinding

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        credentialManager = CredentialManager.create(baseContext)

        auth.signOut()

        // Default to login screen visible
        showLoginForm()

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            loginWithEmail(email, password)
        }

        binding.buttonLoginGmail.setOnClickListener {
            launchCredentialManager()
        }

        // Small "Sign up" under login
        binding.textSignUp.setOnClickListener {
            showSignupForm()
        }

        // Signup screen: buttons
        binding.buttonSignupSubmit.setOnClickListener {
            val email = binding.editTextSignupEmail.text.toString()
            val password = binding.editTextSignupPassword.text.toString()
            val confirm = binding.editTextSignupConfirmPassword.text.toString()
            signupWithEmail(email, password, confirm)
        }

        // New bottom text on signup screen
        binding.textAlreadyHaveAccountLogin.setOnClickListener {
            showLoginForm()
        }
    }

    private fun showLoginForm() {
        val transition = AutoTransition()
        transition.duration = 250
        TransitionManager.beginDelayedTransition(binding.root, transition)
        binding.layoutLoginRoot.visibility = View.VISIBLE
        binding.layoutSignupRoot.visibility = View.GONE
    }

    private fun showSignupForm() {
        val transition = AutoTransition()
        transition.duration = 250
        TransitionManager.beginDelayedTransition(binding.root, transition)
        binding.layoutLoginRoot.visibility = View.GONE
        binding.layoutSignupRoot.visibility = View.VISIBLE
    }

    private fun loginWithEmail(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter Email and Password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signupWithEmail(email: String, password: String, confirmPassword: String) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Fill in All Fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Sign up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMain () {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun launchCredentialManager() {
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId(getString(R.string.default_web_client_id))
            // Only show accounts previously used to sign in.
            .setFilterByAuthorizedAccounts(false)
            .build()

        // Create the Credential Manager request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        // [END create_credential_manager_request]

        lifecycleScope.launch {
            try {
                // Launch Credential Manager UI
                val result = credentialManager.getCredential(
                    context = baseContext,
                    request = request
                )

                // Extract credential from the result returned by Credential Manager
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Toast.makeText(this@LoginActivity, "Couldn't retrieve user's credentials," +
                        "make sure google play store is enabled", Toast.LENGTH_SHORT).show()
                Log.e("debug", "Couldn't retrieve user's credentials: ${e.localizedMessage}")
            }
        }
    }
    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w("debug", "Credential is not of type Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("debug", "signInWithCredential:success")
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    // If sign in fails, display a message to the user
                    Log.w("debug", "signInWithCredential:failure", task.exception)
                }
            }
    }

    //with email authenication
    //        auth.createUserWithEmailAndPassword(email, password)
    //            .addOnCompleteListener(this) { task ->
    //                if (task.isSuccessful) {
    //                    // Optionally log in automatically and navigate
    //                    val user = auth.currentUser
    //                    user?.sendEmailVerification()
    //                        ?.addOnCompleteListener { task ->
    //                            if (task.isSuccessful) {
    //                                Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
    //                                showLoginForm()
    //                            } else {
    //                                Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show()
    //                            }
    //                        }
    //                } else {
    //                    Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
    //                }
    //            }

    //        auth.signInWithEmailAndPassword(email, password)
    //            .addOnCompleteListener(this) { task ->
    //                if (task.isSuccessful) {
    //                    val user = auth.currentUser
    //                    if (user != null && user.isEmailVerified) {
    //                        navigateToMain() // Email verified → allow access
    //                    } else {
    //                        Toast.makeText(this, "Please verify your email", Toast.LENGTH_SHORT).show()
    //                        auth.signOut() // optional: prevent unverified login
    //                    }
    //                } else {
    //                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
    //                }
    //            }
}
