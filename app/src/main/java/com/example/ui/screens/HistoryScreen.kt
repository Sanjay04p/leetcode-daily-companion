package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: LeetCodeViewModel
) {
    val historyList by viewModel.historyProblems.collectAsState()

    var selectedProblemForDetail by remember { mutableStateOf<ProblemEntry?>(null) }
    var problemToDelete by remember { mutableStateOf<ProblemEntry?>(null) }

    // Combine preset filters + individual tags
    val filters = remember {
        listOf("All", "Easy", "Medium", "Hard", "Solved", "Attempted", "Skipped") + viewModel.presetTags
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log History", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("Search by title or problem number...", color = NeutralGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("history_search_input")
            )

            // Horizontal Filter Chips LazyRow
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = viewModel.selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = PrimaryPurple.copy(alpha = 0.2f),
                            labelColor = TextSecondary,
                            selectedLabelColor = PrimaryPurple
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFF2C2C2E),
                            selectedBorderColor = PrimaryPurple
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            // Results List
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No problems found matching search.",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList, key = { it.id }) { problem ->
                        HistoryItemCard(
                            problem = problem,
                            onTap = { selectedProblemForDetail = problem },
                            onLongPress = { problemToDelete = problem }
                        )
                    }
                }
            }
        }

        // 1. Details Modal Bottom Sheet
        if (selectedProblemForDetail != null) {
            val item = selectedProblemForDetail!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { selectedProblemForDetail = null },
                sheetState = sheetState,
                containerColor = CardBackground,
                contentColor = TextPrimary,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LeetCode #${item.leetcodeNumber}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )

                        // Difficulty
                        val diffColor = when (item.difficulty) {
                            "EASY" -> AccentGreen
                            "MEDIUM" -> AccentAmber
                            else -> AccentRed
                        }
                        Box(
                            modifier = Modifier
                                .background(diffColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.difficulty,
                                color = diffColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = item.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    // Tags flow
                    val itemTags = item.tags.split(",").filter { it.trim().isNotEmpty() }
                    if (itemTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp
                        ) {
                            itemTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(tag, color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFF2C2C2E))

                    // Time and metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Time Spent", size = 11, color = TextSecondary)
                            Text(formatTimeInSheet(item.timeTakenSeconds), fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Column {
                            Text("Status", size = 11, color = TextSecondary)
                            val statusColor = when (item.status) {
                                "SOLVED" -> AccentGreen
                                "ATTEMPTED" -> AccentAmber
                                else -> NeutralGray
                            }
                            Text(item.status, color = statusColor, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text("Language", size = 11, color = TextSecondary)
                            Text(item.language, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // Confidence Score
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Confidence Score", fontSize = 13.sp, color = TextSecondary)
                        Row {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (star <= item.confidenceRating) AccentAmber else Color(0xFF2C2C2E),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF2C2C2E))

                    // Log date
                    Text(
                        text = "Logged progress on: " + SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(item.dateLogged)),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Notes
                    if (item.notes.trim().isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131313), RoundedCornerShape(10.dp))
                                .padding(16.dp)
                        ) {
                            Text("Approach & Notes:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(item.notes, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                        }
                    }

                    Button(
                        onClick = { selectedProblemForDetail = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Details", color = Color.White)
                    }
                }
            }
        }

        // 2. Delete Confirmation dialog
        if (problemToDelete != null) {
            AlertDialog(
                onDismissRequest = { problemToDelete = null },
                title = { Text("Delete Problem progress logs?", color = TextPrimary) },
                text = { Text("Are you sure you want to delete LeetCode #${problemToDelete!!.leetcodeNumber} - ${problemToDelete!!.title}? This action cannot be undone.", color = TextSecondary) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteProblem(problemToDelete!!)
                            problemToDelete = null
                        }
                    ) {
                        Text("Delete", color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { problemToDelete = null }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = CardBackground
            )
        }
    }
}

@Composable
fun Text(text: String, size: Int, color: Color) {
    Text(text = text, fontSize = size.sp, color = color)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    problem: ProblemEntry,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val difficultyColor = when (problem.difficulty) {
        "EASY" -> AccentGreen
        "MEDIUM" -> AccentAmber
        else -> AccentRed
    }

    val statusColor = when (problem.status) {
        "SOLVED" -> AccentGreen
        "ATTEMPTED" -> AccentAmber
        else -> NeutralGray
    }

    val tagTextList = remember(problem.tags) {
        problem.tags.split(",").filter { it.trim().isNotEmpty() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${problem.leetcodeNumber}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )

                Text(
                    text = formatLoggedDate(problem.dateLogged),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = problem.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Difficulty Pill
                    Box(
                        modifier = Modifier
                            .background(difficultyColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(problem.difficulty, color = difficultyColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (problem.status) {
                                "SOLVED" -> "✓ Solved"
                                "ATTEMPTED" -> "⟳ Attempted"
                                else -> "— Skipped"
                            },
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Confidence stars
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (star <= problem.confidenceRating) AccentAmber else Color(0xFF2C2C2E),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Tags limit (Show up to 2, +N more if excess)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (tagTextList.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tagTextList.take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF202022), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(tag, color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        if (tagTextList.size > 2) {
                            Box(
                                modifier = Modifier
                                    .background(PrimaryPurple.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("+${tagTextList.size - 2} more", color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = formatTimeInSheet(problem.timeTakenSeconds),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatLoggedDate(timestamp: Long): String {
    val calLogged = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calToday = Calendar.getInstance()
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

    fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    return when {
        isSameDay(calLogged, calToday) -> "Today"
        isSameDay(calLogged, calYesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

fun formatTimeInSheet(seconds: Int): String {
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
