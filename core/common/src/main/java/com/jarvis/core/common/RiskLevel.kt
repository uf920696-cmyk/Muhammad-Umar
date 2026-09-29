package com.jarvis.core.common

/**
 * Tool Risk Levels for ActionRouter safety gates.
 */
enum class RiskLevel {
    /** Torch, volume, open app, URL, camera intent, timer, alarm */
    LOW,

    /** Normal phone call, WhatsApp call attempt */
    MEDIUM,

    /** SMS, WhatsApp auto-send, deletion, private screen/file uploads */
    HIGH
}
