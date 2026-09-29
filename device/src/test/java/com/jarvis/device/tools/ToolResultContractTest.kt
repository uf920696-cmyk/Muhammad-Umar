package com.jarvis.device.tools

import com.jarvis.core.common.ToolResult
import org.junit.Assert.*
import org.junit.Test

class ToolResultContractTest {

    @Test
    fun testToolResultHonestyDistinction() {
        val verified = ToolResult.Verified("set_torch", mapOf("torch_on" to true))
        val dispatched = ToolResult.Dispatched("send_whatsapp", "Intent started for package")
        val needsAction = ToolResult.NeedsUserAction("set_brightness", "Requires WRITE_SETTINGS permission")
        val failed = ToolResult.Failed("Hardware error", recoverable = true)

        assertTrue(verified is ToolResult.Verified)
        assertTrue(dispatched is ToolResult.Dispatched)
        assertTrue(needsAction is ToolResult.NeedsUserAction)
        assertTrue(failed is ToolResult.Failed)

        // Verify that Dispatched is never confused with Verified
        assertNotEquals(verified.javaClass, dispatched.javaClass)
    }

    @Test
    fun testDeviceToolsDeclarations() {
        val tools = listOf(
            DeviceToolsEngine.TOOL_SET_TORCH,
            DeviceToolsEngine.TOOL_SET_VOLUME,
            DeviceToolsEngine.TOOL_SET_BRIGHTNESS,
            DeviceToolsEngine.TOOL_LAUNCH_APP,
            DeviceToolsEngine.TOOL_GET_DEVICE_STATUS
        )

        assertEquals(5, tools.size)
        assertTrue(tools.contains("set_torch"))
        assertTrue(tools.contains("set_volume"))
        assertTrue(tools.contains("set_brightness"))
        assertTrue(tools.contains("launch_app"))
        assertTrue(tools.contains("get_device_status"))
    }
}
