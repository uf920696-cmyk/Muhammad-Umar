package com.jarvis.voice.debug

import android.content.Context
import android.util.Log
import com.jarvis.voice.pipeline.AudioFrame
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * DEBUG-ONLY LOCAL WAV FORWARD TAP:
 * Allows exact inspection of audio frames forwarded by ForwardingGate.
 * Never active in production releases.
 */
class WavForwardTap(private val context: Context) {

    companion object {
        private const val TAG = "WavForwardTap"
    }

    private var currentFile: File? = null
    private var fos: FileOutputStream? = null
    private var totalPcmBytes = 0

    fun startCapture(sessionTag: String) {
        try {
            val debugDir = File(context.cacheDir, "debug_forward_tap")
            if (!debugDir.exists()) debugDir.mkdirs()

            currentFile = File(debugDir, "forward_tap_${sessionTag}_${System.currentTimeMillis()}.wav")
            fos = FileOutputStream(currentFile)
            totalPcmBytes = 0

            // Write 44-byte placeholder WAV header
            fos?.write(ByteArray(44))
            Log.d(TAG, "Debug WAV tap started: ${currentFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting debug WAV tap: ${e.message}")
        }
    }

    fun tapFrame(frame: AudioFrame) {
        val stream = fos ?: return
        try {
            val byteBuffer = ByteArray(frame.data.size * 2)
            for (i in frame.data.indices) {
                val s = frame.data[i].toInt()
                byteBuffer[i * 2] = (s and 0x00FF).toByte()
                byteBuffer[i * 2 + 1] = ((s shr 8) and 0x00FF).toByte()
            }
            stream.write(byteBuffer)
            totalPcmBytes += byteBuffer.size
        } catch (e: Exception) {
            Log.e(TAG, "Error writing frame to debug WAV tap: ${e.message}")
        }
    }

    fun stopCapture() {
        try {
            fos?.flush()
            fos?.close()
            fos = null

            // Write final WAV header with total bytes
            currentFile?.let { file ->
                val raf = RandomAccessFile(file, "rw")
                writeWavHeader(raf, totalPcmBytes, 16000, 1, 16)
                raf.close()
                Log.i(TAG, "Debug WAV tap saved: ${file.absolutePath} ($totalPcmBytes bytes PCM)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed finalizing debug WAV tap: ${e.message}")
        }
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)

        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeInt(Integer.reverseBytes(totalDataLen))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeInt(Integer.reverseBytes(16)) // Subchunk1Size (16 for PCM)
        raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat (1 for PCM)
        raf.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        raf.writeInt(Integer.reverseBytes(sampleRate))
        raf.writeInt(Integer.reverseBytes(byteRate))
        raf.writeShort(java.lang.Short.reverseBytes((channels * (bitsPerSample / 8)).toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
        raf.writeBytes("data")
        raf.writeInt(Integer.reverseBytes(totalAudioLen))
    }
}
