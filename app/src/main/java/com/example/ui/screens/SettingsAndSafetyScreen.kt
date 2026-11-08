package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MedicalRestriction
import com.example.data.model.UserProfile
import com.example.ui.i18n.HydraStrings
import com.example.ui.theme.AquaCyan
import com.example.ui.theme.HeatDangerRed
import com.example.ui.theme.Turquoise
import com.example.ui.viewmodel.ReminderLogItem
import com.example.util.HydraNotificationHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndSafetyScreen(
    profile: UserProfile,
    reminderHistory: List<ReminderLogItem>,
    onUpdateProfile: (UserProfile) -> Unit,
    onToggleLanguage: () -> Unit,
    onEditOnboarding: () -> Unit,
    onRecordSmartReminder: (String, String) -> Unit,
    onDeleteAllData: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val isAr = profile.languageCode == "ar"
    val strings = HydraStrings.forLang(profile.languageCode)

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val title = if (isAr) "💧 حان وقت شرب الماء!" else "💧 Time to Drink Water!"
        val body = if (isAr) {
            "☀️ الجو حار اليوم في ${profile.selectedCity}. اشرب 200 مل ماء بانتظام وخذ استراحة بمكان بارد."
        } else {
            "☀️ Stay hydrated in ${profile.selectedCity}! Sip 200 ml of cool water steadily."
        }
        HydraNotificationHelper.sendSmartNotification(context, title, body)
        onRecordSmartReminder(title, body)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isAr) "تأكيد حذف كافة البيانات" else "Confirm Complete Data Deletion") },
            text = {
                Text(
                    if (isAr) "سيتم حذف جميع سجلات شرب الماء والملف الشخصي وذاكرة العادات نهائياً. هل أنت متأكد؟"
                    else "All hydration logs, personal profile, and learned habits will be permanently deleted. Proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeatDangerRed)
                ) {
                    Text(if (isAr) "حذف نهائي" else "Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAr) "الإعدادات، التذكيرات الذكية والخصوصية" else "Settings, Smart Reminders & Privacy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Language & Profile Editing Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = AquaCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "لغة التطبيق (تبديل فوري RTL/LTR)" else "App Language (Instant RTL/LTR)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onToggleLanguage,
                            modifier = Modifier.testTag("settings_language_button")
                        ) {
                            Text(strings.switchLanguageButton)
                        }
                    }

                    OutlinedButton(
                        onClick = onEditOnboarding,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "تعديل الوزن والعمر وبيانات الملف الشخصي" else "Edit Personal Hydration Profile")
                    }
                }
            }
        }

        // Smart Reminder Engine Card (Section 12)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Turquoise)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "محرك التذكيرات الذكية" else "Smart Reminder Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = profile.remindersEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateProfile(profile.copy(remindersEnabled = enabled))
                            }
                        )
                    }

                    Text(
                        text = if (isAr) "الفاصل الزمني للتذكير (مع مراعاة ساعات النوم ${profile.bedHour}:00 - ${profile.wakeHour}:00):"
                        else "Reminder interval (Sleep-aware ${profile.bedHour}:00 - ${profile.wakeHour}:00):",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val intervals = listOf(30L, 45L, 60L, 90L, 120L)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        intervals.forEach { mins ->
                            FilterChip(
                                selected = profile.reminderIntervalMinutes == mins,
                                onClick = { onUpdateProfile(profile.copy(reminderIntervalMinutes = mins)) },
                                label = { Text("$mins ${if (isAr) "دقيقة" else "min"}") },
                                modifier = Modifier.testTag("interval_${mins}_chip")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val title = if (isAr) "💧 حان وقت شرب الماء!" else "💧 Time to Drink Water!"
                                val body = if (isAr) {
                                    "☀️ الجو حار اليوم في ${profile.selectedCity}. اشرب 200 مل ماء بانتظام وخذ استراحة بمكان بارد."
                                } else {
                                    "☀️ Stay hydrated in ${profile.selectedCity}! Sip 200 ml of cool water steadily."
                                }
                                HydraNotificationHelper.sendSmartNotification(context, title, body)
                                onRecordSmartReminder(title, body)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_smart_notification_button")
                    ) {
                        Text(strings.triggerSmartNotificationButton)
                    }
                }
            }
        }

        // Safe Hydration Mode & Medical Restrictions Card (Section 11)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AquaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.safeHydrationTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isAr) "اختر ملف السلامة الطبي (يمنع الزيادات التلقائية غير الآمنة):"
                        else "Select Medical Safety Profile (locks automatic target increases):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedicalRestriction.entries.forEach { mr ->
                            FilterChip(
                                selected = profile.medicalRestrictionType == mr.key,
                                onClick = { onUpdateProfile(profile.copy(medicalRestrictionType = mr.key)) },
                                label = { Text(if (isAr) mr.titleAr else mr.titleEn) }
                            )
                        }
                    }
                    val currentRestriction = MedicalRestriction.fromKey(profile.medicalRestrictionType)
                    Text(
                        text = if (isAr) currentRestriction.cautionAr else currentRestriction.cautionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Privacy & Habit Learning Controls (Sections 10 & 20)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Turquoise)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "تعلم العادات الذكي والخصوصية" else "AI Habit Learning & Privacy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = profile.habitLearningEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateProfile(profile.copy(habitLearningEnabled = enabled))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f).testTag("delete_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = HeatDangerRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.deleteAllDataButton, color = HeatDangerRed)
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.weight(1f).testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.signOutButton)
                        }
                    }
                }
            }
        }

        // Notification History Section
        item {
            Text(
                text = if (isAr) "سجل التذكيرات الذكية (${reminderHistory.size})" else "Smart Reminder History (${reminderHistory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(reminderHistory, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${item.timeLabel} • ${item.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
