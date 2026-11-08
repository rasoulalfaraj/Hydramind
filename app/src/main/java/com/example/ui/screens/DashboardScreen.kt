package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BeverageCategory
import com.example.data.model.HydrationLog
import com.example.data.model.UserProfile
import com.example.data.weather.WeatherInfo
import com.example.domain.HabitPrediction
import com.example.domain.HydrationEstimate
import com.example.domain.SafetyAssessment
import com.example.ui.i18n.HydraStrings
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.HeatDangerRed
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Turquoise
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    profile: UserProfile,
    weather: WeatherInfo,
    estimate: HydrationEstimate,
    safety: SafetyAssessment,
    habitPrediction: HabitPrediction,
    todayLogs: List<HydrationLog>,
    aiInsight: String,
    statusBanner: String?,
    onAddDrink: (BeverageCategory, Int, String) -> Unit,
    onUndoLast: () -> Unit,
    onDeleteLog: (String) -> Unit,
    onRefreshAiInsight: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenWeather: () -> Unit
) {
    val isAr = profile.languageCode == "ar"
    val strings = HydraStrings.forLang(profile.languageCode)

    var selectedCategory by remember { mutableStateOf(BeverageCategory.WATER) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("300") }
    var customNoteText by remember { mutableStateOf("") }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(if (isAr) "إضافة مشروب مخصص" else "Add Custom Beverage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isAr) "الكمية (مل)" else "Amount (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_amount_input")
                    )
                    OutlinedTextField(
                        value = customNoteText,
                        onValueChange = { customNoteText = it },
                        label = { Text(if (isAr) "ملاحظة اختيارية" else "Optional note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ml = (customAmountText.toIntOrNull() ?: 250).coerceIn(20, 2000)
                        onAddDrink(selectedCategory, ml, customNoteText)
                        customNoteText = ""
                        showCustomDialog = false
                    },
                    modifier = Modifier.testTag("confirm_custom_drink_button")
                ) {
                    Text(if (isAr) "إضافة" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
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
            Spacer(modifier = Modifier.height(6.dp))
            // Header row: Greeting + City/Weather pill + Language toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${strings.greetingPrefix} ${profile.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.appTagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("dashboard_language_button")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.switchLanguageButton)
                }
            }
        }

        // Safety Warning Banner (Safe Hydration Mode - Section 11)
        val safetyBanner = if (isAr) safety.warningBannerAr else safety.warningBannerEn
        if (safetyBanner != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HeatDangerRed.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("safety_warning_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = HeatDangerRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = safetyBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (statusBanner != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Weather Summary Bar
        item {
            Card(
                onClick = onOpenWeather,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                modifier = Modifier.fillMaxWidth().testTag("weather_summary_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = AquaCyan,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${if (isAr) weather.cityNameAr else weather.cityNameEn} • ${weather.temperatureC.roundToInt()}°C",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAr) {
                                    "المحسوسة ${weather.feelsLikeC.roundToInt()}°م • الرطوبة ${weather.humidityPercent}% • ${weather.heatRiskAr}"
                                } else {
                                    "Feels like ${weather.feelsLikeC.roundToInt()}°C • Humidity ${weather.humidityPercent}% • ${weather.heatRiskEn}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Animated Water Container & Core Metrics Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedLiquidWaterTank(
                        completionFraction = (estimate.completionPercent / 100f).coerceIn(0f, 1f),
                        consumedMl = estimate.consumedEffectiveMl,
                        targetMl = estimate.finalBeverageTargetMl,
                        completionPercent = estimate.completionPercent,
                        isArabic = isAr
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricStatColumn(
                            label = strings.dailyTargetLabel,
                            value = "${estimate.finalBeverageTargetMl} ml"
                        )
                        MetricStatColumn(
                            label = strings.consumedTodayLabel,
                            value = "${estimate.consumedEffectiveMl} ml"
                        )
                        MetricStatColumn(
                            label = strings.remainingLabel,
                            value = "${estimate.remainingMl} ml"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Turquoise,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAr) {
                                "التذكير الذكي القادم: كل ${profile.reminderIntervalMinutes} دقيقة (الدفعة المقترحة: ${estimate.suggestedSingleServingMl} مل)"
                            } else {
                                "Next smart reminder: every ${profile.reminderIntervalMinutes} min (Suggested sip: ${estimate.suggestedSingleServingMl} ml)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Quick-Add Beverage Controls (Section 14)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.quickAddTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (todayLogs.isNotEmpty()) {
                            TextButton(
                                onClick = onUndoLast,
                                modifier = Modifier.testTag("undo_last_log_button")
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.undoLastButton)
                            }
                        }
                    }

                    // Beverage Category Selector
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BeverageCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text("${cat.emoji} ${if (isAr) cat.nameAr else cat.nameEn}")
                                },
                                modifier = Modifier.testTag("category_${cat.key}")
                            )
                        }
                    }

                    Text(
                        text = if (isAr) selectedCategory.noteAr else selectedCategory.noteEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Preset Quantities: 100, 150, 200, 250, 330, 500 ml + Custom
                    val presets = listOf(100, 150, 200, 250, 330, 500)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { ml ->
                            Button(
                                onClick = { onAddDrink(selectedCategory, ml, "") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("quick_add_${ml}_button")
                            ) {
                                Text(text = "+$ml ${if (isAr) "مل" else "ml"}")
                            }
                        }
                        OutlinedButton(
                            onClick = { showCustomDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("custom_drink_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.customDrinkButton)
                        }
                    }
                }
            }
        }

        // AI-Generated Hydration Insight Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "رؤية المدرب الذكي اليومية" else "AI Hydration Coach Insight",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onRefreshAiInsight,
                                modifier = Modifier.testTag("refresh_ai_insight_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (isAr) "تحديث النصيحة الذكية" else "Refresh AI insight",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = aiInsight,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = onOpenAiCoach,
                            label = {
                                Text(if (isAr) "اسأل المدرب الذكي" else "Ask AI Coach")
                            }
                        )
                        Text(
                            text = if (isAr) "احتمالية تحقيق الهدف اليوم: ${habitPrediction.goalProbabilityPercent}%"
                            else "Daily goal probability: ${habitPrediction.goalProbabilityPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Daily Consumption Timeline Header
        item {
            Text(
                text = "${strings.todayTimelineTitle} (${todayLogs.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (todayLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.emptyLogsHint,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(todayLogs, key = { it.id.ifBlank { it.loggedAtMillis.toString() } }) { log ->
                val cat = BeverageCategory.fromKey(log.beverageType)
                val timeStr = remember(log.loggedAtMillis) {
                    SimpleDateFormat("HH:mm", Locale.US).format(Date(log.loggedAtMillis))
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(OceanBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = cat.emoji, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${if (isAr) cat.nameAr else cat.nameEn} • ${log.amountMl} ${if (isAr) "مل" else "ml"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAr) {
                                        "الساعة $timeStr • صافي الترطيب: ${log.effectiveHydrationMl} مل" +
                                            if (log.note.isNotBlank()) " • ${log.note}" else ""
                                    } else {
                                        "$timeStr • Effective hydration: ${log.effectiveHydrationMl} ml" +
                                            if (log.note.isNotBlank()) " • ${log.note}" else ""
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteLog(log.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = if (isAr) "حذف السجل" else "Delete log"
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AquaCyan
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AnimatedLiquidWaterTank(
    completionFraction: Float,
    consumedMl: Int,
    targetMl: Int,
    completionPercent: Int,
    isArabic: Boolean
) {
    val animatedLevel by animateFloatAsState(
        targetValue = completionFraction.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "waterLevel"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = Modifier
            .size(210.dp)
            .testTag("animated_water_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            val circlePath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center.x - radius + 8f,
                        center.y - radius + 8f,
                        center.x + radius - 8f,
                        center.y + radius - 8f
                    )
                )
            }

            // Deep background inside the circular vessel
            drawCircle(
                color = DeepNavy.copy(alpha = 0.85f),
                radius = radius - 8f,
                center = center
            )

            clipPath(circlePath) {
                val waterTopY = size.height * (1f - animatedLevel)
                val waveAmplitude = 10f
                val wavePath = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, waterTopY)
                    val steps = 40
                    val stepX = size.width / steps
                    for (i in 0..steps) {
                        val x = i * stepX
                        val angle = (i.toFloat() / steps) * (2f * PI.toFloat()) + wavePhase
                        val y = waterTopY + sin(angle) * waveAmplitude
                        lineTo(x, y)
                    }
                    lineTo(size.width, size.height)
                    close()
                }

                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AquaCyan.copy(alpha = 0.85f),
                            OceanBlue.copy(alpha = 0.92f),
                            Turquoise.copy(alpha = 0.95f)
                        ),
                        startY = waterTopY - 15f,
                        endY = size.height
                    )
                )
            }

            // Outer glowing progress arc
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(AquaCyan, Turquoise, OceanBlue, AquaCyan)),
                startAngle = -90f,
                sweepAngle = 360f * completionFraction.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completionPercent%",
                style = MaterialTheme.typography.displayLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$consumedMl / $targetMl ${if (isArabic) "مل" else "ml"}",
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite.copy(alpha = 0.92f)
            )
        }
    }
}
