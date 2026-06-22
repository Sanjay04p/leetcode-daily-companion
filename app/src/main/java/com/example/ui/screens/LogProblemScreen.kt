package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LeetCodeViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogProblemScreen(
    viewModel: LeetCodeViewModel,
    onNavigateBackOrHome: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showEmptyFieldsError by remember { mutableStateOf("") }

    // Solution Languages list
    val languages = listOf("Python", "Java", "C++", "JavaScript", "Other")
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    // Back handler intercepts system back press if form is dirty
    BackHandler(enabled = viewModel.isFormDirty()) {
        showDiscardDialog = true
    }

    // Handles actual navigation back
    val handleBackNavigation = {
        if (viewModel.isFormDirty()) {
            showDiscardDialog = true
        } else {
            onNavigateBackOrHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Problem", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Problem Number input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LeetCode Problem Number", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    OutlinedTextField(
                        value = viewModel.formNumber,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                viewModel.formNumber = it
                            }
                        },
                        placeholder = { Text("e.g. 42", color = NeutralGray) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_number_input")
                    )
                }

                // 2. Problem Title input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Problem Title", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    OutlinedTextField(
                        value = viewModel.formTitle,
                        onValueChange = { viewModel.formTitle = it },
                        placeholder = { Text("e.g. Trapping Rain Water", color = NeutralGray) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_title_input")
                    )
                }

                // 3. Difficulty Segmented Control
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Difficulty", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(CardBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                            .padding(2.dp)
                    ) {
                        listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
                            val isSelected = viewModel.formDifficulty == diff
                            val (color, label) = when (diff) {
                                "EASY" -> AccentGreen to "Easy"
                                "MEDIUM" -> AccentAmber to "Medium"
                                else -> AccentRed to "Hard"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.formDifficulty = diff
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) color else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 4. Status Segmented Control
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(CardBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                            .padding(2.dp)
                    ) {
                        listOf("SOLVED", "ATTEMPTED", "SKIPPED").forEach { stat ->
                            val isSelected = viewModel.formStatus == stat
                            val (color, label) = when (stat) {
                                "SOLVED" -> AccentGreen to "Solved"
                                "ATTEMPTED" -> AccentAmber to "Attempted"
                                else -> NeutralGray to "Skipped"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.formStatus = stat
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) color else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 5. Tags Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tags", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        mainAxisSpacing = 8.dp,
                        crossAxisSpacing = 8.dp
                    ) {
                        viewModel.presetTags.forEach { tag ->
                            val isSelected = viewModel.formSelectedTags.contains(tag)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) PrimaryPurple.copy(alpha = 0.2f) else Color(0xFF2C2C2E),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) PrimaryPurple else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleTagSelection(tag)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) PrimaryPurple else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 6. Timer Stopwatch Component
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Time Spent", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // MM:SS Display
                            val minutes = viewModel.formTimeSeconds / 60
                            val seconds = viewModel.formTimeSeconds % 60
                            val timeStr = String.format("%02d:%02d", minutes, seconds)

                            Text(
                                text = timeStr,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )

                            // Stopwatch controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!viewModel.isTimerRunning) {
                                    IconButton(onClick = { viewModel.startTimer() }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Timer", tint = AccentGreen, modifier = Modifier.size(32.dp))
                                    }
                                } else {
                                    IconButton(onClick = { viewModel.pauseTimer() }) {
                                        Icon(Icons.Default.Stop, contentDescription = "Pause Timer", tint = AccentRed, modifier = Modifier.size(32.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                IconButton(onClick = { viewModel.resetTimer() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", tint = NeutralGray)
                                }
                            }

                            Divider(color = Color(0xFF2C2C2E), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                            // Manual Override Control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Or input minutes manually:", fontSize = 13.sp, color = TextSecondary)
                                OutlinedTextField(
                                    value = if (viewModel.formTimeSeconds == 0) "" else (viewModel.formTimeSeconds / 60).toString(),
                                    onValueChange = {
                                        val m = it.toIntOrNull() ?: 0
                                        viewModel.formTimeSeconds = m * 60
                                    },
                                    placeholder = { Text("mins", color = NeutralGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryPurple,
                                        unfocusedBorderColor = Color(0xFF2C2C2E),
                                        focusedContainerColor = BackgroundDark,
                                        unfocusedContainerColor = BackgroundDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                        }
                    }
                }

                // 7. Solution Language Dropdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Solution Language", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = viewModel.formLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { languageDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand dropdown", tint = TextSecondary)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = Color(0xFF2C2C2E),
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { languageDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = languageDropdownExpanded,
                            onDismissRequest = { languageDropdownExpanded = false },
                            modifier = Modifier
                                .background(CardBackground)
                                .fillMaxWidth(0.9f)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = TextPrimary) },
                                    onClick = {
                                        viewModel.formLanguage = lang
                                        languageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 8. Notes Text field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notes / Approach", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    OutlinedTextField(
                        value = viewModel.formNotes,
                        onValueChange = { viewModel.formNotes = it },
                        placeholder = { Text("How did you approach this? Key observations?", color = NeutralGray) },
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 9. Confidence star picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confidence Rating", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { star ->
                            val isSelected = star <= viewModel.formConfidence
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star Stars",
                                tint = if (isSelected) AccentAmber else NeutralGray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.formConfidence = star
                                    }
                            )
                        }
                    }
                }

                // Save button and empty fields checks
                Text(
                    text = showEmptyFieldsError,
                    color = AccentRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (viewModel.formNumber.trim().isEmpty() || viewModel.formTitle.trim().isEmpty()) {
                            showEmptyFieldsError = "Problem Number and Title are both required values!"
                        } else if (viewModel.formNumber.toIntOrNull() == null) {
                            showEmptyFieldsError = "Problem Number must be a numeric value!"
                        } else {
                            showEmptyFieldsError = ""
                            viewModel.saveProblem {
                                // Navigates or reports success
                                onNavigateBackOrHome()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_log_button")
                ) {
                    Text("Save Problem Progress ✓", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Confirms Discard if form is dirty
            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    title = { Text("Discard Changes?", color = TextPrimary) },
                    text = { Text("You have unsaved changes in your problem log form. Discard and go back?", color = TextSecondary) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDiscardDialog = false
                                viewModel.resetForm()
                                onNavigateBackOrHome()
                            }
                        ) {
                            Text("Discard", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Keep Editing", color = TextPrimary)
                        }
                    },
                    containerColor = CardBackground
                )
            }
        }
    }
}

// Simple FlowRow helper implementation since standard FlowRow is part of experimental standard/Accompanist
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val xSpacing = mainAxisSpacing.roundToPx()
        val ySpacing = crossAxisSpacing.roundToPx()

        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        val rowHeights = mutableListOf<Int>()
        var currentRowIdx = 0
        var currentRowWidth = 0

        rows.add(mutableListOf())
        rowHeights.add(0)

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            val currentPlaceableWidth = placeable.width
            val currentPlaceableHeight = placeable.height

            if (currentRowWidth + currentPlaceableWidth > constraints.maxWidth && rows[currentRowIdx].isNotEmpty()) {
                currentRowIdx++
                rows.add(mutableListOf())
                rowHeights.add(0)
                currentRowWidth = 0
            }

            rows[currentRowIdx].add(placeable)
            rowHeights[currentRowIdx] = maxOf(rowHeights[currentRowIdx], currentPlaceableHeight)
            currentRowWidth += currentPlaceableWidth + xSpacing
        }

        var totalHeight = rowHeights.sum() + (rowHeights.size - 1) * ySpacing
        totalHeight = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { rowIdx, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + xSpacing
                }
                y += rowHeights[rowIdx] + ySpacing
            }
        }
    }
}
