package com.jarvis.voice.pipeline

/**
 * Encapsulates a single PCM 16-bit mono audio frame with high-precision timestamp.
 */
data class AudioFrame(
    val data: ShortArray,
    val timestampMs: Long,
    val sampleRate: Int = 16000
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioFrame
        if (!data.contentEquals(other.data)) return false
        if (timestampMs != other.timestampMs) return false
        if (sampleRate != other.sampleRate) return false
        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + sampleRate
        return result
    }
}
