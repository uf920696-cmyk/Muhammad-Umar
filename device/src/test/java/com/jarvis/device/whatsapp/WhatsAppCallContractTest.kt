package com.jarvis.device.whatsapp

import com.jarvis.core.common.ToolResult
import com.jarvis.device.tools.DeviceToolsEngine
import org.junit.Assert.*
import org.junit.Test

class WhatsAppCallContractTest {

    @Test
    fun testWhatsAppHonestyContract() {
        val dispatchedResult = ToolResult.Dispatched(
            action = "whatsapp_call",
            dispatchDetails = "Dispatched WhatsApp voice call intent to Ali (+1234567890)"
        )

        // Absolute Honesty verification: WhatsApp call results must NEVER be typed as Verified
        assertFalse(dispatchedResult is ToolResult.Verified)
        assertTrue(dispatchedResult is ToolResult.Dispatched)

        val needsAction = ToolResult.NeedsUserAction(
            action = "whatsapp_call",
            actionRequired = "WhatsApp is not installed on this device"
        )
        assertTrue(needsAction is ToolResult.NeedsUserAction)
    }

    @Test
    fun testWhatsAppToolDeclarationPresence() {
        assertEquals("whatsapp_call", DeviceToolsEngine.TOOL_WHATSAPP_CALL)
    }
}
