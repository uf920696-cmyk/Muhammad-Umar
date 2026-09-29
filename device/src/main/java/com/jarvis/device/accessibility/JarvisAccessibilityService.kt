package com.jarvis.device.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 100% REAL SCREEN AUTOMATION & CLICK CONTROLLER:
 * Allows JARVIS to read what is on the screen and tap/click elements:
 * e.g. "Dusre number par jo song hai usse play karo", "Next video", "Back", "Scroll down"
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAccessService"
        var instance: JarvisAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Jarvis Accessibility Service Connected and Ready!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event stream
    }

    override fun onInterrupt() {
        Log.w(TAG, "Jarvis Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Finds and clicks item by position (e.g. 1st, 2nd, 3rd result on screen).
     * Perfect for: "Dusre number wala song chalao", "Pehla song play karo".
     */
    fun clickItemByPosition(positionIndex: Int): Boolean {
        val root = rootInActiveWindow ?: return false
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        collectClickableNodes(root, clickableNodes)

        if (clickableNodes.isEmpty()) {
            // Also collect list items / child containers if no direct clickable
            collectGeneralItemNodes(root, clickableNodes)
        }

        val targetIndex = (positionIndex - 1).coerceAtLeast(0)
        if (targetIndex < clickableNodes.size) {
            val target = clickableNodes[targetIndex]
            val success = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!success) {
                // Try parent click
                target.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            Log.i(TAG, "Clicked position #$positionIndex: success=$success")
            return true
        }

        // Fallback: Perform screen tap coordinates
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val tapY = (screenHeight * (0.25f + targetIndex * 0.15f)).coerceAtMost(screenHeight * 0.85f)
        return performTap(screenWidth * 0.5f, tapY)
    }

    /**
     * Clicks any text on screen (e.g. "Play", "Subscribe", video title)
     */
    fun clickTextOnScreen(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val clean = text.lowercase().trim()
        val nodes = root.findAccessibilityNodeInfosByText(clean)
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else if (node.parent?.isClickable == true) {
                return node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    /**
     * Performs swipe / scroll down
     */
    fun scroll(down: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return root.performAction(action)
    }

    /**
     * Hardware navigation back button
     */
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Hardware Home button
     */
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun collectClickableNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isClickable && node.isVisibleToUser) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.height() > 30 && rect.width() > 30) {
                list.add(node)
            }
        }
        for (i in 0 until node.childCount) {
            collectClickableNodes(node.getChild(i), list)
        }
    }

    private fun collectGeneralItemNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (node.isVisibleToUser && rect.height() > 80 && rect.width() > 200) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectGeneralItemNodes(node.getChild(i), list)
        }
    }

    private fun performTap(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }
}
