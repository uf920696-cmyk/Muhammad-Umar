package com.jarvis.voice.ack

import android.content.Context
import android.util.Log

/**
 * EXACT USER REQUIREMENT:
 * "pehli roblem ke iski beeps khtm nhi hoi ,wo khtm karo"
 * Completely removes any tone/beep noises for a 100% silent, pleasant experience.
 */
class LocalAckPlayer(private val context: Context) {

    companion object {
        private const val TAG = "LocalAckPlayer"
    }

    /**
     * Completely silent: No beeps played whatsoever.
     */
    fun playAck(): Long {
        Log.d(TAG, "Beeps eliminated per user requirement: totally silent.")
        return System.currentTimeMillis()
    }

    fun release() {
        // No resources to release
    }
}

