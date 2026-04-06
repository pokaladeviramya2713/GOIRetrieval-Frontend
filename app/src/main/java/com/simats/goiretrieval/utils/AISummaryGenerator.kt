package com.simats.goiretrieval.utils

import com.google.gson.Gson
import com.simats.goiretrieval.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * AISummaryGenerator — Rebuilt (v3)
 * Now calls the FastAPI /ai_audit backend endpoint instead of calling Gemini directly.
 * This means:
 * - No Android SDK crash issues
 * - Gemini model selection handled by the server
 * - Cleaner, more reliable architecture
 */
object AISummaryGenerator {

    // Point to your FastAPI backend (same as other API calls in the app)
    private val BACKEND_URL = "${RetrofitClient.BASE_URL}ai_audit"

    data class SummaryResult(
        var brief: String = "",
        var detailed: String = "",
        var reference: String = "",
        var tags: List<String> = listOf(),
        var mockContent: String = ""
    )

    suspend fun generate(
        title: String,
        category: String,
        questions: String? = null,
        score: Int? = 100,
        rawContent: String? = null,
        sourceUrl: String? = null
    ): SummaryResult = withContext(Dispatchers.IO) {

        val question = questions ?: "General compliance audit"
        val docText  = rawContent ?: ""
        val src      = sourceUrl ?: ""

        // Build JSON body to POST to backend
        val body = JSONObject().apply {
            put("question",      question)
            put("document_text", docText)
            put("source_url",    src)
            put("doc_title",     title)
        }.toString()

        android.util.Log.d("AI_BACKEND", "Posting to $BACKEND_URL (doc chars=${docText.length})")

        val responseJson = try {
            val url  = URL(BACKEND_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 20_000
            conn.readTimeout    = 90_000

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }

            val code = conn.responseCode
            android.util.Log.d("AI_BACKEND", "Response code: $code")

            if (code != HttpURLConnection.HTTP_OK) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
                android.util.Log.e("AI_BACKEND", "Backend error: $err")
                return@withContext SummaryResult(
                    brief = "⚠️ Backend Error ($code): Could not reach AI service. Is the server running?",
                    detailed = err,
                    reference = "Server Error",
                    tags = listOf("#Error", "#Server"),
                    mockContent = err
                )
            }

            conn.inputStream.bufferedReader().readText()

        } catch (e: Exception) {
            android.util.Log.e("AI_BACKEND", "Connection failed: ${e.message}")
            return@withContext SummaryResult(
                brief = "⚠️ Cannot reach backend. Make sure your FastAPI server is running on port 8000.",
                detailed = "Exception: ${e.message}",
                reference = "Connection Error",
                tags = listOf("#Error", "#Network"),
                mockContent = "Backend connection failed."
            )
        }

        android.util.Log.d("AI_BACKEND", "Raw response: ${responseJson.take(300)}")

        // Parse the backend response
        return@withContext try {
            val json   = JSONObject(responseJson)
            val status = json.optString("status", "error")

            if (status == "error") {
                SummaryResult(
                    brief    = "⚠️ AI Error: ${json.optString("message", "Unknown error")}",
                    detailed = json.optString("message", "Unknown error"),
                    tags     = listOf("#Error"),
                    mockContent = json.optString("message", "")
                )
            } else {
                val result = json.optJSONObject("result")
                if (result == null) {
                    SummaryResult(brief = "AI returned empty result.", mockContent = responseJson)
                } else {
                    val tagsArray = result.optJSONArray("tags")
                    val tagsList  = mutableListOf<String>()
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) tagsList.add(tagsArray.getString(i))
                    }
                    SummaryResult(
                        brief       = result.optString("brief", ""),
                        detailed    = result.optString("detailed", ""),
                        reference   = result.optString("reference", ""),
                        tags        = tagsList,
                        mockContent = result.optString("mockContent", "")
                    )
                }
            }
        } catch (e: Exception) {
            SummaryResult(
                brief       = responseJson.take(300),
                detailed    = responseJson,
                reference   = "Parse Error",
                tags        = listOf("#ParseError"),
                mockContent = responseJson
            )
        }
    }
}
