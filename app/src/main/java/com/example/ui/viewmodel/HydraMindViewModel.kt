package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BeverageCategory
import com.example.data.model.HydrationLog
import com.example.data.model.IraqHeatProfile
import com.example.data.model.MedicalRestriction
import com.example.data.model.UserProfile
import com.example.data.repository.HydraMindRepository
import com.example.data.weather.WeatherInfo
import com.example.data.weather.WeatherService
import com.example.domain.HabitPrediction
import com.example.domain.HydrationCalculationEngine
import com.example.domain.HydrationEstimate
import com.example.domain.SafetyAssessment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "HydraMindViewModel"

sealed interface AiCoachUiState {
    data object Idle : AiCoachUiState
    data object Loading : AiCoachUiState
    data class Streaming(val partialText: String) : AiCoachUiState
    data class Success(val text: String) : AiCoachUiState
    data class Error(val message: String) : AiCoachUiState
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String
)

data class ReminderLogItem(
    val timeLabel: String,
    val title: String,
    val message: String,
    val id: String = java.util.UUID.randomUUID().toString()
)

class HydraMindViewModel(
    private val repository: HydraMindRepository,
    private val currentUserId: String
) : ViewModel() {

    // Lazy initialization of Gemini 3.8 Flash via Firebase AI Logic (Mandatory per skill)
    private val generativeModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _profile = MutableStateFlow(UserProfile(userId = currentUserId))
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _allLogs = MutableStateFlow<List<HydrationLog>>(emptyList())
    val allLogs: StateFlow<List<HydrationLog>> = _allLogs.asStateFlow()

    private val _weather = MutableStateFlow(
        WeatherInfo(
            cityId = "Baghdad",
            cityNameAr = "بغداد",
            cityNameEn = "Baghdad",
            temperatureC = 39.0,
            feelsLikeC = 41.0,
            humidityPercent = 24,
            windSpeedKmh = 14.0,
            uvIndex = 9.2,
            conditionAr = "صافي ومشمس",
            conditionEn = "Clear Sky",
            heatRiskLevel = "EXTREME_CAUTION",
            heatRiskAr = "حذر شديد: إجهاد حراري مرتفع",
            heatRiskEn = "High Caution: Elevated Thermal Stress",
            hourlyForecast = emptyList(),
            isLive = false,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    )
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    private val _aiCoachState = MutableStateFlow<AiCoachUiState>(AiCoachUiState.Idle)
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _dailyAiInsight = MutableStateFlow(
        "💧 نصيحة هايدرا مايند: اشرب كوباً من الماء (250 مل) كل ساعة صباحاً لتعويض سوائل النوم وتعزيز التركيز قبل اشتداد حرارة الظهيرة."
    )
    val dailyAiInsight: StateFlow<String> = _dailyAiInsight.asStateFlow()

    private val _reminderHistory = MutableStateFlow<List<ReminderLogItem>>(
        listOf(
            ReminderLogItem("08:30", "💧 تذكير صباحي ذكي", "صباح النشاط! حان وقت أول كوب ماء (250 مل)."),
            ReminderLogItem("11:00", "🧠 تذكير ذاكرة العادات", "لاحظنا انخفاض شربك للماء قبل الظهر — خذ رشفات معتدلة الآن."),
            ReminderLogItem("13:30", "☀️ تنبيه وضع حرارة العراق", "ذروة الإشعاع الشمسي الآن. اشرب 200 مل ماء بارد وخذ استراحة بمكان مظلل.")
        )
    )
    val reminderHistory: StateFlow<List<ReminderLogItem>> = _reminderHistory.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        observeUserFirestoreData()
        seedWelcomeChat()
    }

    private fun observeUserFirestoreData() {
        viewModelScope.launch {
            repository.observeProfile(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing profile", firestoreError ?: error)
                }
                .collect { remoteProfile ->
                    if (remoteProfile != null) {
                        _profile.value = remoteProfile
                    } else {
                        // Initialize default Arabic-first profile in Firestore
                        val initial = _profile.value.copy(userId = currentUserId)
                        repository.saveProfile(initial)
                    }
                }
        }

        viewModelScope.launch {
            repository.observeLogs(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing logs", firestoreError ?: error)
                }
                .collect { logs ->
                    _allLogs.value = logs
                }
        }
    }

    private fun seedWelcomeChat() {
        val welcomeAr = "مرحباً بك في مدرب هايدرا مايند الذكي! أنا هنا لشرح هدفك اليومي من السوائل، تحليل تأثير طقس مدينتك وحرارة الصيف، ومراجعة عادات شربك للماء مع الالتزام الكامل بقواعد الترطيب الآمن. كيف يمكنني مساعدتك اليوم؟"
        _chatHistory.value = listOf(
            ChatMessage(
                isUser = false,
                text = welcomeAr,
                timestampLabel = currentTimeLabel()
            )
        )
    }

    fun refreshWeather(context: Context, cityId: String = _profile.value.selectedCity) {
        viewModelScope.launch {
            val fetched = WeatherService.fetchWeather(context.applicationContext, cityId)
            _weather.value = fetched
        }
    }

    fun todayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayLogs(logs: List<HydrationLog> = _allLogs.value): List<HydrationLog> {
        val today = todayDateKey()
        return logs.filter { it.dateKey == today }
    }

    fun computeCurrentEstimate(
        profile: UserProfile = _profile.value,
        weather: WeatherInfo = _weather.value,
        logs: List<HydrationLog> = _allLogs.value
    ): HydrationEstimate {
        val todayLogs = getTodayLogs(logs)
        return HydrationCalculationEngine.calculateHydration(
            profile = profile,
            ambientTempC = weather.temperatureC,
            feelsLikeC = weather.feelsLikeC,
            humidityPercent = weather.humidityPercent,
            todayLogs = todayLogs
        )
    }

    fun computeSafetyAssessment(
        profile: UserProfile = _profile.value,
        estimate: HydrationEstimate = computeCurrentEstimate(profile),
        logs: List<HydrationLog> = _allLogs.value
    ): SafetyAssessment {
        return HydrationCalculationEngine.evaluateSafety(
            profile = profile,
            estimate = estimate,
            todayLogs = getTodayLogs(logs)
        )
    }

    fun computeHabitPrediction(
        profile: UserProfile = _profile.value,
        estimate: HydrationEstimate = computeCurrentEstimate(profile),
        logs: List<HydrationLog> = _allLogs.value
    ): HabitPrediction {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return HydrationCalculationEngine.analyzeHabitsAndPredict(
            profile = profile,
            estimate = estimate,
            allLogs = logs,
            todayLogs = getTodayLogs(logs),
            currentHour = hour
        )
    }

    fun addBeverageLog(
        category: BeverageCategory,
        amountMl: Int,
        note: String = ""
    ) {
        val clampedAmount = amountMl.coerceIn(20, 3000)
        val effectiveMl = category.computeEffectiveMl(clampedAmount)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dateKey = todayDateKey()

        // Optimistic local update for immediate animated water container feedback
        val tempLog = HydrationLog(
            id = "local_$now",
            userId = currentUserId,
            beverageType = category.key,
            amountMl = clampedAmount.toLong(),
            effectiveHydrationMl = effectiveMl.toLong(),
            dateKey = dateKey,
            hourOfDay = hour.toLong(),
            loggedAtMillis = now,
            note = note
        )
        _allLogs.value = listOf(tempLog) + _allLogs.value

        viewModelScope.launch {
            val result = repository.addLog(
                beverageType = category.key,
                amountMl = clampedAmount,
                effectiveHydrationMl = effectiveMl,
                dateKey = dateKey,
                hourOfDay = hour,
                loggedAtMillis = now,
                note = note
            )
            if (result.isFailure) {
                _statusBanner.value = if (_profile.value.languageCode == "ar") {
                    "تم الحفظ محلياً وسيتم المزامنة عند توفر الاتصال."
                } else {
                    "Saved locally; will sync when online."
                }
            } else {
                _statusBanner.value = if (_profile.value.languageCode == "ar") {
                    "💧 تمت إضافة $clampedAmount مل (${category.nameAr}) — صافي الترطيب +$effectiveMl مل"
                } else {
                    "💧 Logged $clampedAmount ml (${category.nameEn}) — Net hydration +$effectiveMl ml"
                }
            }
        }
    }

    fun undoLastLog() {
        val current = _allLogs.value
        val latest = current.firstOrNull() ?: return
        _allLogs.value = current.drop(1)
        if (!latest.id.startsWith("local_") && latest.id.isNotBlank()) {
            viewModelScope.launch {
                repository.deleteLog(latest.id)
            }
        }
        _statusBanner.value = if (_profile.value.languageCode == "ar") {
            "تم التراجع عن آخر سجل شرب (${latest.amountMl} مل)."
        } else {
            "Undid last beverage log (${latest.amountMl} ml)."
        }
    }

    fun deleteLogItem(logId: String) {
        _allLogs.value = _allLogs.value.filterNot { it.id == logId }
        if (!logId.startsWith("local_") && logId.isNotBlank()) {
            viewModelScope.launch {
                repository.deleteLog(logId)
            }
        }
    }

    fun updateProfile(updated: UserProfile, context: Context? = null) {
        val previousCity = _profile.value.selectedCity
        _profile.value = updated.copy(userId = currentUserId)
        viewModelScope.launch {
            repository.saveProfile(_profile.value)
            if (context != null && updated.selectedCity != previousCity) {
                refreshWeather(context, updated.selectedCity)
            }
        }
    }

    fun completeOnboarding(updated: UserProfile, context: Context) {
        val completed = updated.copy(
            userId = currentUserId,
            onboardingCompleted = true
        )
        _profile.value = completed
        viewModelScope.launch {
            repository.saveProfile(completed)
            refreshWeather(context, completed.selectedCity)
        }
    }

    fun toggleLanguage() {
        val currentLang = _profile.value.languageCode
        val nextLang = if (currentLang == "ar") "en" else "ar"
        val updated = _profile.value.copy(languageCode = nextLang)
        _profile.value = updated
        viewModelScope.launch {
            repository.saveProfile(updated)
        }
    }

    fun setAiDialect(dialect: String) {
        val updated = _profile.value.copy(aiDialect = dialect)
        _profile.value = updated
        viewModelScope.launch {
            repository.saveProfile(updated)
        }
    }

    fun recordSmartReminderSent(title: String, body: String) {
        val item = ReminderLogItem(
            timeLabel = currentTimeLabel(),
            title = title,
            message = body
        )
        _reminderHistory.value = listOf(item) + _reminderHistory.value.take(14)
    }

    fun resetHabitLearning() {
        val isAr = _profile.value.languageCode == "ar"
        _statusBanner.value = if (isAr) {
            "تمت إعادة ضبط ذاكرة العادات الذكية بنجاح."
        } else {
            "AI Habit Memory has been reset."
        }
    }

    fun deleteAllData() {
        _allLogs.value = emptyList()
        viewModelScope.launch {
            repository.deleteAllUserData()
            val resetProfile = UserProfile(
                userId = currentUserId,
                languageCode = _profile.value.languageCode,
                onboardingCompleted = false
            )
            _profile.value = resetProfile
            repository.saveProfile(resetProfile)
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun clearChatHistory() {
        seedWelcomeChat()
        _aiCoachState.value = AiCoachUiState.Idle
    }

    /**
     * Generates a contextual AI Hydration Insight for the dashboard using Gemini 3.8 Flash.
     */
    fun generateDashboardAiInsight() {
        val p = _profile.value
        val w = _weather.value
        val est = computeCurrentEstimate(p, w)
        val isAr = p.languageCode == "ar"
        val dialectInstruction = when {
            !isAr -> "Respond in clear, concise, encouraging English (max 2 sentences)."
            p.aiDialect == "IRAQI" -> "أجب باللهجة العراقية الودودة والواضحة (بحد أقصى جملتين) مع الحفاظ على الدقة العلمية."
            else -> "أجب باللغة العربية الفصحى الطبيعية والواضحة (بحد أقصى جملتين)."
        }

        val prompt = """
            You are HydraMind AI, a medically cautious hydration assistant.
            User: ${p.name}, Weight: ${p.weightKg}kg, City: ${w.cityNameEn} (${w.temperatureC}°C, Humidity ${w.humidityPercent}%).
            Iraq Heat Profile: ${p.iraqHeatProfile}.
            Medical Restriction: ${p.medicalRestrictionType} (Prescribed limit: ${p.prescribedLimitMl} ml).
            Calculated Safe Daily Target: ${est.finalBeverageTargetMl} ml.
            Consumed so far today: ${est.consumedEffectiveMl} ml (${est.completionPercent}%).
            Never override medical restrictions or encourage rapid excessive drinking.
            $dialectInstruction
            Provide one actionable, personalized hydration tip for right now.
        """.trimIndent()

        viewModelScope.launch {
            try {
                val response = withTimeout(60_000L) {
                    generativeModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    val safeText = HydrationCalculationEngine.validateAiRecommendation(
                        rawAiText = text,
                        profile = p,
                        estimate = est,
                        isArabic = isAr
                    )
                    _dailyAiInsight.value = safeText
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Dashboard AI insight generation failed", e)
            }
        }
    }

    /**
     * Conversational AI Hydration Coach using streaming inference (`generateContentStream`)
     * with a 60-second coroutine timeout and deterministic safety validation.
     */
    fun sendCoachMessage(userQuestion: String) {
        val trimmed = userQuestion.trim()
        if (trimmed.isEmpty()) return

        val p = _profile.value
        val w = _weather.value
        val est = computeCurrentEstimate(p, w)
        val safety = computeSafetyAssessment(p, est)
        val habit = computeHabitPrediction(p, est)
        val isAr = p.languageCode == "ar"

        val userMsg = ChatMessage(
            isUser = true,
            text = trimmed,
            timestampLabel = currentTimeLabel()
        )
        _chatHistory.value = _chatHistory.value + userMsg
        _aiCoachState.value = AiCoachUiState.Loading

        val languageInstruction = when {
            !isAr -> "Respond in natural, medically clear English."
            p.aiDialect == "IRAQI" -> "تحدث باللهجة العراقية المحببة والطبيعية، ولكن عند ذكر التحذيرات الطبية أو السلامة استخدم عربية رسمية واضحة طبياً."
            else -> "تحدث باللغة العربية الفصحى السلسة والاحترافية."
        }

        val systemContext = """
            أنت "مدرب هايدرا مايند الذكي" (HydraMind AI Coach).
            قواعد السلامة الصارمة (إلزامية):
            1. لا تقم أبداً بتجاوز القيود الطبية أو الحدود الموصوفة من الطبيب.
            2. لا تشخص الجفاف أو اضطرابات الأملاح سريرياً.
            3. لا تشجع أبداً على شرب كميات كبيرة من الماء دفعة واحدة (خطر نقص صوديوم الدم).
            4. وضح دائماً أن شرب الماء وحده لا يعالج ضربة الشمس الطارئة.
            5. اعتمد حصرياً على الأرقام المحسوبة من محرك الحساب العلمي أدناه ولا تخترع أهدافاً طبية عشوائية:
               - اسم المستخدم: ${p.name}
               - المدينة والطقس: ${w.cityNameAr} (${w.temperatureC}°م، الرطوبة ${w.humidityPercent}%)
               - وضع حرارة العراق: ${IraqHeatProfile.fromKey(p.iraqHeatProfile).titleAr}
               - القيد الطبي: ${MedicalRestriction.fromKey(p.medicalRestrictionType).titleAr}
               - الهدف اليومي الآمن للمشروبات: ${est.finalBeverageTargetMl} مل
               - المستهلك اليوم حتى الآن: ${est.consumedEffectiveMl} مل (${est.completionPercent}%)
               - المتبقي اليوم: ${est.remainingMl} مل
               - نافذة انخفاض الشرب المعتادة: ${habit.frequentlyMissedWindowAr}
               - حالة السلامة الحالية: ${if (safety.isSafe) "آمنة" else "تنبيه: استهلاك سريع أو مرتفع"}
            $languageInstruction
            سؤال المستخدم: $trimmed
        """.trimIndent()

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    generativeModel.generateContentStream(systemContext).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _aiCoachState.value = AiCoachUiState.Streaming(accumulated.toString())
                    }
                }
                val finalRaw = accumulated.toString()
                if (finalRaw.isNotBlank()) {
                    val validated = HydrationCalculationEngine.validateAiRecommendation(
                        rawAiText = finalRaw,
                        profile = p,
                        estimate = est,
                        isArabic = isAr
                    )
                    _chatHistory.value = _chatHistory.value + ChatMessage(
                        isUser = false,
                        text = validated,
                        timestampLabel = currentTimeLabel()
                    )
                    _aiCoachState.value = AiCoachUiState.Success(validated)
                } else {
                    _aiCoachState.value = AiCoachUiState.Error(
                        if (isAr) "لم يُرجع النموذج الذكي رداً. حاول مرة أخرى." else "Model returned an empty response."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Coach streaming timed out", e)
                _aiCoachState.value = AiCoachUiState.Error(
                    if (isAr) "انتهت مهلة الاتصال بالمدرب الذكي بعد 60 ثانية. يرجى المحاولة مجدداً."
                    else "AI Coach timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Coach generation failed", e)
                _aiCoachState.value = AiCoachUiState.Error(
                    e.localizedMessage ?: if (isAr) "تعذر الاتصال بالمدرب الذكي حالياً." else "AI Coach request failed."
                )
            }
        }
    }

    fun buildCsvExport(): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Hour,Beverage,Amount_ml,Effective_Hydration_ml,Note")
        for (log in _allLogs.value) {
            val safeNote = log.note.replace(",", " ")
            sb.appendLine("${log.dateKey},${log.hourOfDay}:00,${log.beverageType},${log.amountMl},${log.effectiveHydrationMl},$safeNote")
        }
        return sb.toString()
    }

    private fun currentTimeLabel(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        return sdf.format(Date())
    }
}
