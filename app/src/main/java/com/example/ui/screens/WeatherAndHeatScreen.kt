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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.IraqHeatProfile
import com.example.data.model.UserProfile
import com.example.data.weather.WeatherInfo
import com.example.data.weather.WeatherService
import com.example.domain.HydrationCalculationEngine
import com.example.domain.HydrationEstimate
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.HeatAmber
import com.example.ui.theme.HeatDangerRed
import com.example.ui.theme.SafeEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherAndHeatScreen(
    profile: UserProfile,
    weather: WeatherInfo,
    estimate: HydrationEstimate,
    onSelectCity: (String) -> Unit,
    onSelectIraqHeatProfile: (String) -> Unit,
    onRefreshWeather: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val isAr = profile.languageCode == "ar"
    val (heatGuidanceAr, heatGuidanceEn) = HydrationCalculationEngine.getIraqHeatGuidance(
        tempC = weather.temperatureC,
        feelsLikeC = weather.feelsLikeC,
        profileKey = profile.iraqHeatProfile
    )

    val updatedTimeLabel = remember(weather.updatedAtEpochMs) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date(weather.updatedAtEpochMs))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAr) "ذكاء الطقس ووضع حرارة العراق ☀️" else "Weather Intelligence & Iraq Heat Mode ☀️",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAr) {
                            if (weather.isLive) "بيانات حية محدثة ($updatedTimeLabel)"
                            else "بيانات مخزنة محلياً لعدم الاتصال ($updatedTimeLabel)"
                        } else {
                            if (weather.isLive) "Live weather data ($updatedTimeLabel)"
                            else "Cached offline weather data ($updatedTimeLabel)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (weather.isLive) SafeEmerald else HeatAmber
                    )
                }
                IconButton(
                    onClick = onRefreshWeather,
                    modifier = Modifier.testTag("refresh_weather_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (isAr) "تحديث الطقس" else "Refresh weather"
                    )
                }
            }
        }

        // City Selector Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = AquaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "اختر المدينة (العراق والخليج):" else "Select City:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherService.supportedCities.forEach { city ->
                            FilterChip(
                                selected = profile.selectedCity.equals(city.id, ignoreCase = true),
                                onClick = { onSelectCity(city.id) },
                                label = { Text(if (isAr) city.nameAr else city.nameEn) },
                                modifier = Modifier.testTag("city_chip_${city.id}")
                            )
                        }
                    }
                }
            }
        }

        // Current Weather & Heat Stress Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) weather.cityNameAr else weather.cityNameEn,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isAr) weather.conditionAr else weather.conditionEn,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${weather.temperatureC.roundToInt()}°C",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherMetricItem(
                            icon = Icons.Default.Thermostat,
                            label = if (isAr) "المحسوسة" else "Feels Like",
                            value = "${weather.feelsLikeC.roundToInt()}°C"
                        )
                        WeatherMetricItem(
                            icon = Icons.Default.WaterDrop,
                            label = if (isAr) "الرطوبة" else "Humidity",
                            value = "${weather.humidityPercent}%"
                        )
                        WeatherMetricItem(
                            icon = Icons.Default.Air,
                            label = if (isAr) "الرياح" else "Wind",
                            value = "${weather.windSpeedKmh.roundToInt()} km/h"
                        )
                        WeatherMetricItem(
                            icon = Icons.Default.WbSunny,
                            label = if (isAr) "مؤشر UV" else "UV Index",
                            value = "${weather.uvIndex}"
                        )
                    }

                    Text(
                        text = if (isAr) heatGuidanceAr else heatGuidanceEn,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = if (isAr) {
                            "تعديل الترطيب البيئي الحالي: +${estimate.environmentAdjustmentMl} مل (محسوب علمياً حسب نطاق الإجهاد الحراري والتعرض الخارجي)."
                        } else {
                            "Current environmental hydration adjustment: +${estimate.environmentAdjustmentMl} ml (based on thermal stress band and outdoor exposure)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Hourly Weather Timeline
        if (weather.hourlyForecast.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isAr) "الجدول الزمني لدرجات الحرارة ومؤشر الأشعة فوق البنفسجية" else "Hourly Temperature & UV Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            weather.hourlyForecast.forEach { h ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(text = h.hourLabel, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${h.tempC}°C",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "UV ${h.uvIndex.roundToInt()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (h.uvIndex >= 8.0) HeatDangerRed else AquaCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Specialized Iraq Heat Mode Profiles (Section 9)
        item {
            Text(
                text = if (isAr) "ملفات وضع حرارة العراق التخصصية (5 أوضاع)" else "Iraq Heat Mode Specialized Profiles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(IraqHeatProfile.entries) { hp ->
            val selected = profile.iraqHeatProfile == hp.key
            Card(
                onClick = { onSelectIraqHeatProfile(hp.key) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("heat_profile_${hp.key}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectIraqHeatProfile(hp.key) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) hp.titleAr else hp.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAr) hp.subtitleAr else hp.subtitleEn,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isAr) "استراحة التبريد والظل المقترحة: كل ${hp.coolingBreakMinutes} دقيقة"
                            else "Recommended cooling & shade break: every ${hp.coolingBreakMinutes} min",
                            style = MaterialTheme.typography.labelLarge,
                            color = AquaCyan
                        )
                    }
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeEmerald)
                    }
                }
            }
        }

        // Heat Exhaustion & Heatstroke Safety Education Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HeatDangerRed.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = HeatDangerRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "دليل الوقاية: الإجهاد الحراري وضربة الشمس الطارئة"
                            else "Medical Safety: Heat Exhaustion vs. Heatstroke",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isAr) {
                            "• الإجهاد الحراري: تعرق غزير، دوخة، صداع، وغثيان — انتقل فوراً لمكان مكيف أو مظلل، برد الجسم، واشرب الماء على دفعات متدرجة.\n" +
                                "• تحذير طبي صارم: شرب الماء وحده لا يعالج ضربة الشمس (ارتفاع حرارة الجسم مع تشوش ذهني أو فقدان وعي أو جفاف الجلد الحار). ضربة الشمس حالة طبية طارئة تستوجب الاتصال بالإسعاف والتبريد الخارجي الفوري."
                        } else {
                            "• Heat Exhaustion: Heavy sweating, dizziness, headache, nausea — move to shade/AC, cool the body, and sip fluids gradually.\n" +
                                "• Critical Medical Notice: Drinking water alone CANNOT treat heatstroke (confusion, hot dry skin, fainting). Heatstroke is a life-threatening emergency requiring immediate medical care and active external cooling."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WeatherMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
