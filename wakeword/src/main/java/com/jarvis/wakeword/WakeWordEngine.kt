package com.jarvis.wakeword

import com.jarvis.voice.pipeline.AudioFrame

/**
 * Wake Word Engine Interface.
 * Consumes AudioFrames from the single AudioPipeline.
 * NEVER creates its own AudioRecord.
 */
interface WakeWordEngine {
    fun initialize(): Boolean
    fun processFrame(frame: AudioFrame): Boolean
    fun release()
}
