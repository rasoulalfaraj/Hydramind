package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.IraqHeatProfile
import com.example.data.model.MedicalRestriction
import com.example.data.model.UserProfile
import com.example.data.weather.WeatherService
import com.example.domain.HydrationCalculationEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    onToggleLanguage: () -> Unit,
    onComplete: (UserProfile) -> Unit
) {
    val isAr = initialProfile.languageCode == "ar"

    var name by remember(initialProfile.name) { mutableStateOf(initialProfile.name) }
    var ageText by remember(initialProfile.age) { mutableStateOf(initialProfile.age.toString()) }
    var weightText by remember(initialProfile.weightKg) { mutableStateOf(initialProfile.weightKg.toString()) }
    var heightText by remember(initialProfile.heightCm) { mutableStateOf(initialProfile.heightCm.toString()) }
    var biologicalSex by remember(initialProfile.biologicalSex) { mutableStateOf(initialProfile.biologicalSex) }
    var activityLevel by remember(initialProfile.activityLevel) { mutableStateOf(initialProfile.activityLevel) }
    var outdoorMinutes by remember(initialProfile.outdoorMinutes) { mutableIntStateOf(initialProfile.outdoorMinutes.toInt()) }
    var exerciseMinutes by remember(initialProfile.exerciseMinutes) { mutableIntStateOf(initialProfile.exerciseMinutes.toInt()) }
    var sweatingLevel by remember(initialProfile.sweatingLevel) { mutableStateOf(initialProfile.sweatingLevel) }
    var selectedCity by remember(initialProfile.selectedCity) { mutableStateOf(initialProfile.selectedCity) }
    var iraqHeatProfile by remember(initialProfile.iraqHeatProfile) { mutableStateOf(initialProfile.iraqHeatProfile) }
    var medicalType by remember(initialProfile.medicalRestrictionType) { mutableStateOf(initialProfile.medicalRestrictionType) }
    var prescribedLimitText by remember(initialProfile.prescribedLimitMl) {
        mutableStateOf(if (initialProfile.prescribedLimitMl > 0) initialProfile.prescribedLimitMl.toString() else "")
    }

    val draftProfile = initialProfile.copy(
        name = name.ifBlank { if (isAr) "أحمد" else "Ahmed" },
        age = (ageText.toLongOrNull() ?: 28L).coerceIn(5L, 120L),
        weightKg = (weightText.toLongOrNull() ?: 72L).coerceIn(15L, 300L),
        heightCm = (heightText.toLongOrNull() ?: 172L).coerceIn(50L, 250L),
        biologicalSex = biologicalSex,
        activityLevel = activityLevel,
        outdoorMinutes = outdoorMinutes.toLong(),
        exerciseMinutes = exerciseMinutes.toLong(),
        sweatingLevel = sweatingLevel,
        selectedCity = selectedCity,
        iraqHeatProfile = iraqHeatProfile,
        medicalRestrictionType = medicalType,
        prescribedLimitMl = (prescribedLimitText.toLongOrNull() ?: 0L).coerceIn(0L, 10000L)
    )

    val liveEstimate = HydrationCalculationEngine.calculateHydration(
        profile = draftProfile,
        ambientTempC = 39.0,
        feelsLikeC = 41.0,
        humidityPercent = 25,
        todayLogs = emptyList()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAr) "إعداد ملف الترطيب الذكي" else "Personalized Hydration Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAr) "تُستخدم هذه البيانات لتقدير احتياجك اليومي بدقة علمية؛ ويمكنك تخطي الحقول الطبية الاختيارية."
                    else "Used to estimate your daily fluid needs scientifically; optional medical fields can be skipped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onToggleLanguage,
                modifier = Modifier.testTag("onboarding_lang_button")
            ) {
                Icon(Icons.Default.Language, contentDescription = null)
                Text(if (isAr) " EN" else " عربي")
            }
        }

        // Section 1: Basic Info
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isAr) "1. المعلومات الشخصية الأساسية" else "1. Basic Personal Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isAr) "الاسم" else "Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isAr) "العمر (سنة)" else "Age (yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("onboarding_age_input")
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isAr) "الوزن (كغم)" else "Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("onboarding_weight_input")
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isAr) "الطول (سم)" else "Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (isAr) "المدينة (لتحليل الطقس وحرارة العراق):" else "City (for live weather & heat analysis):",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeatherService.supportedCities.take(6).forEach { city ->
                        FilterChip(
                            selected = selectedCity == city.id,
                            onClick = { selectedCity = city.id },
                            label = { Text(if (isAr) city.nameAr else city.nameEn) }
                        )
                    }
                }
            }
        }

        // Section 2: Activity, Heat Exposure & Iraq Heat Mode Profile
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isAr) "2. النشاط البدني ووضع حرارة العراق ☀️" else "2. Activity & Iraq Heat Mode Profile ☀️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "LOW" to (if (isAr) "نشاط منخفض" else "Low Activity"),
                        "MODERATE" to (if (isAr) "نشاط متوسط" else "Moderate"),
                        "HIGH" to (if (isAr) "نشاط عالي" else "High"),
                        "ATHLETE" to (if (isAr) "رياضي" else "Athlete")
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = activityLevel == key,
                            onClick = { activityLevel = key },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = if (isAr) "التعرض اليومي للشمس/الخارج: $outdoorMinutes دقيقة"
                    else "Daily outdoor sun exposure: $outdoorMinutes min",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = outdoorMinutes.toFloat(),
                    onValueChange = { outdoorMinutes = it.toInt() },
                    valueRange = 0f..240f
                )

                Text(
                    text = if (isAr) "ملف وضع حرارة العراق المخصص:" else "Iraq Heat Mode Specialized Profile:",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IraqHeatProfile.entries.forEach { hp ->
                        FilterChip(
                            selected = iraqHeatProfile == hp.key,
                            onClick = { iraqHeatProfile = hp.key },
                            label = { Text(if (isAr) hp.titleAr else hp.titleEn) }
                        )
                    }
                }
            }
        }

        // Section 3: Optional Medical Safety Profile
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (isAr) " 3. وضع الترطيب الآمن والقيود الطبية (اختياري)"
                        else " 3. Safe Hydration & Medical Restrictions (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isAr) "لماذا نسأل؟ بعض حالات الكلى أو القلب تتطلب عدم زيادة السوائل تلقائياً في الطقس الحار. يمكنك تركها «لا توجد قيود»."
                    else "Why we ask: Kidney or heart conditions require locking automatic fluid increases in hot weather.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedicalRestriction.entries.forEach { mr ->
                        FilterChip(
                            selected = medicalType == mr.key,
                            onClick = { medicalType = mr.key },
                            label = { Text(if (isAr) mr.titleAr else mr.titleEn) }
                        )
                    }
                }
                if (medicalType != "NONE") {
                    OutlinedTextField(
                        value = prescribedLimitText,
                        onValueChange = { prescribedLimitText = it.filter { ch -> ch.isDigit() } },
                        label = {
                            Text(
                                if (isAr) "الحد اليومي الموصوف من الطبيب بالملليلتر (اختياري)"
                                else "Physician-prescribed daily limit in ml (optional)"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section 4: Live Science-Based Target Preview Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = if (isAr) " هدفك اليومي التقديري من المشروبات: ${liveEstimate.finalBeverageTargetMl} مل"
                        else " Estimated Daily Beverage Target: ${liveEstimate.finalBeverageTargetMl} ml",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = if (isAr) liveEstimate.explanationAr else liveEstimate.explanationEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (isAr) liveEstimate.confidenceNoteAr else liveEstimate.confidenceNoteEn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Button(
            onClick = { onComplete(draftProfile) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("complete_onboarding_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text(
                text = if (isAr) " حفظ والبدء في هايدرا مايند" else " Save & Launch HydraMind AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
