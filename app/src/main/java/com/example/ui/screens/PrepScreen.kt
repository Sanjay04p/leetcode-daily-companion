package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InterviewQuestion
import com.example.ui.LeetCodeViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepScreen(
    viewModel: LeetCodeViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val resumeProfile by viewModel.resumeProfile.collectAsStateWithLifecycle()
    val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()

    // 3 sub-screens choosing via top tab row
    var selectedTopTab by remember { mutableStateOf(0) } // 0 = Resume, 1 = Questions, 2 = Mock Interview
    val tabs = listOf("Resume", "Questions", "Mock Interview")

    // Question Detail State
    var selectedQuestionForDetail by remember { mutableStateOf<InterviewQuestion?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    // If mock interview is active, we render full screen mock, hiding standard tabs!
    if (viewModel.isMockActive) {
        MockInterviewActiveView(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(BackgroundDark)) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = PrimaryPurple
                                )
                                Text(
                                    text = "Interview Prep",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = TextPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = BackgroundDark,
                            titleContentColor = TextPrimary
                        )
                    )

                    // Horizontal top tab row
                    TabRow(
                        selectedTabIndex = selectedTopTab,
                        containerColor = BackgroundDark,
                        contentColor = PrimaryPurple,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTopTab]),
                                color = PrimaryPurple
                            )
                        },
                        divider = {
                            HorizontalDivider(color = BorderColor)
                        }
                    ) {
                        tabs.forEachIndexed { index, label ->
                            Tab(
                                selected = selectedTopTab == index,
                                onClick = { selectedTopTab = index },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selectedTopTab == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                },
                                selectedContentColor = PrimaryPurple,
                                unselectedContentColor = TextSecondary
                            )
                        }
                    }
                }
            },
            containerColor = BackgroundDark
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BackgroundDark)
            ) {
                when (selectedTopTab) {
                    0 -> ResumeSubScreen(
                        viewModel = viewModel,
                        onNavigateToQuestions = { selectedTopTab = 1 }
                    )
                    1 -> QuestionsSubScreen(
                        viewModel = viewModel,
                        onQuestionClick = { q ->
                            selectedQuestionForDetail = q
                            showBottomSheet = true
                        }
                    )
                    2 -> MockInterviewSubScreen(viewModel = viewModel)
                }
            }

            // Bottom Sheet for Question Detail View
            if (showBottomSheet && selectedQuestionForDetail != null) {
                val q = selectedQuestionForDetail!!
                // Reactively bind question contents to state because updates inside sheet affect fields
                val freshQ = allQuestions.find { it.id == q.id } ?: q

                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                        selectedQuestionForDetail = null
                    },
                    sheetState = sheetState,
                    containerColor = CardBackground,
                    contentColor = TextPrimary,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = NeutralGray) }
                ) {
                    QuestionDetailBottomSheetContent(
                        question = freshQ,
                        viewModel = viewModel,
                        onDismiss = {
                            showBottomSheet = false
                            selectedQuestionForDetail = null
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// PREP SUB-SCREEN 1 — RESUME
// ==========================================
@Composable
fun ResumeSubScreen(
    viewModel: LeetCodeViewModel,
    onNavigateToQuestions: () -> Unit
) {
    val resumeProfile by viewModel.resumeProfile.collectAsStateWithLifecycle()
    var isEditingText by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Active display depending on state
        if (resumeProfile == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = PrimaryPurple.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "Prepare from Your Resume",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Add your professional resume to generate exactly 25 custom, highly-relevant interview questions structured for your tech stack & projects.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            val profile = resumeProfile!!
            if (!isEditingText) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = "Your Resume Profile",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val formattedTime = remember(profile.lastUpdated) {
                                        val date = java.util.Date(profile.lastUpdated)
                                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                                        sdf.format(date)
                                    }
                                    Text(
                                        text = "Last updated: $formattedTime",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { isEditingText = true },
                                        modifier = Modifier
                                            .background(BorderColor, CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Resume Text",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteResume() },
                                        modifier = Modifier
                                            .background(
                                                AccentRed.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Resume",
                                            tint = AccentRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(color = BorderColor)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Extract and show first 4 lines in preview card
                            val previewLines = remember(profile.resumeText) {
                                profile.resumeText.lines().take(4)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                previewLines.forEach { line ->
                                    if (line.isNotBlank()) {
                                        Text(
                                            text = "• ${line.trim()}",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (profile.resumeText.lines().size > 4) {
                                    Text(
                                        text = "+ ${profile.resumeText.lines().size - 4} more lines...",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Paste and Generate controls (shown in empty state or when editing)
        if (resumeProfile == null || isEditingText) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resume Content",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        if (isEditingText && resumeProfile != null) {
                            Text(
                                text = "Cancel Edit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AccentRed,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.resumeInputText = resumeProfile?.resumeText ?: ""
                                        isEditingText = false
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.resumeInputText,
                        onValueChange = { viewModel.resumeInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 350.dp),
                        placeholder = {
                            Text(
                                text = "Paste your full resume text here — work experience, projects, skills, education, headers ...",
                                fontSize = 13.sp,
                                color = NeutralGray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }
            }
        }

        // Action button for generating questions
        item {
            Spacer(modifier = Modifier.height(10.dp))
            if (viewModel.isGeneratingQuestions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryPurple,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = when (viewModel.prepGenerationStep) {
                                LeetCodeViewModel.PrepGenerationStep.STEP1_RUNNING -> "Generating project & technical questions... (1/2)"
                                LeetCodeViewModel.PrepGenerationStep.STEP2_RUNNING -> "Generating behavioral & HR questions... (2/2)"
                                else -> "Analyzing your resume... ✦"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                val isFailed = viewModel.prepGenerationStep == LeetCodeViewModel.PrepGenerationStep.STEP1_FAILED ||
                               viewModel.prepGenerationStep == LeetCodeViewModel.PrepGenerationStep.STEP2_FAILED
                if (isFailed) {
                    Button(
                        onClick = {
                            viewModel.retryFailedGenerationStep {
                                onNavigateToQuestions()
                            }
                            isEditingText = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        enabled = viewModel.resumeInputText.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.prepGenerationStep == LeetCodeViewModel.PrepGenerationStep.STEP1_FAILED) "Retry Step 1 (Project & Technical)" else "Retry Step 2 (Behavioral & HR)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.generateQuestionsFromResume {
                                onNavigateToQuestions()
                            }
                            isEditingText = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                        border = BorderStroke(1.dp, PrimaryPurple),
                        enabled = viewModel.resumeInputText.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Over Completely",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.generateQuestionsFromResume {
                                onNavigateToQuestions()
                            }
                            isEditingText = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        enabled = viewModel.resumeInputText.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Interview Questions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Display active errors nicely
            if (viewModel.generateError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error Icon",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.generateError ?: "",
                            fontSize = 12.sp,
                            color = AccentRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREP SUB-SCREEN 2 — QUESTIONS
// ==========================================
@Composable
fun QuestionsSubScreen(
    viewModel: LeetCodeViewModel,
    onQuestionClick: (InterviewQuestion) -> Unit
) {
    val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()

    val categories = listOf("All", "Project", "Technical", "Behavioral", "HR")
    val stats = listOf("All", "Practiced", "Not Practiced")

    // Collect counts reactively
    val totalCount = allQuestions.size
    val practicedCount = remember(allQuestions) { allQuestions.count { it.isPracticed } }

    // Combine filters statically/reactively
    val filteredList = remember(allQuestions, viewModel.prepCategoryFilter, viewModel.prepStatusFilter) {
        allQuestions.filter { q ->
            val catMatch = if (viewModel.prepCategoryFilter == "All") true else q.category.equals(viewModel.prepCategoryFilter, ignoreCase = true)
            val stateMatch = when (viewModel.prepStatusFilter) {
                "Practiced" -> q.isPracticed
                "Not Practiced" -> !q.isPracticed
                else -> true
            }
            catMatch && stateMatch
        }
    }

    if (allQuestions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = NeutralGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No questions generated yet",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Go to the 'Resume' tab and insert your profile to generate specialized interview questions.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = viewModel.prepCategoryFilter == category,
                        onClick = { viewModel.prepCategoryFilter = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryPurple,
                            selectedLeadingIconColor = PrimaryPurple,
                            containerColor = CardBackground,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewModel.prepCategoryFilter == category,
                            borderColor = if (viewModel.prepCategoryFilter == category) PrimaryPurple else BorderColor,
                            selectedBorderColor = PrimaryPurple,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            // Status Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(stats) { stat ->
                    FilterChip(
                        selected = viewModel.prepStatusFilter == stat,
                        onClick = { viewModel.prepStatusFilter = stat },
                        label = { Text(stat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryPurple,
                            containerColor = CardBackground,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = viewModel.prepStatusFilter == stat,
                            borderColor = if (viewModel.prepStatusFilter == stat) PrimaryPurple else BorderColor,
                            selectedBorderColor = PrimaryPurple,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            // Summary badge row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalCount questions • $practicedCount practiced",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
                Text(
                    text = "${filteredList.size} displayed",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Scrollable Question Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { question ->
                    QuestionCardItem(
                        question = question,
                        onClick = { onQuestionClick(question) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionCardItem(
    question: InterviewQuestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(category = question.category)
                    DifficultyBadge(difficulty = question.difficulty)
                }

                if (question.isPracticed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Practiced",
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        if (question.selfRating > 0) {
                            Text(
                                text = "${question.selfRating}★",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Question Text
            Text(
                text = question.question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Source Project tag if any
            if (!question.sourceProject.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(PrimaryPurple.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Project: ${question.sourceProject}",
                        fontSize = 11.sp,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// PREP SUB-SCREEN 3 — MOCK INTERVIEW
// ==========================================
@Composable
fun MockInterviewSubScreen(viewModel: LeetCodeViewModel) {
    val allQuestions by viewModel.allQuestions.collectAsStateWithLifecycle()
    val optionsCategory = listOf("All", "Project", "Technical", "Behavioral", "HR")
    val optionsCount = listOf("5 questions", "10 questions", "All unpracticed")

    if (allQuestions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Laptop,
                contentDescription = null,
                tint = NeutralGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mock Interview workspace is empty",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add and index a resume to unlock the simulated custom Mock Interview board.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else if (viewModel.mockSessionResults != null) {
        MockCompletionView(viewModel = viewModel)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = AccentAmber.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LaptopMac,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "Mock Interview Sandbox",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Simulate a real coding or systems engineering technical interview session. Customize tags, answer under constraints, and score yourself.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Configure Session Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        // Option 1: Category selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Category Focus:",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                optionsCategory.take(3).forEach { cat ->
                                    val isSelected = viewModel.mockSelectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryPurple else BorderColor)
                                            .clickable { viewModel.mockSelectedCategory = cat }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                optionsCategory.drop(3).forEach { cat ->
                                    val isSelected = viewModel.mockSelectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryPurple else BorderColor)
                                            .clickable { viewModel.mockSelectedCategory = cat }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f)) // placeholder spacer to align row
                            }
                        }

                        // Option 2: Count selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Questions Count / Selection:",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            optionsCount.forEach { opt ->
                                val isSelected = viewModel.mockSelectedCountOption == opt
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) PrimaryPurple else BorderColor,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.mockSelectedCountOption = opt }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) PrimaryPurple else TextPrimary
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.mockSelectedCountOption = opt },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = PrimaryPurple,
                                            unselectedColor = NeutralGray
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.startMockInterview() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Mock Interview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// ACTIVE MOCK INTERVIEW EXPERIENCE (FULL-SCREEN MODE)
// ==========================================
@Composable
fun MockInterviewActiveView(viewModel: LeetCodeViewModel) {
    val q = viewModel.mockQuestions.getOrNull(viewModel.currentMockQuestionIndex)

    if (q == null) {
        // Safe fallback
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryPurple)
        }
        return
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardBackground,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.finishMockSession() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Quit Interview",
                            tint = AccentRed
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatSeconds(viewModel.mockTimerSeconds),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AccentAmber
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress tracker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Question ${viewModel.currentMockQuestionIndex + 1} of ${viewModel.mockQuestions.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${((viewModel.currentMockQuestionIndex + 1).toFloat() / viewModel.mockQuestions.size * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                LinearProgressIndicator(
                    progress = { (viewModel.currentMockQuestionIndex + 1).toFloat() / viewModel.mockQuestions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryPurple,
                    trackColor = BorderColor
                )
            }

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryChip(category = q.category)
                        DifficultyBadge(difficulty = q.difficulty)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Input / Suggested Answer Toggle Card space
            Box(modifier = Modifier.weight(1f)) {
                if (!viewModel.isMockAnswerRevealed) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Draft your answer / notes:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = viewModel.mockUserAnswer,
                            onValueChange = { viewModel.mockUserAnswer = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            placeholder = {
                                Text(
                                    "Type bullet points, pseudocode or full paragraph answers here...",
                                    fontSize = 12.sp,
                                    color = NeutralGray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Suggested Answer Outline:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple
                            )
                            Text(
                                text = q.suggestedAnswer,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextPrimary
                            )

                            if (viewModel.mockUserAnswer.isNotBlank()) {
                                HorizontalDivider(color = BorderColor)
                                Text(
                                    text = "Your drafted answer:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = viewModel.mockUserAnswer,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions Drawer
            if (!viewModel.isMockAnswerRevealed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.skipMockQuestion() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Skip →",
                            fontWeight = FontWeight.Bold,
                            color = AccentRed,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.isMockAnswerRevealed = true },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(
                            text = "Reveal Suggested Answer",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CardBackground,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "How well did you answer?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        StarRatingRow(
                            rating = viewModel.mockCurrentRating,
                            onRatingSelected = { viewModel.mockCurrentRating = it },
                            starSize = 36.dp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = {
                                viewModel.submitMockQuestionAnswer(viewModel.mockCurrentRating)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            Text(
                                text = "Next Question →",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SESSION COMPLETION & SUMMARY SCREEN View
// ==========================================
@Composable
fun MockCompletionView(viewModel: LeetCodeViewModel) {
    val results = viewModel.mockSessionResults ?: return
    var showQAReview by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Session Complete! 🎉",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = AccentGreen
                )
                Text(
                    text = "A perfect milestone in interview preparedness.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Stats overview card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Performance Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Answered", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "${results.answeredCount}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                        Column {
                            Text("Skipped", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "${results.skippedCount}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                        }
                        Column {
                            Text("Avg Rating", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                String.format("%.1f ★", results.averageRating),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    // Specific breakdown per Category in session
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val cats = results.attempts.map { it.question.category }.distinct()
                        cats.forEach { category ->
                            val list = results.attempts.filter { it.question.category == category }
                            val answeredCount = list.count { !it.skipped }
                            val skipCount = list.count { it.skipped }
                            val totalMap = list.size

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryChip(category = category)
                                Text(
                                    text = "$answeredCount ans, $skipCount skip ($totalMap total)",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded Scrollable Q&A review list
        if (showQAReview) {
            item {
                Text(
                    text = "Q&A Session Audit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(results.attempts) { attempt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryChip(category = attempt.question.category)
                            if (attempt.skipped) {
                                Text(
                                    "SKIPPED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentRed
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Scored: ${attempt.rating}★",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentAmber
                                    )
                                }
                            }
                        }

                        Text(
                            text = attempt.question.question,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )

                        if (!attempt.skipped) {
                            if (attempt.userAnswer.isNotBlank()) {
                                Text(
                                    text = "Your Answer: ${attempt.userAnswer}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Suggested: ${attempt.question.suggestedAnswer}",
                                fontSize = 12.sp,
                                color = PrimaryPurple,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showQAReview = !showQAReview },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrimaryPurple)
                ) {
                    Text(
                        text = if (showQAReview) "Hide Answers" else "Review Answers",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.mockSessionResults = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(
                        text = "Practice Again",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// DETAILED MODAL BOTTOM SHEET CONTENT
// ==========================================
@Composable
fun QuestionDetailBottomSheetContent(
    question: InterviewQuestion,
    viewModel: LeetCodeViewModel,
    onDismiss: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(question.userAnswer ?: "") }
    var starRating by remember { mutableStateOf(question.selfRating) }
    var practicedState by remember { mutableStateOf(question.isPracticed) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question Header
        Text(
            text = question.question,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(category = question.category)
            DifficultyBadge(difficulty = question.difficulty)

            if (question.sourceProject != null) {
                Text(
                    text = "Project: ${question.sourceProject}",
                    fontSize = 11.sp,
                    color = PrimaryPurple,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(PrimaryPurple.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = BorderColor)

        // Reveal Toggle Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BorderColor)
                .clickable { revealed = !revealed }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (revealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (revealed) "Suggested Answer" else "Reveal Answer ▼",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = if (revealed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = NeutralGray
            )
        }

        AnimatedVisibility(
            visible = revealed,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BorderColor.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = question.suggestedAnswer,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextPrimary
                    )

                    // Regenerate answer link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (viewModel.isRegeneratingAnswer) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PrimaryPurple,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Regenerate this answer",
                                fontSize = 11.sp,
                                color = PrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.regenerateAnswer(question.id, question.question) {
                                            // Completion callback
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Custom Notes Area
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Your answer / notes:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                placeholder = {
                    Text(
                        "Draft your notes, facts, and STAR stories here...",
                        fontSize = 12.sp,
                        color = NeutralGray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 13.sp)
            )
        }

        // Practiced Toggle Checkbox and ratings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Checkbox(
                    checked = practicedState,
                    onCheckedChange = { practicedState = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentGreen,
                        uncheckedColor = NeutralGray
                    )
                )
                Text(
                    text = "Mark as Practiced",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (practicedState) {
                StarRatingRow(
                    rating = starRating,
                    onRatingSelected = { starRating = it },
                    starSize = 24.dp
                )
            }
        }

        // Save Notes Action
        Button(
            onClick = {
                viewModel.updateQuestionNotesAndRating(
                    questionId = question.id,
                    userAnswer = noteText,
                    selfRating = starRating,
                    isPracticed = practicedState
                )
                viewModel.triggerSnackbar("Response saved successfully.")
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text("Save Response Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
    }
}

// ==========================================
// DECORATIVE REUSABLE COMPOSABLES
// ==========================================
@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    val upper = category.uppercase()
    val containerColor = when (upper) {
        "PROJECT" -> Color(0xFF2196F3).copy(alpha = 0.12f)
        "TECHNICAL" -> AccentAmber.copy(alpha = 0.12f)
        "BEHAVIORAL" -> AccentGreen.copy(alpha = 0.12f)
        "HR" -> NeutralGray.copy(alpha = 0.15f)
        else -> BorderColor
    }
    val contentColor = when (upper) {
        "PROJECT" -> Color(0xFF2196F3)
        "TECHNICAL" -> AccentAmber
        "BEHAVIORAL" -> AccentGreen
        "HR" -> TextSecondary
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.replaceFirstChar { it.titlecase() },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun DifficultyBadge(difficulty: String, modifier: Modifier = Modifier) {
    val desc = difficulty.uppercase()
    val color = when (desc) {
        "EASY" -> AccentGreen
        "MEDIUM" -> AccentAmber
        "HARD" -> AccentRed
        else -> NeutralGray
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = desc,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
fun StarRatingRow(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 28.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i stars",
                tint = if (i <= rating) AccentAmber else NeutralGray,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onRatingSelected(i) }
            )
        }
    }
}

// Helpers for formatter
fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
