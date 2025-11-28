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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore

import android.app.PendingIntent



class CourtAvailabilityService : Service() {

    private var registration: ListenerRegistration? = null

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

        startForeground(FOREGROUND_ID, buildForegroundNotification(courtName))


        startListening(collection, docId, courtName)
        return START_STICKY

        if (intent?.action == ACTION_STOP) {
            cleanupAndStop()
            return START_NOT_STICKY
        }
    }

    private fun startListening(collection: String, docId: String, courtName: String) {
        registration?.remove()

        val docRef = FirebaseFirestore.getInstance().collection(collection).document(docId)

        registration = docRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener

            // Adjust this field name to match your Firestore schema:
            // in your detail UI you used court.base.courtsAvailable == 0 / > 0
            val courtsAvailable = snap.getLong("courtsAvailable")?.toInt() ?: 0

            if (courtsAvailable > 0) {
                sendAvailableNotification(courtName)
                cleanupAndStop()
            }
        }
    }

    private fun sendAvailableNotification(courtName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Court available 🎉")
            .setContentText("$courtName has availability now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(ALERT_ID, notif)
    }

    private fun buildForegroundNotification(courtName: String): Notification {
        val stopIntent = Intent(this, CourtAvailabilityService::class.java).apply {
            action = ACTION_STOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags)

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
            .build()
    }

    private fun ensureChannels() {
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
        registration?.remove()
        registration = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        registration?.remove()
        registration = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_DOCUMENT_ID = "extra_document_id"
        const val EXTRA_COLLECTION = "extra_collection"
        const val EXTRA_COURT_NAME = "extra_court_name"

        private const val CHANNEL_FOREGROUND = "court_foreground"
        private const val CHANNEL_ALERTS = "court_alerts"
        private const val FOREGROUND_ID = 1001
        private const val ALERT_ID = 2001

        private const val ACTION_STOP = "com.example.group22_opencourt.STOP_MONITORING"

    }
}
