package com.jarvis.device.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import com.jarvis.core.common.ToolResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TORCH CONTROLLER:
 * Real hardware control using CameraManager.setTorchMode().
 * Uses TorchCallback read-back to verify true hardware state.
 * Never claims Verified without actual hardware confirmation.
 */
class TorchController(private val context: Context) {

    companion object {
        private const val TAG = "TorchController"
        private const val READ_BACK_TIMEOUT_MS = 1500L
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val isTorchOn = AtomicBoolean(false)
    private var cameraIdWithFlash: String? = null

    init {
        try {
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraIdWithFlash = id
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding camera with flash: ${e.message}")
        }
    }

    fun isTorchOn(): Boolean = isTorchOn.get()

    /**
     * Toggles flashlight and awaits read-back confirmation from CameraManager.TorchCallback.
     */
    suspend fun setTorch(enabled: Boolean): ToolResult {
        val cm = cameraManager ?: return ToolResult.Unsupported("CameraManager is not available on this device")
        val cid = cameraIdWithFlash ?: return ToolResult.Unsupported("No rear camera with flash found on this device")

        val confirmationDeferred = CompletableDeferred<Boolean>()

        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, mode: Boolean) {
                if (cameraId == cid && mode == enabled) {
                    isTorchOn.set(mode)
                    confirmationDeferred.complete(true)
                }
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                if (cameraId == cid) {
                    confirmationDeferred.complete(false)
                }
            }
        }

        return try {
            cm.registerTorchCallback(callback, null)
            cm.setTorchMode(cid, enabled)

            val confirmed = withTimeoutOrNull(READ_BACK_TIMEOUT_MS) {
                confirmationDeferred.await()
            }

            cm.unregisterTorchCallback(callback)

            if (confirmed == true) {
                ToolResult.Verified(
                    action = "set_torch",
                    verifiedState = mapOf("torch_on" to enabled),
                    detail = if (enabled) "Flashlight turned ON (confirmed by CameraManager)" else "Flashlight turned OFF (confirmed by CameraManager)"
                )
            } else if (confirmed == false) {
                ToolResult.Failed("Torch mode became unavailable (possibly in use by another app or battery low)")
            } else {
                // Timeout on read-back
                ToolResult.Dispatched(
                    action = "set_torch",
                    detail = "Flashlight command dispatched, but read-back timed out after ${READ_BACK_TIMEOUT_MS}ms"
                )
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException: ${e.message}")
            ToolResult.Failed("Camera access exception: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected torch error: ${e.message}")
            ToolResult.Failed("Unexpected torch error: ${e.message}")
        }
    }
}
