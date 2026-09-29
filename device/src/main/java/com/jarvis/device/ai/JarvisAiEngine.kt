package com.jarvis.device.ai

import android.content.Context
import android.util.Log
import com.jarvis.core.common.agent.AgentResponse
import com.jarvis.core.common.agent.DefaultJarvisAgentEngine
import com.jarvis.core.common.agent.JarvisAgentEngine

/**
 * JARVIS AI ENGINE (PHASE 1 - REAL LLM CONVERSATIONAL AGENT):
 * - 100% Real Artificial Intelligence backed by Gemini LLM.
 * - Manages multi-turn conversation memory (contextual follow-ups).
 * - Understands natural language, Roman Urdu, Urdu, Hindi, English seamlessly.
 * - Zero hardcoded response tables, zero fake replies, zero brittle web scraping.
 * - Extensible for Phase 2 Tool Calling.
 */
class JarvisAiEngine(
    private val context: Context? = null,
    private val agentEngine: JarvisAgentEngine = if (context != null) {
        DefaultJarvisAgentEngine.getInstance(context)
    } else {
        // Fallback for non-context unit testing / injection
        object : JarvisAgentEngine {
            override val conversationHistory = emptyList<com.jarvis.core.common.agent.ConversationTurn>()
            override suspend fun processUtterance(utterance: String): AgentResponse {
                return AgentResponse.Error("Context required for network communication")
            }
            override fun clearHistory() {}
            override fun getHistorySnapshot() = emptyList<com.jarvis.core.common.agent.ConversationTurn>()
        }
    }
) {

    companion object {
        private const val TAG = "JarvisAiEngine"
    }

    /**
     * Sends user query to the genuine LLM brain with multi-turn conversation memory.
     * Returns the genuine LLM response or truthful failure message.
     */
    suspend fun askAi(userQuery: String): String {
        val trimmed = userQuery.trim()
        if (trimmed.isEmpty()) {
            return "Ji? Main sun rahi hoon, boliye."
        }

        Log.i(TAG, "Forwarding utterance to LLM Agent Brain: '$trimmed'")
        return when (val response = agentEngine.processUtterance(trimmed)) {
            is AgentResponse.Conversational -> {
                Log.d(TAG, "Received conversational response (model=${response.modelUsed})")
                response.replyText
            }
            is AgentResponse.ToolRequest -> {
                Log.i(TAG, "Tool request recognized: ${response.toolName}")
                // In Phase 1, tools are not executed yet; truthful acknowledgment:
                "Samajh gayi, ${response.toolName} ka amal darkar hai. (Phase 2 Tool Registry integration in progress)."
            }
            is AgentResponse.Error -> {
                Log.e(TAG, "Agent error: ${response.errorMessage}")
                // Truthful error response — NEVER fake a successful answer!
                "Maazrat chahti hoon, AI server se jawab hasil nahi ho saka: ${response.errorMessage}"
            }
        }
    }

    fun clearSessionMemory() {
        agentEngine.clearHistory()
    }
}
