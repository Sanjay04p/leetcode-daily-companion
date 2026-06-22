package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AIHintState
import com.example.ui.LeetCodeViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHintScreen(
    viewModel: LeetCodeViewModel,
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coach Approach Hint", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ai_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
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
                // Intro Text
                Text(
                    text = "Stuck on a tricky problem statement? Ask the CP Coach for patterns and core insights. No spoilers, just elite mental mappings. ✦",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                // 1. Language selector Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target Language", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(CardBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(2.dp)
                    ) {
                        listOf("Python", "Java", "C++").forEach { lang ->
                            val isSelected = viewModel.aiLanguage == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) PrimaryPurple else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.aiLanguage = lang
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    lang,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) PrimaryPurple else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 2. Text Input Area
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LeetCode Problem Statement", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    OutlinedTextField(
                        value = viewModel.aiProblemStatement,
                        onValueChange = { viewModel.aiProblemStatement = it },
                        placeholder = { Text("Paste the LeetCode daily problem statement here...", color = NeutralGray) },
                        minLines = 6,
                        maxLines = 12,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_input_field")
                    )
                }

                // 3. Get Approach Hint button (purple)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.getAIHint()
                    },
                    enabled = viewModel.aiProblemStatement.trim().isNotEmpty() && viewModel.aiState != AIHintState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        disabledContainerColor = PrimaryPurple.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("get_ai_hint_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Sparkles")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Approach Hint ✦", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                }

                // 4. Output Area (Success, Loading, Error, Idle)
                when (val state = viewModel.aiState) {
                    is AIHintState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Waiting for input text... 💡", color = TextSecondary)
                        }
                    }

                    is AIHintState.Loading -> {
                        ShimmerLoadingCard()
                    }

                    is AIHintState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BoxStroke(1.dp, AccentRed)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Coach Error", fontWeight = FontWeight.Bold, color = AccentRed)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(state.message, color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }

                    is AIHintState.Success -> {
                        CoachHintCard(state.parsed)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

fun BoxStroke(width: androidx.compose.ui.unit.Dp, color: Color): BorderStroke {
    return BorderStroke(width, color)
}

@Composable
fun CoachHintCard(result: com.example.ui.AIHintResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryPurple)
                Text("Coach's Mentality Mappings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Divider(color = Color(0xFF2C2C2E))

            HintSection("1. Pattern Found", result.pattern, AccentGreen)
            HintSection("2. Key Insight", result.keyInsight, AccentAmber)
            HintSection("3. Solving Algorithm", result.algorithm, PrimaryPurple)
            HintSection("4. Complexity Targets", result.complexity, TextPrimary)
            HintSection("5. Edge Case Warning", result.watchOut, AccentRed)
        }
    }
}

@Composable
fun HintSection(heading: String, text: String, headingColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(heading, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = headingColor)
        Text(text, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
    }
}

@Composable
fun ShimmerLoadingCard() {
    val shimmerColors = listOf(
        Color(0xFF202022),
        Color(0xFF2C2C2E),
        Color(0xFF202022)
    )

    val transition = rememberInfiniteTransition()
    val translateAnimBy = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimBy.value, y = translateAnimBy.value)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )

            Divider(color = Color(0xFF2C2C2E))

            (1..4).forEach { _ ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(14.dp)
                            .background(brush, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(brush, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
