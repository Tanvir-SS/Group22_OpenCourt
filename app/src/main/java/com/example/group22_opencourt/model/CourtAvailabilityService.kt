package com.example.group22_opencourt.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.group22_opencourt.R


import android.app.PendingIntent
import androidx.lifecycle.Observer
import com.example.group22_opencourt.MainActivity
import kotlin.or
import kotlin.text.compareTo


class CourtAvailabilityService : Service() {

    // LiveData observing court document
    private var courtLiveData : FirestoreDocumentLiveData<Court?>? = null
    private val observer = Observer<Court?> { court ->
        if (court != null) {
            if (court.base.courtsAvailable > 0) {
                sendAvailableNotification(court.base.name, court.base.id)
                cleanupAndStop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val docId = intent?.getStringExtra(EXTRA_DOCUMENT_ID)
        val collection = intent?.getStringExtra(EXTRA_COLLECTION) ?: "courts"
        val courtName = intent?.getStringExtra(EXTRA_COURT_NAME) ?: "Court"

        if (docId.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // start the service in the foreground
        startForeground(FOREGROUND_ID, buildForegroundNotification(courtName))

        // start listening to court availability
        startListening(collection, docId, courtName)
        return START_STICKY
    }

    private fun startListening(collection: String, docId: String, courtName: String) {
        // observe the court document for changes
        courtLiveData?.removeObserver(observer)
        courtLiveData = CourtRepository.instance.getCourtLiveData(docId)
        courtLiveData?.observeForever(observer)

    }

    private fun sendAvailableNotification(courtName: String, courtId : String) {
        // send a notification that the court is available
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // intent to open the app when the notification is tapped
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOCUMENT_ID, courtId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            flags
        )

        // build and send the notification
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Court Available! 🎉")
            .setContentText("$courtName is Available Now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)
            .build()

        // notify the user
        nm.notify(ALERT_ID, notif)
    }

    private fun buildForegroundNotification(courtName: String): Notification {
        // build the foreground notification with a stop action
        val stopIntent = Intent(this, CourtAvailabilityService::class.java).apply {
            action = ACTION_STOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // create pending intent for stop action
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags)

        // create the notification
        return NotificationCompat.Builder(this, CHANNEL_FOREGROUND)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Watching court availability")
            .setContentText("Monitoring $courtName…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            // build the notification
            .build()
    }

    private fun ensureChannels() {
        // create notification channels if on Android O or higher
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOREGROUND,
                "Availability Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Court Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun cleanupAndStop() {
        // clean up observers and stop the service
        courtLiveData?.removeObserver(observer)
        courtLiveData = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        // clean up observers
        courtLiveData?.removeObserver(observer)
        courtLiveData = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // extras for intent
        const val EXTRA_DOCUMENT_ID = "extra_document_id"
        const val EXTRA_COLLECTION = "extra_collection"
        const val EXTRA_COURT_NAME = "extra_court_name"
        // notification channel and IDs
        private const val CHANNEL_FOREGROUND = "court_foreground"
        private const val CHANNEL_ALERTS = "court_alerts"
        private const val FOREGROUND_ID = 1001
        private const val ALERT_ID = 2001
        // action to stop the service
        private const val ACTION_STOP = "com.example.group22_opencourt.STOP_MONITORING"

    }
}
