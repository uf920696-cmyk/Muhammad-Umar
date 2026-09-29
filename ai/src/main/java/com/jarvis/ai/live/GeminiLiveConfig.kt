package com.jarvis.ai.live

/**
 * GEMINI LIVE AUDIO SPECIFICATION & FORMAT CONSTANTS:
 * Matches Gemini Multimodal Live API specifications:
 * - Input format: 16 kHz sample rate, 16-bit PCM, 1 channel (mono), little-endian.
 * - Output format: 24 kHz sample rate, 16-bit PCM, 1 channel (mono).
 * - Chunk size: 512 samples (32ms) to 1024 samples (64ms).
 */
object GeminiLiveConfig {
    /** Model identifier - configuration driven */
    const val GEMINI_LIVE_MODEL = "models/gemini-3.8-live"

    const val INPUT_SAMPLE_RATE = 16000
    const val INPUT_CHANNELS = 1
    const val INPUT_BYTES_PER_SAMPLE = 2

    const val OUTPUT_SAMPLE_RATE = 24000
    const val OUTPUT_CHANNELS = 1

    /** Target latency chunk size in samples (32ms at 16kHz) */
    const val CHUNK_SAMPLES = 512

    /** Maximum session duration: 15 minutes */
    const val MAX_SESSION_DURATION_MS = 15 * 60 * 1000L
}
