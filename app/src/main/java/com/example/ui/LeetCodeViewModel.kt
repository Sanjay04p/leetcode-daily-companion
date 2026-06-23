package com.example.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProblemEntry
import com.example.data.ProblemRepository
import com.example.data.ResumeProfile
import com.example.data.InterviewQuestion
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

data class MockAttempt(
    val question: InterviewQuestion,
    val userAnswer: String,
    val rating: Int,
    val isRevealed: Boolean,
    val skipped: Boolean,
    val timeTakenSeconds: Int
)

data class MockSessionResults(
    val answeredCount: Int,
    val skippedCount: Int,
    val averageRating: Float,
    val attempts: List<MockAttempt>
)

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

    // --- INTERVIEW PREPARATION DATA AND FLOWS ---
    val resumeProfile: StateFlow<ResumeProfile?> = repository.resumeProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allQuestions: StateFlow<List<InterviewQuestion>> = repository.allQuestions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Sub-Screen 1: Resume Profile Inputs and State
    var resumeInputText by mutableStateOf("")
    var isGeneratingQuestions by mutableStateOf(false)
        private set
    var generateError by mutableStateOf<String?>(null)
    var isRegeneratingAnswer by mutableStateOf(false)
        private set

    // Sub-Screen 2: Questions Filtering Status
    var prepCategoryFilter by mutableStateOf("All")
    var prepStatusFilter by mutableStateOf("All")

    // Sub-Screen 3: Mock Interview Pre-Start Options
    var mockSelectedCategory by mutableStateOf("All")
    var mockSelectedCountOption by mutableStateOf("5 questions")

    // Active Mock State
    var isMockActive by mutableStateOf(false)
    val mockQuestions = mutableStateListOf<InterviewQuestion>()
    var currentMockQuestionIndex by mutableStateOf(0)
    var mockTimerSeconds by mutableStateOf(0)
    var mockUserAnswer by mutableStateOf("")
    var isMockAnswerRevealed by mutableStateOf(false)
    var mockCurrentRating by mutableStateOf(0)
    private var mockTimerJob: Job? = null
    val completedMockAttempts = mutableStateListOf<MockAttempt>()
    var mockSessionResults by mutableStateOf<MockSessionResults?>(null)

    // Notification channel
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    fun triggerSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    private fun cleanJson(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }
        return text.trim()
    }

    // Initialize resume text from Database if available
    init {
        viewModelScope.launch {
            resumeProfile.collectLatest { profile ->
                if (profile != null && resumeInputText.isEmpty()) {
                    resumeInputText = profile.resumeText
                }
            }
        }
    }

    fun generateQuestionsFromResume(onSuccess: () -> Unit) {
        val resumeText = resumeInputText.trim()
        if (resumeText.isEmpty()) {
            generateError = "Please paste your resume first."
            return
        }
        isGeneratingQuestions = true
        generateError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    viewModelScope.launch(Dispatchers.Main) {
                        generateError = "Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio."
                        isGeneratingQuestions = false
                    }
                    return@launch
                }

                val systemInstruction = """
                    You are a senior technical interviewer at a top tech company.
                    Analyze the resume below and generate exactly 25 interview questions the candidate should prepare for.
                    Return ONLY a valid JSON array, no markdown, no explanation. Each object:
                    {
                      "question": "string",
                      "suggestedAnswer": "string (3-5 sentences, STAR format for behavioral, technical explanation for others)",
                      "category": "PROJECT" | "TECHNICAL" | "BEHAVIORAL" | "HR",
                      "sourceProject": "string or null",
                      "difficulty": "EASY" | "MEDIUM" | "HARD"
                    }
                    Make questions specific to their actual projects, tech stack, and experience — not generic.
                    Include 8 PROJECT questions, 8 TECHNICAL questions, 6 BEHAVIORAL questions, 3 HR questions.
                """.trimIndent()

                val promptText = "Resume:\n$resumeText"

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
                            generateError = "API Server returned code ${response.code}: $responseBody"
                            isGeneratingQuestions = false
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
                            val rawText = parts.getJSONObject(0).getString("text")
                            val cleanJsonStr = cleanJson(rawText)
                            val qList = mutableListOf<InterviewQuestion>()

                            try {
                                val jsonArray = org.json.JSONArray(cleanJsonStr)
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val question = obj.optString("question", "")
                                    val suggestedAnswer = obj.optString("suggestedAnswer", "")
                                    val category = obj.optString("category", "TECHNICAL").uppercase()
                                    val sourceProject = if (obj.isNull("sourceProject")) null else obj.optString("sourceProject", null)
                                    val difficulty = obj.optString("difficulty", "MEDIUM").uppercase()

                                    if (question.isNotEmpty()) {
                                        qList.add(
                                            InterviewQuestion(
                                                question = question,
                                                suggestedAnswer = suggestedAnswer,
                                                category = category,
                                                sourceProject = sourceProject,
                                                difficulty = difficulty,
                                                dateGenerated = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    generateError = "JSON Parsing failed: ${e.message}\nRaw text structure was unexpected."
                                    isGeneratingQuestions = false
                                }
                                return@launch
                            }

                            if (qList.isEmpty()) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    generateError = "No valid questions could be extracted from JSON."
                                    isGeneratingQuestions = false
                                }
                                return@launch
                            }

                            // Save to database
                            val profile = ResumeProfile(
                                resumeText = resumeText,
                                lastUpdated = System.currentTimeMillis()
                            )
                            repository.insertResumeProfile(profile)
                            repository.clearAllQuestions()
                            repository.insertQuestions(qList)

                            viewModelScope.launch(Dispatchers.Main) {
                                isGeneratingQuestions = false
                                if (qList.size < 25) {
                                    _snackbarMessage.emit("Generated ${qList.size} questions!")
                                } else {
                                    _snackbarMessage.emit("25 questions generated!")
                                }
                                onSuccess()
                            }
                        } else {
                            viewModelScope.launch(Dispatchers.Main) {
                                generateError = "No text response parsed from generation candidates."
                                isGeneratingQuestions = false
                            }
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            generateError = "No generation candidates returned."
                            isGeneratingQuestions = false
                        }
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    generateError = "Error: ${e.message ?: "Unknown service error"}"
                    isGeneratingQuestions = false
                }
            }
        }
    }

    fun deleteResume() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteResumeProfile()
            repository.clearAllQuestions()
            viewModelScope.launch(Dispatchers.Main) {
                resumeInputText = ""
                _snackbarMessage.emit("Resume profile and questions deleted.")
            }
        }
    }

    fun regenerateAnswer(questionId: Int, questionText: String, completion: () -> Unit = {}) {
        isRegeneratingAnswer = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    viewModelScope.launch(Dispatchers.Main) {
                        isRegeneratingAnswer = false
                        _snackbarMessage.emit("Gemini API key is not configured in Secrets panel.")
                    }
                    return@launch
                }

                val resumeText = resumeProfile.value?.resumeText ?: ""
                val resumeExcerpt = if (resumeText.length > 500) resumeText.take(500) else resumeText

                val systemInstruction = "You are a senior interviewer. Regenerate a better suggested answer for the given interview question. Keep it 3-5 sentences, specific and concrete."
                val promptText = """
                    Regenerate a better suggested answer for this interview question: $questionText
                    Candidate background excerpt: $resumeExcerpt
                """.trimIndent()

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
                        viewModelScope.launch(Dispatchers.Main) {
                            isRegeneratingAnswer = false
                            _snackbarMessage.emit("API call failed (code ${response.code})")
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
                            val newAnswer = parts.getJSONObject(0).getString("text").trim()
                            
                            val currentList = allQuestions.value
                            val questionToUpdate = currentList.find { it.id == questionId }
                            if (questionToUpdate != null) {
                                val updated = questionToUpdate.copy(suggestedAnswer = newAnswer)
                                repository.updateQuestion(updated)
                            }

                            viewModelScope.launch(Dispatchers.Main) {
                                isRegeneratingAnswer = false
                                _snackbarMessage.emit("Suggested answer regenerated!")
                                completion()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    isRegeneratingAnswer = false
                    _snackbarMessage.emit("Error: ${e.message}")
                }
            }
        }
    }

    fun updateQuestionNotesAndRating(questionId: Int, userAnswer: String, selfRating: Int, isPracticed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = allQuestions.value
            val questionToUpdate = currentList.find { it.id == questionId }
            if (questionToUpdate != null) {
                val updated = questionToUpdate.copy(
                    userAnswer = userAnswer.ifBlank { null },
                    selfRating = selfRating,
                    isPracticed = isPracticed
                )
                repository.updateQuestion(updated)
            }
        }
    }

    // --- MOCK INTERVIEW ACTIONS ---
    fun startMockInterview() {
        val allQ = allQuestions.value
        val filtered = allQ.filter { q ->
            val catMatch = if (mockSelectedCategory == "All") true else q.category.equals(mockSelectedCategory, ignoreCase = true)
            val stateMatch = if (mockSelectedCountOption == "All unpracticed") !q.isPracticed else true
            catMatch && stateMatch
        }.shuffled()

        val targetCount = when (mockSelectedCountOption) {
            "5 questions" -> 5
            "10 questions" -> 10
            else -> filtered.size
        }

        val selected = filtered.take(targetCount)
        if (selected.isEmpty()) {
            triggerSnackbar("No questions matched your parameters!")
            return
        }

        isMockActive = true
        mockQuestions.clear()
        mockQuestions.addAll(selected)
        currentMockQuestionIndex = 0
        mockSessionResults = null
        completedMockAttempts.clear()
        setupCurrentMockQuestion()
    }

    fun setupCurrentMockQuestion() {
        mockUserAnswer = ""
        isMockAnswerRevealed = false
        mockCurrentRating = 0
        mockTimerSeconds = 0
        startMockQuestionTimer()
    }

    fun startMockQuestionTimer() {
        mockTimerJob?.cancel()
        mockTimerJob = viewModelScope.launch {
            while (isMockActive) {
                delay(1000)
                mockTimerSeconds++
            }
        }
    }

    fun stopMockQuestionTimer() {
        mockTimerJob?.cancel()
    }

    fun skipMockQuestion() {
        stopMockQuestionTimer()
        val currentQ = mockQuestions.getOrNull(currentMockQuestionIndex) ?: return
        completedMockAttempts.add(
            MockAttempt(
                question = currentQ,
                userAnswer = "",
                rating = 0,
                isRevealed = false,
                skipped = true,
                timeTakenSeconds = mockTimerSeconds
            )
        )
        advanceMockQuestion()
    }

    fun submitMockQuestionAnswer(rating: Int) {
        stopMockQuestionTimer()
        val currentQ = mockQuestions.getOrNull(currentMockQuestionIndex) ?: return
        val finalRating = if (rating > 0) rating else 3 // Fallback if rating was unchanged but submitted

        viewModelScope.launch(Dispatchers.IO) {
            val updated = currentQ.copy(
                isPracticed = true,
                selfRating = finalRating,
                userAnswer = mockUserAnswer.ifBlank { null }
            )
            repository.updateQuestion(updated)
        }

        completedMockAttempts.add(
            MockAttempt(
                question = currentQ,
                userAnswer = mockUserAnswer,
                rating = finalRating,
                isRevealed = true,
                skipped = false,
                timeTakenSeconds = mockTimerSeconds
            )
        )
        advanceMockQuestion()
    }

    fun advanceMockQuestion() {
        if (currentMockQuestionIndex + 1 < mockQuestions.size) {
            currentMockQuestionIndex++
            setupCurrentMockQuestion()
        } else {
            finishMockSession()
        }
    }

    fun finishMockSession() {
        isMockActive = false
        stopMockQuestionTimer()

        val answered = completedMockAttempts.filter { !it.skipped }
        val skipped = completedMockAttempts.filter { it.skipped }
        val avgRating = if (answered.isNotEmpty()) answered.map { it.rating }.average().toFloat() else 0.0f

        mockSessionResults = MockSessionResults(
            answeredCount = answered.size,
            skippedCount = skipped.size,
            averageRating = avgRating,
            attempts = completedMockAttempts.toList()
        )
    }

    fun start5MinMock() {
        mockSelectedCategory = "All"
        mockSelectedCountOption = "All unpracticed"

        val allQ = allQuestions.value
        val filtered = allQ.filter { !it.isPracticed }.shuffled()
        var targetCount = filtered.take(5)
        if (targetCount.isEmpty()) {
            // fallback to any 5 questions if none are unpracticed
            targetCount = allQ.shuffled().take(5)
        }

        if (targetCount.isEmpty()) {
            triggerSnackbar("Please generate interview questions first!")
            return
        }

        isMockActive = true
        mockQuestions.clear()
        mockQuestions.addAll(targetCount)
        currentMockQuestionIndex = 0
        mockSessionResults = null
        completedMockAttempts.clear()
        setupCurrentMockQuestion()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
