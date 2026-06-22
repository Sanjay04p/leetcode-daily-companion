package com.example.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProblemEntry
import com.example.data.ProblemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.Calendar

data class AIHintResult(
    val pattern: String = "",
    val keyInsight: String = "",
    val algorithm: String = "",
    val complexity: String = "",
    val watchOut: String = ""
)

sealed interface AIHintState {
    object Idle : AIHintState
    object Loading : AIHintState
    data class Success(val rawResponse: String, val parsed: AIHintResult) : AIHintState
    data class Error(val message: String) : AIHintState
}

class LeetCodeViewModel(private val repository: ProblemRepository) : ViewModel() {

    // Preset list of tags
    val presetTags = listOf(
        "Array", "String", "DP", "Graph", "Tree", "BFS", "DFS", "Greedy",
        "Backtracking", "Binary Search", "Two Pointers", "Sliding Window",
        "Stack", "Heap", "Math", "Hash Map", "Linked List"
    )

    // All problems from Database ordered by dateLogged desc
    val problems: StateFlow<List<ProblemEntry>> = repository.allProblems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun getEpochDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    // Streaks calculations
    val currentStreak: StateFlow<Int> = problems.map { list ->
        val solvedDays = list.filter { it.status == "SOLVED" }
            .map { getEpochDay(it.dateLogged) }
            .distinct()
            .sortedDescending()

        val today = getEpochDay(System.currentTimeMillis())
        if (solvedDays.isEmpty()) return@map 0
        val firstSolved = solvedDays.first()
        if (firstSolved < today - 1) {
            0
        } else {
            var count = 0
            var checkDay = firstSolved
            while (solvedDays.contains(checkDay)) {
                count++
                checkDay--
            }
            count
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val longestStreak: StateFlow<Int> = problems.map { list ->
        val solvedDays = list.filter { it.status == "SOLVED" }
            .map { getEpochDay(it.dateLogged) }
            .distinct()
            .sorted() // ascending for forward scanning

        if (solvedDays.isEmpty()) return@map 0
        var maxStreak = 1
        var currentLen = 1
        for (i in 1 until solvedDays.size) {
            if (solvedDays[i] == solvedDays[i - 1] + 1) {
                currentLen++
            } else {
                if (currentLen > maxStreak) {
                    maxStreak = currentLen
                }
                currentLen = 1
            }
        }
        maxOf(maxStreak, currentLen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- SCREEN 2 - LOG PROBLEM FORM STATE ---
    var formNumber by mutableStateOf("")
    var formTitle by mutableStateOf("")
    var formDifficulty by mutableStateOf("EASY") // EASY, MEDIUM, HARD
    var formStatus by mutableStateOf("SOLVED")     // SOLVED, ATTEMPTED, SKIPPED
    val formSelectedTags = mutableStateListOf<String>()
    var formTimeSeconds by mutableStateOf(0)
    var formLanguage by mutableStateOf("Python")
    var formNotes by mutableStateOf("")
    var formConfidence by mutableStateOf(3)

    // Timer status
    var isTimerRunning by mutableStateOf(false)
        private set
    private var timerJob: Job? = null

    fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (isTimerRunning) {
                delay(1000)
                formTimeSeconds++
            }
        }
    }

    fun pauseTimer() {
        isTimerRunning = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        formTimeSeconds = 0
    }

    fun toggleTagSelection(tag: String) {
        if (formSelectedTags.contains(tag)) {
            formSelectedTags.remove(tag)
        } else {
            formSelectedTags.add(tag)
        }
    }

    fun isFormDirty(): Boolean {
        return formNumber.trim().isNotEmpty() ||
                formTitle.trim().isNotEmpty() ||
                formSelectedTags.isNotEmpty() ||
                formNotes.trim().isNotEmpty() ||
                formTimeSeconds > 0
    }

    fun resetForm() {
        formNumber = ""
        formTitle = ""
        formDifficulty = "EASY"
        formStatus = "SOLVED"
        formSelectedTags.clear()
        formTimeSeconds = 0
        formLanguage = "Python"
        formNotes = ""
        formConfidence = 3
        isTimerRunning = false
        timerJob?.cancel()
    }

    fun saveProblem(onSuccess: () -> Unit) {
        val num = formNumber.toIntOrNull() ?: return
        val title = formTitle.trim().ifEmpty { "Problem $num" }
        viewModelScope.launch(Dispatchers.IO) {
            val problem = ProblemEntry(
                leetcodeNumber = num,
                title = title,
                difficulty = formDifficulty,
                status = formStatus,
                tags = formSelectedTags.joinToString(","),
                timeTakenSeconds = formTimeSeconds,
                language = formLanguage,
                notes = formNotes,
                confidenceRating = formConfidence,
                dateLogged = System.currentTimeMillis()
            )
            repository.insert(problem)
            viewModelScope.launch(Dispatchers.Main) {
                resetForm()
                onSuccess()
            }
        }
    }

    // --- SCREEN 3 - HISTORY SEARCH & FILTERS ---
    var searchQuery by mutableStateOf("")
    var selectedFilter by mutableStateOf("All") // All, Easy, Medium, Hard, Solved, Attempted, Skipped, or preset tags

    val historyProblems: StateFlow<List<ProblemEntry>> = combine(
        problems,
        snapshotFlow { searchQuery },
        snapshotFlow { selectedFilter }
    ) { list, query, filter ->
        var result = list

        if (query.trim().isNotEmpty()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.leetcodeNumber.toString().contains(q) ||
                        it.title.lowercase().contains(q)
            }
        }

        if (filter != "All") {
            result = when (filter) {
                "Easy" -> result.filter { it.difficulty == "EASY" }
                "Medium" -> result.filter { it.difficulty == "MEDIUM" }
                "Hard" -> result.filter { it.difficulty == "HARD" }
                "Solved" -> result.filter { it.status == "SOLVED" }
                "Attempted" -> result.filter { it.status == "ATTEMPTED" }
                "Skipped" -> result.filter { it.status == "SKIPPED" }
                else -> result.filter {
                    it.tags.split(",").map { t -> t.trim() }.contains(filter)
                }
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteProblem(problem: ProblemEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(problem)
        }
    }

    // --- SCREEN 5 - AI HINT STATE ---
    var aiProblemStatement by mutableStateOf("")
    var aiLanguage by mutableStateOf("Python")
    var aiState by mutableStateOf<AIHintState>(AIHintState.Idle)
        private set

    fun getAIHint() {
        val statement = aiProblemStatement.trim()
        if (statement.isEmpty()) return
        aiState = AIHintState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // BuildConfig mapping for GEMINI_API_KEY
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    viewModelScope.launch(Dispatchers.Main) {
                        aiState = AIHintState.Error("Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio.")
                    }
                    return@launch
                }

                val systemInstruction = """
                    You are a competitive programming coach. Given a LeetCode problem statement, respond ONLY with:
                    1. Pattern: [pattern name]
                    2. Key insight: [1-2 sentences]
                    3. Algorithm: [approach in plain English, no code]
                    4. Complexity: Time O(...) | Space O(...)
                    5. Watch out for: [1 edge case]
                    Do NOT write any code. Keep total response under 150 words.
                """.trimIndent()

                val promptText = "Preferred coding language: $aiLanguage\n\nProblem Statement:\n$statement"

                // Construct REST Call
                val contentsArray = org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(
                        JSONObject().put("text", promptText)
                    ))
                )

                val systemInstructionObj = JSONObject().put("parts", org.json.JSONArray().put(
                    JSONObject().put("text", systemInstruction)
                ))

                val requestBodyJson = JSONObject().apply {
                    put("contents", contentsArray)
                    put("systemInstruction", systemInstructionObj)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = RequestBody.create(mediaType, requestBodyJson.toString())
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        viewModelScope.launch(Dispatchers.Main) {
                            aiState = AIHintState.Error("API call failed (code ${response.code}): $responseBody")
                        }
                        return@launch
                    }
                    val responseString = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseString)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val text = parts.getJSONObject(0).getString("text")
                            val parsedResult = parseAIHint(text)
                            viewModelScope.launch(Dispatchers.Main) {
                                aiState = AIHintState.Success(text, parsedResult)
                            }
                        } else {
                            viewModelScope.launch(Dispatchers.Main) {
                                aiState = AIHintState.Error("Error: Empty parts list in content.")
                            }
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            aiState = AIHintState.Error("Error: No candidates found in the response.")
                        }
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    aiState = AIHintState.Error("Network/Execution error: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    private fun parseAIHint(rawResponse: String): AIHintResult {
        var pattern = ""
        var keyInsight = ""
        var algorithm = ""
        val complexityStr = StringBuilder()
        var watchOut = ""

        val lines = rawResponse.lines().map { it.trim() }
        var currentSection = 0 // 1: pattern, 2: insight, 3: algorithm, 4: complexity, 5: watchout

        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("1.") || lower.contains("pattern:") -> {
                    currentSection = 1
                    pattern = line.substringAfter(":").trim()
                }
                lower.startsWith("2.") || lower.contains("key insight:") || lower.contains("insight:") -> {
                    currentSection = 2
                    keyInsight = line.substringAfter(":").trim()
                }
                lower.startsWith("3.") || lower.contains("algorithm:") -> {
                    currentSection = 3
                    algorithm = line.substringAfter(":").trim()
                }
                lower.startsWith("4.") || lower.contains("complexity:") -> {
                    currentSection = 4
                    val content = line.substringAfter(":").trim()
                    if (content.isNotEmpty()) {
                        complexityStr.append(content).append("\n")
                    }
                }
                lower.startsWith("5.") || lower.contains("watch out for:") || lower.contains("watch out:") -> {
                    currentSection = 5
                    watchOut = line.substringAfter(":").trim()
                }
                line.isNotEmpty() -> {
                    when (currentSection) {
                        1 -> pattern += " " + line
                        2 -> keyInsight += " " + line
                        3 -> algorithm += " " + line
                        4 -> complexityStr.append(line).append("\n")
                        5 -> watchOut += " " + line
                    }
                }
            }
        }

        // Clean up parsed output if parts of the list identifier got mixed in
        pattern = pattern.removePrefix("1.").removePrefix("Pattern:").trim()
        keyInsight = keyInsight.removePrefix("2.").removePrefix("Key insight:").removePrefix("Key Insight:").trim()
        algorithm = algorithm.removePrefix("3.").removePrefix("Algorithm:").trim()
        val complexity = complexityStr.toString().removePrefix("4.").removePrefix("Complexity:").trim()
        watchOut = watchOut.removePrefix("5.").removePrefix("Watch out for:").removePrefix("Watch out:").trim()

        if (pattern.isEmpty() && keyInsight.isEmpty() && algorithm.isEmpty() && complexity.isEmpty() && watchOut.isEmpty()) {
            return AIHintResult(
                pattern = "General Pattern",
                keyInsight = rawResponse,
                algorithm = "Please follow the guidelines standard steps.",
                complexity = "Time O(?) | Space O(?)",
                watchOut = "Check constraints carefully."
            )
        }

        return AIHintResult(
            pattern = pattern.ifEmpty { "General Pattern" },
            keyInsight = keyInsight.ifEmpty { "Refer to detailed guidelines" },
            algorithm = algorithm.ifEmpty { "Refer to detailed steps" },
            complexity = complexity.ifEmpty { "Time O(?) | Space O(?)" },
            watchOut = watchOut.ifEmpty { "No significant edge case provided" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
