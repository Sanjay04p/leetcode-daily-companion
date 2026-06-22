package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProblemEntry
import com.example.ui.LeetCodeViewModel
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: LeetCodeViewModel
) {
    val problems by viewModel.problems.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()

    // 1. Total split
    val totalCount = problems.size
    val solvedCount = remember(problems) { problems.count { it.status == "SOLVED" } }
    val attemptedCount = remember(problems) { problems.count { it.status == "ATTEMPTED" } }
    val skippedCount = remember(problems) { problems.count { it.status == "SKIPPED" } }

    val easyCount = remember(problems) { problems.count { it.difficulty == "EASY" } }
    val mediumCount = remember(problems) { problems.count { it.difficulty == "MEDIUM" } }
    val hardCount = remember(problems) { problems.count { it.difficulty == "HARD" } }

    // 2. Heatmap dates calculation (Last 30 Days)
    val last30DaysStats = remember(problems) {
        val todayEpoch = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / (24 * 60 * 60 * 1000L)

        val daysMap = mutableMapOf<Long, Int>() // epochDay to SOLVED count
        problems.filter { it.status == "SOLVED" }.forEach {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateLogged }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val pEpoch = cal.timeInMillis / (24 * 60 * 60 * 1000L)
            daysMap[pEpoch] = (daysMap[pEpoch] ?: 0) + 1
        }

        (29 downTo 0).map { offset ->
            val day = todayEpoch - offset
            val count = daysMap[day] ?: 0
            count
        }
    }

    // 3. Top 5 Tags calculations
    val top5Tags = remember(problems) {
        val countMap = mutableMapOf<String, Int>()
        problems.forEach { item ->
            item.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tag ->
                    countMap[tag] = (countMap[tag] ?: 0) + 1
                }
        }
        countMap.entries
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .take(5)
    }
    val maxTagCount = remember(top5Tags) {
        top5Tags.firstOrNull()?.second ?: 1
    }

    // 4. Avg Time by Difficulty
    val avgTimeEasy = remember(problems) {
        val list = problems.filter { it.difficulty == "EASY" }
        if (list.isEmpty()) 0 else list.sumOf { it.timeTakenSeconds } / list.size
    }
    val avgTimeMedium = remember(problems) {
        val list = problems.filter { it.difficulty == "MEDIUM" }
        if (list.isEmpty()) 0 else list.sumOf { it.timeTakenSeconds } / list.size
    }
    val avgTimeHard = remember(problems) {
        val list = problems.filter { it.difficulty == "HARD" }
        if (list.isEmpty()) 0 else list.sumOf { it.timeTakenSeconds } / list.size
    }
    val maxAvgTimeSeconds = remember(avgTimeEasy, avgTimeMedium, avgTimeHard) {
        maxOf(avgTimeEasy, avgTimeMedium, avgTimeHard, 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Insights", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Streak Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Streak", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$currentStreak Days", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Longest Streak", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$longestStreak Days", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                    }
                }
            }

            // Total problems split breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Total Problems Logged", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$totalCount",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryPurple,
                            modifier = Modifier.padding(end = 24.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            StatusProgressRow("Solved", solvedCount, AccentGreen)
                            StatusProgressRow("Attempted", attemptedCount, AccentAmber)
                            StatusProgressRow("Skipped", skippedCount, NeutralGray)
                        }
                    }
                }
            }

            // Difficulty Cards (color-coded metric cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DifficultyMetricCard("Easy", easyCount, AccentGreen, modifier = Modifier.weight(1f))
                DifficultyMetricCard("Medium", mediumCount, AccentAmber, modifier = Modifier.weight(1f))
                DifficultyMetricCard("Hard", hardCount, AccentRed, modifier = Modifier.weight(1f))
            }

            // Activity Heatmap Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Activity Heatmap (Last 30 Days)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Draw grid layout: 10 columns by 3 rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (row in 0 until 3) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (col in 0 until 10) {
                                    val index = row * 10 + col
                                    val count = last30DaysStats[index]

                                    val color = when {
                                        count == 0 -> Color(0xFF2C2C2E)
                                        count == 1 -> Color(0xFF004D40)
                                        count == 2 || count == 3 -> Color(0xFF00796B)
                                        else -> AccentGreen // 4+
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .background(color, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Less", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        listOf(Color(0xFF2C2C2E), Color(0xFF004D40), Color(0xFF00796B), AccentGreen).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("More", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            // Top 5 Tags Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Top 5 Tags By Problem Count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    if (top5Tags.isEmpty()) {
                        Text("Log problems with tags to generate graph.", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        top5Tags.forEach { (tag, count) ->
                            val percent = count.toFloat() / maxTagCount.toFloat()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.width(100.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .background(Color(0xFF2C2C2E), RoundedCornerShape(7.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(percent)
                                            .background(PrimaryPurple, RoundedCornerShape(7.dp))
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "$count",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Avg Time by Difficulty Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Avg Solve Time By Difficulty", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    DifficultyAvgTimeBar("Easy", avgTimeEasy, AccentGreen, maxAvgTimeSeconds)
                    DifficultyAvgTimeBar("Medium", avgTimeMedium, AccentAmber, maxAvgTimeSeconds)
                    DifficultyAvgTimeBar("Hard", avgTimeHard, AccentRed, maxAvgTimeSeconds)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun StatusProgressRow(status: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(status, fontSize = 13.sp, color = TextSecondary)
        }

        Text(
            "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
fun DifficultyMetricCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$count", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        }
    }
}

@Composable
fun DifficultyAvgTimeBar(label: String, avgSeconds: Int, color: Color, maxAvgTimeSeconds: Int) {
    val percent = (avgSeconds.toFloat() / maxAvgTimeSeconds.toFloat()).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Text(
                text = if (avgSeconds > 0) formatAvgTime(avgSeconds) else "N/A",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent)
                    .background(color, RoundedCornerShape(5.dp))
            )
        }
    }
}

fun formatAvgTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
