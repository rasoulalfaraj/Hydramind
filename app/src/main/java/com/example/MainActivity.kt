package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.HydraMindRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.i18n.HydraStrings
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AnalyticsAndHabitsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsAndSafetyScreen
import com.example.ui.screens.WeatherAndHeatScreen
import com.example.ui.theme.HydraMindTheme
import com.example.ui.viewmodel.HydraMindViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class HydraDestination {
    DASHBOARD,
    WEATHER_HEAT,
    AI_COACH,
    ANALYTICS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            HydraMindRootApp()
        }
    }
}

@Composable
fun HydraMindRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var unauthIsArabic by rememberSaveable { mutableStateOf(true) } // Arabic-first mandatory default

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        HydraMindTheme(isArabic = unauthIsArabic) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AuthScreen(
                    isArabic = unauthIsArabic,
                    onToggleLanguage = { unauthIsArabic = !unauthIsArabic },
                    onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                )
            }
        }
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedHydraMindApp(currentUserId = uid)
        }
    }
}

@Composable
fun AuthenticatedHydraMindApp(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: HydraMindViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HydraMindViewModel(HydraMindRepository(db), currentUserId)
            }
        }
    )

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val aiCoachState by viewModel.aiCoachState.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val dailyAiInsight by viewModel.dailyAiInsight.collectAsStateWithLifecycle()
    val reminderHistory by viewModel.reminderHistory.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(HydraDestination.DASHBOARD) }
    var editingOnboarding by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(profile.selectedCity) {
        viewModel.refreshWeather(context, profile.selectedCity)
    }

    val isArabic = profile.languageCode == "ar"
    val strings = HydraStrings.forLang(profile.languageCode)

    val todayLogs = remember(allLogs) { viewModel.getTodayLogs(allLogs) }
    val estimate = remember(profile, weather, allLogs) {
        viewModel.computeCurrentEstimate(profile, weather, allLogs)
    }
    val safety = remember(profile, estimate, allLogs) {
        viewModel.computeSafetyAssessment(profile, estimate, allLogs)
    }
    val habitPrediction = remember(profile, estimate, allLogs) {
        viewModel.computeHabitPrediction(profile, estimate, allLogs)
    }

    HydraMindTheme(isArabic = isArabic) {
        if (!profile.onboardingCompleted || editingOnboarding) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    OnboardingScreen(
                        initialProfile = profile,
                        onToggleLanguage = { viewModel.toggleLanguage() },
                        onComplete = { updated ->
                            editingOnboarding = false
                            viewModel.completeOnboarding(updated, context)
                        }
                    )
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == HydraDestination.DASHBOARD,
                            onClick = { currentTab = HydraDestination.DASHBOARD },
                            icon = { Icon(Icons.Default.Home, contentDescription = strings.navDashboard) },
                            label = { Text(strings.navDashboard) },
                            modifier = Modifier.testTag("nav_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentTab == HydraDestination.WEATHER_HEAT,
                            onClick = { currentTab = HydraDestination.WEATHER_HEAT },
                            icon = { Icon(Icons.Default.WbSunny, contentDescription = strings.navWeatherHeat) },
                            label = { Text(strings.navWeatherHeat) },
                            modifier = Modifier.testTag("nav_weather_heat")
                        )
                        NavigationBarItem(
                            selected = currentTab == HydraDestination.AI_COACH,
                            onClick = { currentTab = HydraDestination.AI_COACH },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.navAiCoach) },
                            label = { Text(strings.navAiCoach) },
                            modifier = Modifier.testTag("nav_ai_coach")
                        )
                        NavigationBarItem(
                            selected = currentTab == HydraDestination.ANALYTICS,
                            onClick = { currentTab = HydraDestination.ANALYTICS },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = strings.navAnalytics) },
                            label = { Text(strings.navAnalytics) },
                            modifier = Modifier.testTag("nav_analytics")
                        )
                        NavigationBarItem(
                            selected = currentTab == HydraDestination.SETTINGS,
                            onClick = { currentTab = HydraDestination.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                            label = { Text(strings.navSettings) },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        HydraDestination.DASHBOARD -> DashboardScreen(
                            profile = profile,
                            weather = weather,
                            estimate = estimate,
                            safety = safety,
                            habitPrediction = habitPrediction,
                            todayLogs = todayLogs,
                            aiInsight = dailyAiInsight,
                            statusBanner = statusBanner,
                            onAddDrink = { cat, ml, note -> viewModel.addBeverageLog(cat, ml, note) },
                            onUndoLast = { viewModel.undoLastLog() },
                            onDeleteLog = { id -> viewModel.deleteLogItem(id) },
                            onRefreshAiInsight = { viewModel.generateDashboardAiInsight() },
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onOpenAiCoach = { currentTab = HydraDestination.AI_COACH },
                            onOpenWeather = { currentTab = HydraDestination.WEATHER_HEAT }
                        )
                        HydraDestination.WEATHER_HEAT -> WeatherAndHeatScreen(
                            profile = profile,
                            weather = weather,
                            estimate = estimate,
                            onSelectCity = { cityId ->
                                viewModel.updateProfile(profile.copy(selectedCity = cityId), context)
                            },
                            onSelectIraqHeatProfile = { hpKey ->
                                viewModel.updateProfile(profile.copy(iraqHeatProfile = hpKey))
                            },
                            onRefreshWeather = { viewModel.refreshWeather(context, profile.selectedCity) },
                            onNavigateBack = { currentTab = HydraDestination.DASHBOARD }
                        )
                        HydraDestination.AI_COACH -> AiCoachScreen(
                            profile = profile,
                            estimate = estimate,
                            chatHistory = chatHistory,
                            aiState = aiCoachState,
                            onSendMessage = { msg -> viewModel.sendCoachMessage(msg) },
                            onSetDialect = { dialect -> viewModel.setAiDialect(dialect) },
                            onClearConversation = { viewModel.clearChatHistory() },
                            onNavigateBack = { currentTab = HydraDestination.DASHBOARD }
                        )
                        HydraDestination.ANALYTICS -> AnalyticsAndHabitsScreen(
                            profile = profile,
                            estimate = estimate,
                            habitPrediction = habitPrediction,
                            allLogs = allLogs,
                            todayLogs = todayLogs,
                            csvExportProvider = { viewModel.buildCsvExport() },
                            onResetHabitMemory = { viewModel.resetHabitLearning() },
                            onNavigateBack = { currentTab = HydraDestination.DASHBOARD }
                        )
                        HydraDestination.SETTINGS -> SettingsAndSafetyScreen(
                            profile = profile,
                            reminderHistory = reminderHistory,
                            onUpdateProfile = { updated -> viewModel.updateProfile(updated, context) },
                            onToggleLanguage = { viewModel.toggleLanguage() },
                            onEditOnboarding = { editingOnboarding = true },
                            onRecordSmartReminder = { title, body -> viewModel.recordSmartReminderSent(title, body) },
                            onDeleteAllData = { viewModel.deleteAllData() },
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            onNavigateBack = { currentTab = HydraDestination.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}
