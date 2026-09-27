package com.pocketmind.core.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pocketmind.core.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private const val MAX_BUFFER_SIZE = 50

        @Volatile
        var instance: NotificationListener? = null
            private set

        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

        fun isEnabled(): Boolean = instance != null

        fun clearAll() {
            _notifications.value = emptyList()
        }
    }

    // Apps to monitor
    private val monitoredApps = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.google.android.gm",
        "com.instagram.android",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.twitter.android",
        "com.linkedin.android"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        instance = null
        Log.i(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!shouldCapture(sbn)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getString("android.title") ?: return
        val text = extras.getString("android.text") ?: extras.getString("android.bigText") ?: ""
        val appName = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(sbn.packageName, 0)
        ).toString()

        val item = NotificationItem(
            id = sbn.id,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        Log.d(TAG, "Captured notification: $appName — $title")
        _notifications.update { current ->
            val updated = current.toMutableList()
            updated.add(0, item) // newest first
            if (updated.size > MAX_BUFFER_SIZE) {
                updated.subList(MAX_BUFFER_SIZE, updated.size).clear()
            }
            updated
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        _notifications.update { current ->
            current.filter { it.id != sbn.id || it.packageName != sbn.packageName }
        }
    }

    private fun shouldCapture(sbn: StatusBarNotification): Boolean {
        // Only capture from monitored apps, skip system apps
        if (!sbn.isClearable) return false // ongoing/system notifications
        // Optionally filter only monitored apps
        // return sbn.packageName in monitoredApps
        return true // capture all clearable notifications
    }

    /**
     * Reply to a notification via RemoteInput (for WhatsApp, etc.)
     */
    fun replyToNotification(sbn: StatusBarNotification, replyText: String): Boolean {
        val notification = sbn.notification ?: return false
        for (action in notification.actions ?: return false) {
            val remoteInputs = action.remoteInputs ?: continue
            if (remoteInputs.isNotEmpty()) {
                try {
                    val intent = android.content.Intent()
                    val bundle = android.os.Bundle()
                    for (ri in remoteInputs) {
                        bundle.putCharSequence(ri.resultKey, replyText)
                    }
                    android.app.RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                    action.actionIntent.send(applicationContext, 0, intent)
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Reply failed", e)
                }
            }
        }
        return false
    }
}
