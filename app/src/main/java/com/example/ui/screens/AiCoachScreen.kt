package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.domain.HydrationEstimate
import com.example.ui.i18n.HydraStrings
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.HeatDangerRed
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Turquoise
import com.example.ui.viewmodel.AiCoachUiState
import com.example.ui.viewmodel.ChatMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    profile: UserProfile,
    estimate: HydrationEstimate,
    chatHistory: List<ChatMessage>,
    aiState: AiCoachUiState,
    onSendMessage: (String) -> Unit,
    onSetDialect: (String) -> Unit,
    onClearConversation: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val isAr = profile.languageCode == "ar"
    val strings = HydraStrings.forLang(profile.languageCode)
    var inputText by remember { mutableStateOf("") }

    val quickPrompts = if (isAr) {
        listOf(
            "اشرح لي كيف تم حساب هدفي اليومي (${estimate.finalBeverageTargetMl} مل)؟",
            "شلون أوزع شرب المي اليوم بالحر بدون ما أتعب الكلى؟",
            "ما الفرق بين شرب الماء النقي والقهوة أو الشاي؟",
            "اقترح لي جدول تذكيرات يناسب ساعات عملي ونومي"
        )
    } else {
        listOf(
            "Explain how my ${estimate.finalBeverageTargetMl} ml daily target was calculated",
            "How should I pace my water intake during high Iraq summer heat?",
            "How do coffee and tea count toward my hydration goal?",
            "Suggest an optimal reminder schedule for my waking hours"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header + Dialect Selector + Clear Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AquaCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = strings.aiCoachTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Turquoise,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "محمي بمحرك قواعد الترطيب الآمن" else "Protected by Safe Hydration Rules Engine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(
                onClick = onClearConversation,
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = if (isAr) "مسح المحادثة" else "Clear chat"
                )
            }
        }

        // Arabic Dialect Toggle (MSA vs Iraqi Dialect)
        if (isAr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أسلوب الحديث:",
                    style = MaterialTheme.typography.labelLarge
                )
                FilterChip(
                    selected = profile.aiDialect == "MSA",
                    onClick = { onSetDialect("MSA") },
                    label = { Text(strings.dialectMsaLabel) },
                    modifier = Modifier.testTag("dialect_msa_chip")
                )
                FilterChip(
                    selected = profile.aiDialect == "IRAQI",
                    onClick = { onSetDialect("IRAQI") },
                    label = { Text(strings.dialectIraqiLabel) },
                    modifier = Modifier.testTag("dialect_iraqi_chip")
                )
            }
        }

        // Suggested Questions
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            quickPrompts.forEachIndexed { idx, prompt ->
                AssistChip(
                    onClick = { onSendMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.testTag("quick_prompt_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat History List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatHistory, key = { it.id }) { msg ->
                val alignment = if (msg.isUser) Alignment.End else Alignment.Start
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.isUser) 18.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 18.dp
                                )
                            )
                            .background(
                                if (msg.isUser) OceanBlue
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (msg.isUser) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = msg.timestampLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Streaming / Loading / Error states
            when (aiState) {
                is AiCoachUiState.Loading -> {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "المدرب الذكي يكتب الآن..." else "AI Coach is thinking...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is AiCoachUiState.Streaming -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = aiState.partialText,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is AiCoachUiState.Error -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HeatDangerRed.copy(alpha = 0.16f)),
                            modifier = Modifier.fillMaxWidth().testTag("ai_coach_error_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = HeatDangerRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = aiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(strings.aiCoachPlaceholder) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_coach_input"),
                shape = RoundedCornerShape(18.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = aiState !is AiCoachUiState.Loading && aiState !is AiCoachUiState.Streaming,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("ai_coach_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = strings.sendButton,
                    tint = DeepNavy
                )
            }
        }
    }
}
