package com.jarvis.ai.agent

import android.content.Context
import com.jarvis.core.common.agent.DefaultJarvisAgentEngine as CoreDefaultJarvisAgentEngine
import com.jarvis.core.common.agent.JarvisAgentEngine
import com.jarvis.core.security.SecureStorageManager

/**
 * AI Module entry point for JarvisAgentEngine.
 * Delegates to the core engine implementation.
 */
class DefaultJarvisAgentEngine(
    context: Context,
    secureStorageManager: SecureStorageManager = SecureStorageManager(context),
    maxHistoryTurns: Int = 16
) : JarvisAgentEngine by CoreDefaultJarvisAgentEngine.getInstance(context)
