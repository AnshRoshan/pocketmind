package com.pocketmind.core.action

import android.content.Context
import com.pocketmind.core.model.ActionResult
import com.pocketmind.core.model.PhoneAction
import com.pocketmind.core.model.UIAction
import com.pocketmind.core.service.AccessibilityBridge
import com.pocketmind.core.service.NotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentExecutor: IntentExecutor
) {

    suspend fun execute(action: PhoneAction): ActionResult {
        return try {
            when (action) {
                is PhoneAction.OpenApp ->
                    intentExecutor.launchApp(action.packageName)

                is PhoneAction.SetAlarm ->
                    intentExecutor.setAlarm(action.hour, action.minute, action.label)

                is PhoneAction.CreateCalendarEvent ->
                    intentExecutor.createCalendarEvent(action.event)

                is PhoneAction.MakeCall ->
                    intentExecutor.makeCall(action.number)

                is PhoneAction.NavigateTo ->
                    intentExecutor.navigateTo(action.destination)

                is PhoneAction.TakePhoto ->
                    intentExecutor.takePhoto()

                is PhoneAction.SearchWeb ->
                    intentExecutor.searchWeb(action.query)

                is PhoneAction.ToggleWifi ->
                    intentExecutor.openWifiSettings()

                is PhoneAction.ToggleBluetooth ->
                    intentExecutor.openBluetoothSettings()

                is PhoneAction.ReadNotifications ->
                    readNotifications(action.appFilter)

                is PhoneAction.UIInteraction ->
                    performUIInteraction(action)

                is PhoneAction.ReadFile ->
                    readFile(action.path)

                is PhoneAction.WriteFile ->
                    writeFile(action.path, action.content)

                is PhoneAction.Chat ->
                    ActionResult(success = true, output = "")

                else ->
                    ActionResult(success = false, output = "Action not yet implemented", error = "Not implemented")
            }
        } catch (e: Exception) {
            ActionResult(success = false, output = "Error executing action", error = e.message)
        }
    }

    private fun readNotifications(filter: String?): ActionResult {
        val notifications = NotificationListener.notifications.value
        val filtered = if (filter != null) {
            notifications.filter { it.packageName.contains(filter) || it.appName.contains(filter) }
        } else notifications

        return if (filtered.isEmpty()) {
            ActionResult(success = true, output = "No notifications found")
        } else {
            val summary = filtered.take(10).joinToString("\n") { notif ->
                "📱 ${notif.appName}: ${notif.title} — ${notif.text.take(80)}"
            }
            ActionResult(success = true, output = summary)
        }
    }

    private suspend fun performUIInteraction(action: PhoneAction.UIInteraction): ActionResult {
        val bridge = AccessibilityBridge.instance
            ?: return ActionResult(
                success = false,
                output = "Accessibility service not enabled",
                error = "Enable Pocketmind in Accessibility Settings"
            )

        var successCount = 0
        for (step in action.steps) {
            val stepSuccess = when (step.action) {
                UIAction.CLICK -> bridge.clickNodeWithText(step.target)
                UIAction.TYPE -> {
                    if (step.value != null) {
                        bridge.findAndType(step.target, step.value)
                    } else false
                }
                UIAction.SCROLL_DOWN -> bridge.scrollDown()
                UIAction.SCROLL_UP -> bridge.scrollUp()
                UIAction.BACK -> bridge.pressBack()
                UIAction.HOME -> bridge.pressHome()
                UIAction.LONG_PRESS -> bridge.clickNodeWithText(step.target) // fallback
                UIAction.SWIPE_LEFT -> bridge.swipe(900f, 500f, 100f, 500f)
                UIAction.SWIPE_RIGHT -> bridge.swipe(100f, 500f, 900f, 500f)
            }
            if (stepSuccess) successCount++
            delay(500) // Wait between steps
        }

        return ActionResult(
            success = successCount > 0,
            output = "Completed $successCount/${action.steps.size} steps"
        )
    }

    private fun readFile(path: String): ActionResult {
        return try {
            val content = java.io.File(path).readText()
            ActionResult(success = true, output = content.take(2000))
        } catch (e: Exception) {
            ActionResult(success = false, output = "Cannot read file", error = e.message)
        }
    }

    private fun writeFile(path: String, content: String): ActionResult {
        return try {
            java.io.File(path).writeText(content)
            ActionResult(success = true, output = "File written to $path")
        } catch (e: Exception) {
            ActionResult(success = false, output = "Cannot write file", error = e.message)
        }
    }
}
