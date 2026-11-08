package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BeverageCategory
import com.example.data.model.HydrationLog
import com.example.data.model.UserProfile
import com.example.domain.HabitPrediction
import com.example.domain.HydrationEstimate
import com.example.ui.i18n.HydraStrings
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.Turquoise

@Composable
fun AnalyticsAndHabitsScreen(
    profile: UserProfile,
    estimate: HydrationEstimate,
    habitPrediction: HabitPrediction,
    allLogs: List<HydrationLog>,
    todayLogs: List<HydrationLog>,
    csvExportProvider: () -> String,
    onResetHabitMemory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val isAr = profile.languageCode == "ar"
    val strings = HydraStrings.forLang(profile.languageCode)
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        val csvContent = remember { csvExportProvider() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(if (isAr) "تصدير البيانات والتقرير الذكي (CSV)" else "Export Hydration Report (CSV)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAr) {
                            "ملخص التقرير:\n• الهدف اليومي: ${estimate.finalBeverageTargetMl} مل\n" +
                                "• المستهلك اليوم: ${estimate.consumedEffectiveMl} مل (${estimate.completionPercent}%)\n" +
                                "• إجمالي السجلات المحفوظة: ${allLogs.size}"
                        } else {
                            "Report Summary:\n• Daily Target: ${estimate.finalBeverageTargetMl} ml\n" +
                                "• Today's Intake: ${estimate.consumedEffectiveMl} ml (${estimate.completionPercent}%)\n" +
                                "• Total Stored Logs: ${allLogs.size}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = csvContent.take(600),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = AquaCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "التحليلات التنبؤية وذاكرة العادات" else "Predictive Analytics & Habit Memory",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Predictive Hydration Analytics Card (Section 15)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "التحليلات التنبؤية لليوم" else "Today's Predictive Hydration Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${habitPrediction.goalProbabilityPercent}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isAr) "احتمالية تحقيق الهدف اليومي" else "Probability of Reaching Goal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${habitPrediction.expectedEndOfDayMl} ml",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isAr) "الاستهلاك المتوقع بنهاية اليوم" else "Expected End-of-Day Intake",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (habitPrediction.goalProbabilityPercent / 100f).coerceIn(0f, 1f) },
                        color = AquaCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Text(
                        text = if (isAr) {
                            "ملاحظة علمية: تعتمد التوقعات على وتيرة شربك الفعلية اليوم وسجلاتك السابقة؛ الارتباطات مع الطقس تمثل ملاحظات سلوكية وليست تشخيصاً سريرياً."
                        } else {
                            "Note: Predictions are derived from your logged pace and historical habits, not clinical dehydration measurements."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // AI Habit Memory Card (Section 10)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = Turquoise)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.habitMemoryTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onResetHabitMemory,
                            modifier = Modifier.testTag("reset_habit_memory_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "إعادة ضبط" else "Reset")
                        }
                    }

                    Text(
                        text = if (isAr) habitPrediction.habitInsightAr else habitPrediction.habitInsightEn,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isAr) "متوسط حجم الكوب: ${habitPrediction.averageServingMl} مل"
                            else "Avg Serving: ${habitPrediction.averageServingMl} ml",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isAr) "فترة الانخفاض: ${habitPrediction.frequentlyMissedWindowAr}"
                            else "Low Window: ${habitPrediction.frequentlyMissedWindowEn}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Hourly Distribution Canvas Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isAr) "توزيع شرب السوائل خلال ساعات اليوم" else "Hourly Hydration Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val buckets = remember(todayLogs, allLogs) {
                        val source = if (todayLogs.isNotEmpty()) todayLogs else allLogs
                        val slots = listOf(8, 10, 12, 14, 16, 18, 20, 22)
                        slots.map { slotHour ->
                            val sum = source.filter {
                                val h = it.hourOfDay.toInt()
                                h == slotHour || h == slotHour - 1
                            }.sumOf { it.effectiveHydrationMl.toInt() }
                            "${slotHour}:00" to sum
                        }
                    }

                    val maxVal = (buckets.maxOfOrNull { it.second } ?: 500).coerceAtLeast(300)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barCount = buckets.size
                        val spacing = size.width / (barCount * 2f)
                        val barWidth = spacing * 1.1f

                        buckets.forEachIndexed { idx, (_, ml) ->
                            val ratio = (ml.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f)
                            val barHeight = size.height * ratio
                            val x = spacing * 0.5f + idx * (barWidth + spacing * 0.85f)
                            val y = size.height - barHeight
                            drawRoundRect(
                                color = if (ml > 0) AquaCyan else OceanBlue.copy(alpha = 0.25f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        buckets.forEach { (label, _) ->
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Beverage Category Breakdown
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAr) "إحصائيات أنواع المشروبات ومساهمة الترطيب" else "Beverage Category Contributions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BeverageCategory.entries.forEach { cat ->
                        val catMl = allLogs.filter { it.beverageType == cat.key }.sumOf { it.amountMl.toInt() }
                        val effMl = allLogs.filter { it.beverageType == cat.key }.sumOf { it.effectiveHydrationMl.toInt() }
                        if (catMl > 0 || cat == BeverageCategory.WATER) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${cat.emoji} ${if (isAr) cat.nameAr else cat.nameEn}")
                                Text(
                                    text = if (isAr) "إجمالي $catMl مل (صافي $effMl مل)"
                                    else "Total $catMl ml (Net $effMl ml)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = AquaCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gamification & Healthy Consistency Badges (Section 18)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SafeEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "شارات الاستمرارية والترطيب الصحي (بدون إفراط)"
                            else "Healthy Hydration Milestones & Streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AchievementBadge(
                            emoji = "🔥",
                            title = if (isAr) "سلسلة ${profile.streakDays} أيام" else "${profile.streakDays}-Day Streak",
                            unlocked = true
                        )
                        AchievementBadge(
                            emoji = "🛡️",
                            title = if (isAr) "ترطيب آمن متوازن" else "Safe Pacing",
                            unlocked = estimate.consumedEffectiveMl <= estimate.finalBeverageTargetMl * 1.2
                        )
                        AchievementBadge(
                            emoji = "☀️",
                            title = if (isAr) "درع حرارة العراق" else "Iraq Heat Shield",
                            unlocked = todayLogs.isNotEmpty()
                        )
                    }
                }
            }
        }

        // Export CSV / Smart Report Button
        item {
            Button(
                onClick = { showExportDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_csv_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.exportCsvButton, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AchievementBadge(emoji: String, title: String, unlocked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (unlocked) Turquoise.copy(alpha = 0.25f) else OceanBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
