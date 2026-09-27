package com.pocketmind.core.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import com.pocketmind.core.model.ActionResult
import com.pocketmind.core.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun launchApp(packageName: String): ActionResult {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionResult(false, "App not installed: $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ActionResult(true, "Opened $packageName")
    }

    fun setAlarm(hour: Int, minute: Int, label: String = ""): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (label.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Alarm set for ${hour.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')} ${if (label.isNotBlank()) "\"$label\"" else ""}")
    }

    fun createCalendarEvent(event: CalendarEvent): ActionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTime)
            event.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            event.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Calendar event created: ${event.title}")
    }

    fun makeCall(number: String): ActionResult {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Calling $number")
    }

    fun navigateTo(destination: String): ActionResult {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult(true, "Navigating to $destination")
        } catch (e: Exception) {
            // Fallback to web
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(destination)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ActionResult(true, "Opening maps for $destination")
        }
    }

    fun takePhoto(): ActionResult {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Camera opened")
    }

    fun searchWeb(query: String): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Searching: $query")
    }

    fun openWifiSettings(): ActionResult {
        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Opened WiFi settings")
    }

    fun openBluetoothSettings(): ActionResult {
        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Opened Bluetooth settings")
    }
}
