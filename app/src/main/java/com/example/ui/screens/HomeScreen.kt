package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProblemEntry
import com.example.ui.LeetCodeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LeetCodeViewModel,
    onNavigateToLog: () -> Unit,
    onNavigateToAIHint: () -> Unit
) {
    val problems by viewModel.problems.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()

    // Date computation for timezone
    val todayDateStr = remember {
        SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    val todayEpoch = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    // Filter problems logged today
    val todayProblems = remember(problems) {
        problems.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.dateLogged
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val problemEpoch = cal.timeInMillis / (24 * 60 * 60 * 1000L)
            problemEpoch == todayEpoch
        }
    }

    val todaySolvedCount = remember(todayProblems) {
        todayProblems.filter { it.status == "SOLVED" }.size
    }

    val todayTotalSeconds = remember(todayProblems) {
        todayProblems.sumOf { it.timeTakenSeconds }
    }

    val todayEasyCount = remember(todayProblems) {
        todayProblems.count { it.difficulty == "EASY" }
    }
    val todayMediumCount = remember(todayProblems) {
        todayProblems.count { it.difficulty == "MEDIUM" }
    }
    val todayHardCount = remember(todayProblems) {
        todayProblems.count { it.difficulty == "HARD" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Companion",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = todayDateStr,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAIHint,
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("ai_hint_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "AI Hint",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Hint ✦", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Streak Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = AccentAmber.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = if (streak > 0) "$streak Day Streak" else "Let's Start a Streak!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                        Text(
                            text = if (streak > 0) "Keep the momentum going!" else "Solve at least 1 problem to activate.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Today's Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Today's Summary",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Solved Today", fontSize = 12.sp, color = TextSecondary)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$todaySolvedCount",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Problems", fontSize = 14.sp, color = TextSecondary)
                            }
                        }

                        Column {
                            Text("Total Time spent", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                formatTime(todayTotalSeconds),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DifficultyChip(count = todayEasyCount, difficulty = "EASY")
                        DifficultyChip(count = todayMediumCount, difficulty = "MEDIUM")
                        DifficultyChip(count = todayHardCount, difficulty = "HARD")
                    }
                }
            }

            // Today's Problems Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Today's Problems",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (todayProblems.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No problems logged yet. Let's grind 💪",
                                fontSize = 16.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = onNavigateToLog,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("log_problem_cta")
                            ) {
                                Text("Log a Problem", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todayProblems) { problem ->
                            TodayProblemCard(problem)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun DifficultyChip(count: Int, difficulty: String) {
    val (label, color) = when (difficulty) {
        "EASY" -> "Easy" to AccentGreen
        "MEDIUM" -> "Med" to AccentAmber
        else -> "Hard" to AccentRed
    }

    Row(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun TodayProblemCard(problem: ProblemEntry) {
    val difficultyColor = when (problem.difficulty) {
        "EASY" -> AccentGreen
        "MEDIUM" -> AccentAmber
        else -> AccentRed
    }

    val statusIcon = when (problem.status) {
        "SOLVED" -> Icons.Default.CheckCircle to AccentGreen
        "ATTEMPTED" -> Icons.Default.Schedule to AccentAmber
        else -> Icons.Default.Info to NeutralGray
    }

    val statusText = when (problem.status) {
        "SOLVED" -> "Solved"
        "ATTEMPTED" -> "Attempted"
        else -> "Skipped"
    }

    Card(
        modifier = Modifier
            .width(260.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${problem.leetcodeNumber}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                // Difficulty Pill
                Box(
                    modifier = Modifier
                        .background(
                            color = difficultyColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = problem.difficulty,
                        color = difficultyColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = problem.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon.first,
                        contentDescription = null,
                        tint = statusIcon.second,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = statusIcon.second
                    )
                }

                Text(
                    text = formatTime(problem.timeTakenSeconds),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    if (seconds <= 0) return "0s"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
