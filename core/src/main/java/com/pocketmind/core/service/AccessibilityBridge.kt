package com.pocketmind.core.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AccessibilityBridge : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityBridge"
        @Volatile
        var instance: AccessibilityBridge? = null
            private set

        fun isEnabled(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events handled on-demand, not streaming
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Click on the first node whose text or content-description matches [text].
     */
    fun clickNodeWithText(text: String): Boolean {
        val node = findNodeByText(rootInActiveWindow, text) ?: run {
            Log.w(TAG, "Node not found: $text")
            return false
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK).also {
            node.recycle()
        }
    }

    /**
     * Type [text] into the currently focused text field.
     */
    fun typeText(text: String): Boolean {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: run {
            Log.w(TAG, "No focused input found")
            return false
        }
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args).also {
            focusedNode.recycle()
        }
    }

    /**
     * Find a node by text and type into it.
     */
    fun findAndType(targetText: String, inputText: String): Boolean {
        val node = findNodeByText(rootInActiveWindow, targetText) ?: run {
            Log.w(TAG, "Could not find input field: $targetText")
            return false
        }
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, inputText)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args).also {
            node.recycle()
        }
    }

    /**
     * Press the global back button.
     */
    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    /**
     * Press the home button.
     */
    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    /**
     * Scroll down (forward) in the focused scrollable view.
     */
    fun scrollDown(): Boolean {
        val scrollable = findScrollableNode(rootInActiveWindow)
        return scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)?.also {
            scrollable.recycle()
        } ?: false
    }

    /**
     * Scroll up (backward) in the focused scrollable view.
     */
    fun scrollUp(): Boolean {
        val scrollable = findScrollableNode(rootInActiveWindow)
        return scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)?.also {
            scrollable.recycle()
        } ?: false
    }

    /**
     * Perform a swipe gesture from (x1,y1) to (x2,y2).
     */
    suspend fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300): Boolean =
        suspendCancellableCoroutine { cont ->
            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription) {
                    cont.resume(false)
                }
            }, null)
        }

    /**
     * Dump all visible node texts for debugging.
     */
    fun dumpScreenText(): List<String> {
        val texts = mutableListOf<String>()
        collectNodeTexts(rootInActiveWindow, texts)
        return texts
    }

    /**
     * Get screen content as a structured string for SLM context.
     */
    fun getScreenContext(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        buildNodeContext(root, sb, depth = 0)
        return sb.toString()
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun findNodeByText(root: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        root ?: return null

        // Try exact content description
        val byDesc = root.findAccessibilityNodeInfosByText(text)
        if (byDesc.isNotEmpty()) return byDesc.first()

        // Try partial match
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            if (nodeText.contains(text, ignoreCase = true)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        root ?: return null
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val found = findScrollableNode(root.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, texts: MutableList<String>) {
        node ?: return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        for (i in 0 until node.childCount) {
            collectNodeTexts(node.getChild(i), texts)
        }
    }

    private fun buildNodeContext(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        node ?: return
        val indent = "  ".repeat(depth)
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val role = node.className?.split(".")?.lastOrNull() ?: ""
        if (!text.isNullOrBlank() || !desc.isNullOrBlank()) {
            sb.appendLine("$indent[$role] ${text ?: desc}")
        }
        for (i in 0 until node.childCount) {
            buildNodeContext(node.getChild(i), sb, depth + 1)
        }
    }
}
