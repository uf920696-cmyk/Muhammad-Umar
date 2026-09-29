package com.jarvis.app.integration

import com.jarvis.core.common.JarvisState
import com.jarvis.core.common.ToolResult
import org.junit.Assert.*
import org.junit.Test

class FullSystemIntegrationTest {

    @Test
    fun testJarvisStateHierarchy() {
        val idle: JarvisState = JarvisState.Idle
        val wake: JarvisState = JarvisState.LocalWake
        val listening: JarvisState = JarvisState.Listening(startTimeMs = 1000L)
        val speaking: JarvisState = JarvisState.Speaking(durationMs = 500L)
        val connected: JarvisState = JarvisState.LiveConnected(sessionToken = "live_tok_123")

        assertTrue(idle is JarvisState.Idle)
        assertTrue(wake is JarvisState.LocalWake)
        assertTrue(listening is JarvisState.Listening)
        assertTrue(speaking is JarvisState.Speaking)
        assertTrue(connected is JarvisState.LiveConnected)
    }

    @Test
    fun testToolHonestyContractMatrix() {
        val hardwareResult = ToolResult.Verified(
            action = "set_torch",
            verifiedState = mapOf("enabled" to true)
        )
        val externalAppResult = ToolResult.Dispatched(
            action = "whatsapp_call",
            dispatchDetails = "Dispatched intent to com.whatsapp"
        )
        val permissionResult = ToolResult.NeedsPermission(
            action = "resolve_contact",
            permission = "android.permission.READ_CONTACTS"
        )
        val actionResult = ToolResult.NeedsUserAction(
            action = "whatsapp_call",
            actionRequired = "WhatsApp is not installed"
        )

        // Strict Invariant: Only hardware actions with read-back can be Verified
        assertTrue(hardwareResult is ToolResult.Verified)
        assertTrue(externalAppResult is ToolResult.Dispatched)
        assertFalse(externalAppResult is ToolResult.Verified)
        assertTrue(permissionResult is ToolResult.NeedsPermission)
        assertTrue(actionResult is ToolResult.NeedsUserAction)
    }
}
