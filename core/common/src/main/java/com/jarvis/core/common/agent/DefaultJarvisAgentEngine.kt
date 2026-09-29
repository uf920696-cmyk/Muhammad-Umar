package com.jarvis.core.common.agent

import android.content.Context
import android.util.Log
import com.jarvis.core.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * DEFAULT JARVIS AGENT ENGINE (PHASE 1):
 * - Real LLM-powered autonomous AI Brain.
 * - Manages in-session conversation memory across multiple turns.
 * - Connects to the secure backend Gemini proxy (/api/chat).
 * - Provides direct Gemini REST fallback if a direct API key is configured.
 * - Never returns fake, hardcoded, or scraped answers.
 * - Extensible response contract prepared for Phase 2 Tool Calling.
 */
class DefaultJarvisAgentEngine(
    private val context: Context,
    private val secureStorageManager: SecureStorageManager = SecureStorageManager(context),
    private val maxHistoryTurns: Int = 16
) : JarvisAgentEngine {

    companion object {
        private const val TAG = "JarvisAgentEngine"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000

        @Volatile
        private var instance: DefaultJarvisAgentEngine? = null

        fun getInstance(context: Context): DefaultJarvisAgentEngine {
            return instance ?: synchronized(this) {
                instance ?: DefaultJarvisAgentEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val historyMutex = Mutex()
    private val _history = mutableListOf<ConversationTurn>()

    override val conversationHistory: List<ConversationTurn>
        get() = _history.toList()

    override fun clearHistory() {
        _history.clear()
        Log.i(TAG, "Conversation history cleared")
    }

    override fun getHistorySnapshot(): List<ConversationTurn> {
        return _history.toList()
    }

    override suspend fun processUtterance(utterance: String): AgentResponse = withContext(Dispatchers.IO) {
        val cleanInput = utterance.trim()
        if (cleanInput.isBlank()) {
            return@withContext AgentResponse.Error("Empty user utterance received")
        }

        // 1. Record User Turn in Memory
        val userTurn = ConversationTurn(
            role = AgentRole.USER,
            text = cleanInput,
            timestamp = System.currentTimeMillis()
        )

        val turnsForRequest = historyMutex.withLock {
            _history.add(userTurn)
            trimHistoryLocked()
            // Send previous turns excluding the current one as conversationHistory
            _history.dropLast(1).toList()
        }

        // 2. Execute LLM Query via Backend Proxy or Direct Fallback
        val backendUrl = secureStorageManager.getBackendUrl()
        val directApiKey = secureStorageManager.getGeminiApiKey()
        val model = secureStorageManager.getAiModel()

        var lastError: Exception? = null

        // Try Backend Proxy first
        try {
            val response = queryBackendProxy(backendUrl, cleanInput, turnsForRequest, model)
            if (response != null) {
                recordAssistantTurn(response)
                return@withContext response
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend proxy query failed (${e.message}), checking direct fallback...")
            lastError = e
        }

        // Try Direct Gemini API fallback if API key configured
        if (!directApiKey.isNullOrBlank()) {
            try {
                val directResponse = queryDirectGemini(directApiKey, cleanInput, turnsForRequest, model)
                if (directResponse != null) {
                    recordAssistantTurn(directResponse)
                    return@withContext directResponse
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct Gemini API query also failed: ${e.message}", e)
                lastError = e
            }
        }

        // Truthful Error reporting — NEVER return a fake reply!
        val errMsg = lastError?.message ?: "Unable to contact Gemini AI service"
        Log.e(TAG, "Agent reasoning failed: $errMsg")
        AgentResponse.Error(
            errorMessage = "JARVIS AI connection failed: $errMsg. Please check server connection or configure Gemini API key in Settings.",
            cause = lastError
        )
    }

    private suspend fun recordAssistantTurn(response: AgentResponse) {
        if (response is AgentResponse.Conversational) {
            historyMutex.withLock {
                _history.add(
                    ConversationTurn(
                        role = AgentRole.ASSISTANT,
                        text = response.replyText,
                        timestamp = System.currentTimeMillis()
                    )
                )
                trimHistoryLocked()
            }
        }
    }

    private fun trimHistoryLocked() {
        while (_history.size > maxHistoryTurns) {
            _history.removeAt(0)
        }
    }

    /**
     * Query backend proxy endpoint: POST /api/chat
     */
    private fun queryBackendProxy(
        baseUrl: String,
        message: String,
        history: List<ConversationTurn>,
        model: String
    ): AgentResponse? {
        val endpoint = "${baseUrl.trimEnd('/')}/api/chat"
        Log.d(TAG, "Connecting to backend AI endpoint: $endpoint")

        val payload = JSONObject().apply {
            put("message", message)
            put("model", model)
            put("role", "jarvis_core")

            val historyArray = JSONArray()
            history.forEach { turn ->
                historyArray.put(JSONObject().apply {
                    put("role", if (turn.role == AgentRole.USER) "user" else "assistant")
                    put("text", turn.text)
                })
            }
            put("conversationHistory", historyArray)
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val json = JSONObject(responseText)

            // Check if function calls were returned (for Phase 2 tool integration)
            val functionCalls = json.optJSONArray("functionCalls")

            if (functionCalls != null && functionCalls.length() > 0) {
                val firstCall = functionCalls.getJSONObject(0)
                val toolName = firstCall.optString("name", "unknown_tool")
                val argsObj = firstCall.optJSONObject("args") ?: JSONObject()
                val argsMap = mutableMapOf<String, Any?>()
                argsObj.keys().forEach { key ->
                    argsMap[key] = argsObj.get(key)
                }
                return AgentResponse.ToolRequest(
                    toolName = toolName,
                    arguments = argsMap,
                    rationale = "Requested by LLM in response to: $message"
                )
            }

            val replyText = json.optString("text", "").trim()
            val usedModel = json.optString("modelUsed", model)

            if (replyText.isNotEmpty()) {
                return AgentResponse.Conversational(
                    replyText = replyText,
                    modelUsed = usedModel
                )
            }
        } else {
            val errStream = conn.errorStream ?: conn.inputStream
            val errText = errStream?.let { BufferedReader(InputStreamReader(it, "UTF-8")).use { r -> r.readText() } } ?: "HTTP $statusCode"
            Log.e(TAG, "Backend returned error $statusCode: $errText")
            throw RuntimeException("Backend HTTP $statusCode: $errText")
        }

        return null
    }

    /**
     * Direct Gemini REST API Fallback if configured by user
     */
    private fun queryDirectGemini(
        apiKey: String,
        message: String,
        history: List<ConversationTurn>,
        model: String
    ): AgentResponse? {
        val targetModel = if (model.isNotBlank()) model else "gemini-3.8-flash"
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"
        Log.d(TAG, "Direct Gemini API query for model: $targetModel")

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "You are J.A.R.V.I.S., Tony Stark's brilliant, highly autonomous personal assistant. You possess deep, encyclopedic intelligence across all subjects (science, history, politics, everyday life, coding). You speak fluently in English, Urdu, Hindi, and Roman Urdu. Match the user's language effortlessly. Keep spoken responses natural, polished, concise, and helpful. Never claim ignorance when knowledge is available.")
                }))
            })

            val contents = JSONArray()
            history.forEach { turn ->
                contents.put(JSONObject().apply {
                    put("role", if (turn.role == AgentRole.USER) "user" else "model")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", turn.text)
                    }))
                })
            }
            // Add current message
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", message)
                }))
            })

            put("contents", contents)
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val json = JSONObject(responseText)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) {
                        return AgentResponse.Conversational(
                            replyText = text.trim(),
                            modelUsed = targetModel
                        )
                    }
                }
            }
        } else {
            val errText = conn.errorStream?.let { BufferedReader(InputStreamReader(it, "UTF-8")).use { r -> r.readText() } } ?: "HTTP $statusCode"
            Log.e(TAG, "Direct Gemini error $statusCode: $errText")
            throw RuntimeException("Gemini API error $statusCode: $errText")
        }

        return null
    }
}
