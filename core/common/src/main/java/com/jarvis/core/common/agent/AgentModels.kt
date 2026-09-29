package com.jarvis.core.common.agent

enum class AgentRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

data class ConversationTurn(
    val role: AgentRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AgentResponse {
    data class Conversational(
        val replyText: String,
        val modelUsed: String = "gemini-3.8-flash"
    ) : AgentResponse()

    data class ToolRequest(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val rationale: String? = null
    ) : AgentResponse()

    data class Error(
        val errorMessage: String,
        val cause: Throwable? = null
    ) : AgentResponse()
}

interface JarvisAgentEngine {
    val conversationHistory: List<ConversationTurn>
    suspend fun processUtterance(utterance: String): AgentResponse
    fun clearHistory()
    fun getHistorySnapshot(): List<ConversationTurn>
}
