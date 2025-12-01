package com.example.group22_opencourt

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class OpenCourtApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (needed before any Firebase service is used)
        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG) {
            // Use debug provider for emulator / development
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // Use Play Integrity provider in production
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )

        }

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.getAppCheckToken(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result?.token
                Log.d("APP_CHECK", "Token is: $token")
            } else {
                Log.e("APP_CHECK", "Could not retrieve App Check token", task.exception)
            } }


        Log.d("APP_CHECK", "APP CHECK INSTALLED")
    }
}
