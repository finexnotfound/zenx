package com.example.zenx.ai

import com.example.zenx.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FlintAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Interactive conversation history
    private val chatHistory = mutableListOf<Pair<String, String>>()

    fun clearHistory() {
        chatHistory.clear()
    }

    suspend fun askFlint(userInput: String, systemContext: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If key is valid and not dummy, call Gemini API
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val apiResponse = callGeminiApi(apiKey, userInput, systemContext)
                chatHistory.add("user" to userInput)
                chatHistory.add("model" to apiResponse)
                return@withContext apiResponse
            } catch (e: Exception) {
                // If API call failed, fallback gracefully to smart built-in terminal AI engine
                return@withContext fallbackFlintResponse(userInput, "API Connection Note: ${e.message}")
            }
        } else {
            // Intelligent built-in Flint response with terminal expertise
            return@withContext fallbackFlintResponse(userInput, null)
        }
    }

    private fun callGeminiApi(apiKey: String, prompt: String, systemContext: String?): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Include recent history (up to last 6 turns)
        val recentHistory = chatHistory.takeLast(6)
        for ((role, text) in recentHistory) {
            val part = JSONObject().put("text", text)
            val contentObj = JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(part))
            contentsArray.put(contentObj)
        }

        // Current user message
        val currentPart = JSONObject().put("text", prompt)
        val currentContent = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(currentPart))
        contentsArray.put(currentContent)

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)

            // System instructions tailored for Flint Terminal AI
            val systemPrompt = """
                You are Flint, an advanced Linux terminal AI assistant running inside ZEN X on Android.
                You are deeply knowledgeable in Linux commands, Bash, Git, Python, C, JavaScript, networking, package management, and Android internals.
                Give concise, high-value, terminal-friendly responses.
                Use clean markdown for code or commands.
                ${systemContext ?: ""}
            """.trimIndent()

            val sysPart = JSONObject().put("text", systemPrompt)
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(sysPart)))

            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.9)
                put("maxOutputTokens", 1024)
            }
            put("generationConfig", genConfig)
        }

        val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response body from Gemini API")

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "No text generated.")
            }
        }
        return "Flint received an empty response."
    }

    private fun fallbackFlintResponse(input: String, note: String?): String {
        val q = input.trim().lowercase()

        val baseResponse = when {
            q.contains("hello") || q.contains("hi") || q == "hey" ->
                "Hello! I'm Flint, your ZEN X terminal AI. How can I assist your terminal workflow today?"

            q.contains("who are you") || q.contains("what are you") ->
                "I am Flint, the native AI companion embedded in ZEN X. I help you with bash scripting, package management, git operations, and code execution."

            q.contains("git") && (q.contains("clone") || q.contains("how")) ->
                """
Git Cloning in ZEN X:
  To clone any GitHub repository into your filesystem:
    git clone https://github.com/octocat/Hello-World

  If Git is not yet installed:
    pkg install git

  After cloning, explore the project:
    cd Hello-World
    ls -la
    cat README.md
""".trimIndent()

            q.contains("package") || q.contains("pkg") || q.contains("install") ->
                """
ZEN X Package Manager (`pkg`):
  • pkg install <name>   - Install package (git, python, node, tree, cowsay...)
  • pkg list             - View installed packages
  • pkg search <query>   - Search available packages
  • pkg uninstall <name> - Remove a package
  • pkg open flint       - Return to Flint AI anytime
""".trimIndent()

            q.contains("python") ->
                """
Running Python in ZEN X:
  1. Interactive REPL:
     python
  2. Run a script:
     python hello.py
  3. Create a quick script:
     echo 'print("ZEN X Python test")' > test.py
     python test.py
""".trimIndent()

            q.contains("node") || q.contains("javascript") || q.contains("js") ->
                """
Running JavaScript/Node in ZEN X:
  1. Interactive Node REPL:
     node
  2. Run a script:
     node app.js
""".trimIndent()

            q.contains("help") ->
                """
Flint AI Terminal Assistance:
  • Ask me anything: "how do I write a bash loop?", "explain git branching"
  • Write scripts: "give me a python script to calculate primes"
  • Exit Flint: type 'exit' or 'quit' to return to standard ZEN X bash prompt.
""".trimIndent()

            q.startsWith("write") || q.contains("script") || q.contains("code") ->
                """
Here is a helpful script snippet for your request:

```bash
#!/bin/bash
# ZEN X automated backup script
echo "Starting archive at $(date)..."
tar -czf backup_$(date +%Y%m%d).tar.gz ~/projects/
echo "Done! Backup created in ~"
```

Save it using:
  nano backup.sh
  chmod +x backup.sh
  ./backup.sh
""".trimIndent()

            else ->
                """
Flint: I analyzed your query: "$input"

Advice & Recommendation:
• In ZEN X you have a full virtual Unix environment.
• You can run shell utilities: `ls`, `grep`, `curl`, `tree`, `nano`.
• For external APIs or web data: `curl https://wttr.in/Paris`
• Type 'exit' to switch back to normal shell commands.
""".trimIndent()
        }

        return if (note != null) {
            "$baseResponse\n\n[$note]"
        } else {
            baseResponse
        }
    }
}
