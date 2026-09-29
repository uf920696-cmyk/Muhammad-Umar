package com.jarvis.core.common.agent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JarvisAgentEngineTest {

    private class MockJarvisAgentEngine : JarvisAgentEngine {
        private val _history = mutableListOf<ConversationTurn>()
        override val conversationHistory: List<ConversationTurn>
            get() = _history.toList()

        var nextResponse: AgentResponse = AgentResponse.Conversational("Default reply", "gemini-3.8-flash")
        var recordedTurnsAtCallTime = listOf<ConversationTurn>()

        override suspend fun processUtterance(utterance: String): AgentResponse {
            _history.add(ConversationTurn(AgentRole.USER, utterance))
            recordedTurnsAtCallTime = _history.dropLast(1).toList()
            val resp = nextResponse
            if (resp is AgentResponse.Conversational) {
                _history.add(ConversationTurn(AgentRole.ASSISTANT, resp.replyText))
            }
            return resp
        }

        override fun clearHistory() {
            _history.clear()
        }

        override fun getHistorySnapshot(): List<ConversationTurn> = _history.toList()
    }

    @Test
    fun testMultiTurnConversationMemoryPreserved() = runBlocking {
        val engine = MockJarvisAgentEngine()

        // Turn 1
        engine.nextResponse = AgentResponse.Conversational(
            "Pakistan ki economy ek developing economy hai.",
            "gemini-3.8-flash"
        )
        val resp1 = engine.processUtterance("Mujhe Pakistan ki economy ke bare mein batao.")
        assertTrue(resp1 is AgentResponse.Conversational)
        assertEquals("Pakistan ki economy ek developing economy hai.", (resp1 as AgentResponse.Conversational).replyText)
        assertEquals(2, engine.conversationHistory.size)
        assertEquals("Mujhe Pakistan ki economy ke bare mein batao.", engine.conversationHistory[0].text)
        assertEquals(AgentRole.USER, engine.conversationHistory[0].role)
        assertEquals(AgentRole.ASSISTANT, engine.conversationHistory[1].role)

        // Turn 2: Follow-up question relying on context
        engine.nextResponse = AgentResponse.Conversational(
            "Iska sabse bara issue fiscal deficit aur inflation hai.",
            "gemini-3.8-flash"
        )
        val resp2 = engine.processUtterance("Iska sabse bara issue kya hai?")
        assertTrue(resp2 is AgentResponse.Conversational)

        // Verify that previous turns were recorded and available for contextual prompt construction
        assertEquals(2, engine.recordedTurnsAtCallTime.size)
        assertEquals("Mujhe Pakistan ki economy ke bare mein batao.", engine.recordedTurnsAtCallTime[0].text)
        assertEquals("Pakistan ki economy ek developing economy hai.", engine.recordedTurnsAtCallTime[1].text)
        assertEquals(4, engine.conversationHistory.size)
    }

    @Test
    fun testAgentResponseHierarchy() {
        val conv: AgentResponse = AgentResponse.Conversational("Hello sir", "gemini-3.8-flash")
        val tool: AgentResponse = AgentResponse.ToolRequest("set_volume", mapOf("percent" to 50))
        val error: AgentResponse = AgentResponse.Error("Network timeout connecting to AI server")

        assertTrue(conv is AgentResponse.Conversational)
        assertTrue(tool is AgentResponse.ToolRequest)
        assertTrue(error is AgentResponse.Error)

        assertEquals("set_volume", (tool as AgentResponse.ToolRequest).toolName)
        assertEquals(50, tool.arguments["percent"])
        assertEquals("Network timeout connecting to AI server", (error as AgentResponse.Error).errorMessage)
    }

    @Test
    fun testClearHistoryEmptiesSession() = runBlocking {
        val engine = MockJarvisAgentEngine()
        engine.processUtterance("Hello Jarvis")
        assertEquals(2, engine.conversationHistory.size)

        engine.clearHistory()
        assertEquals(0, engine.conversationHistory.size)
        assertTrue(engine.getHistorySnapshot().isEmpty())
    }

    @Test
    fun testErrorResponseDoesNotFakeSuccess() = runBlocking {
        val engine = MockJarvisAgentEngine()
        engine.nextResponse = AgentResponse.Error("Connection refused: 503 Service Unavailable")

        val resp = engine.processUtterance("Who is the prime minister?")
        assertTrue(resp is AgentResponse.Error)
        val err = resp as AgentResponse.Error
        assertTrue(err.errorMessage.contains("503 Service Unavailable"))
        // Assistant turn is NOT added when error occurs
        assertEquals(1, engine.conversationHistory.size)
        assertEquals(AgentRole.USER, engine.conversationHistory[0].role)
    }
}
