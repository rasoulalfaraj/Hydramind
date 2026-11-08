package com.example.domain

import com.example.data.model.HydrationLog
import com.example.data.model.IraqHeatProfile
import com.example.data.model.MedicalRestriction
import com.example.data.model.UserProfile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class HydrationEstimate(
    val totalWaterNeedsMl: Int,
    val baselineBeverageMl: Int,
    val activityAdjustmentMl: Int,
    val environmentAdjustmentMl: Int,
    val finalBeverageTargetMl: Int,
    val consumedEffectiveMl: Int,
    val consumedRawMl: Int,
    val remainingMl: Int,
    val completionPercent: Int,
    val suggestedSingleServingMl: Int,
    val confidenceNoteAr: String,
    val confidenceNoteEn: String,
    val explanationAr: String,
    val explanationEn: String,
    val medicalLocked: Boolean,
    val safetyNoteAr: String,
    val safetyNoteEn: String
)

data class SafetyAssessment(
    val isSafe: Boolean,
    val rapidIntakeDetected: Boolean,
    val lastHourIntakeMl: Int,
    val excessiveDailyIntake: Boolean,
    val medicalLimitExceeded: Boolean,
    val warningBannerAr: String?,
    val warningBannerEn: String?,
    val educationalNoteAr: String,
    val educationalNoteEn: String
)

data class HabitPrediction(
    val goalProbabilityPercent: Int,
    val expectedEndOfDayMl: Int,
    val frequentlyMissedWindowAr: String,
    val frequentlyMissedWindowEn: String,
    val suggestedExtraReminderHour: Int,
    val averageServingMl: Int,
    val peakDrinkingHour: Int,
    val habitInsightAr: String,
    val habitInsightEn: String
)

object HydrationCalculationEngine {

    /**
     * Science-based hydration calculation engine (Section 7).
     * Separates total water needs (food + beverages) from beverage fluid intake (~80%),
     * plus cautious adjustments for physical exertion and thermal stress.
     * Strictly respects physician-directed fluid limits and medical restrictions.
     */
    fun calculateHydration(
        profile: UserProfile,
        ambientTempC: Double,
        feelsLikeC: Double,
        humidityPercent: Int,
        todayLogs: List<HydrationLog>
    ): HydrationEstimate {
        val weight = profile.weightKg.toInt().coerceIn(25, 220)
        val age = profile.age.toInt().coerceIn(10, 100)

        // 1. Baseline Total Water Intake (Food + Beverages) based on age & body mass guidance (EFSA / NAM principles)
        val mlPerKgTotal = when {
            age < 30 -> 38.0
            age <= 55 -> 34.0
            else -> 30.0
        }
        val sexAdjustmentTotal = when (profile.biologicalSex.uppercase()) {
            "MALE" -> 150
            "FEMALE" -> 0
            else -> 75
        }
        val baseTotalWaterMl = ((weight * mlPerKgTotal).roundToInt() + sexAdjustmentTotal).coerceIn(1600, 4200)

        // ~80% of total water comes from beverages, ~20% from moisture in food
        val baselineBeverageMl = (baseTotalWaterMl * 0.80).roundToInt()

        val restriction = MedicalRestriction.fromKey(profile.medicalRestrictionType)
        val hasMedicalRestriction = restriction.hasRestriction || profile.prescribedLimitMl > 0L

        // 2. Physical Activity & Exertion Adjustment (only if no medical restriction)
        val rawActivityAddMl = if (hasMedicalRestriction) {
            0
        } else {
            val levelBase = when (profile.activityLevel.uppercase()) {
                "LOW" -> 0
                "MODERATE" -> 200
                "HIGH" -> 400
                "ATHLETE" -> 600
                else -> 150
            }
            val exerciseAdd = ((profile.exerciseMinutes.toInt().coerceIn(0, 240) / 30.0) * 220.0).roundToInt()
            (levelBase + exerciseAdd).coerceIn(0, 1200)
        }

        // 3. Environmental / Heat Stress Adjustment (cautious band-based assessment, NOT arbitrary per-degree formula)
        val effectiveThermalC = max(ambientTempC, feelsLikeC)
        val rawEnvAddMl = if (hasMedicalRestriction) {
            0
        } else {
            val heatBandAdd = when {
                effectiveThermalC >= 44.0 -> 650 // Extreme Iraq summer heat
                effectiveThermalC >= 38.0 -> 450 // Severe heat
                effectiveThermalC >= 32.0 -> 250 // Warm/Hot
                effectiveThermalC >= 27.0 -> 120 // Mildly warm
                else -> 0
            }
            val outdoorFactor = when {
                profile.outdoorMinutes >= 120 -> 200
                profile.outdoorMinutes >= 45 -> 100
                else -> 0
            }
            val sweatFactor = when (profile.sweatingLevel.uppercase()) {
                "HIGH" -> 180
                "MODERATE" -> 70
                else -> 0
            }
            val humidityFactor = if (humidityPercent >= 65 && effectiveThermalC >= 30.0) 100 else 0
            (heatBandAdd + outdoorFactor + sweatFactor + humidityFactor).coerceIn(0, 1100)
        }

        val unclampedTarget = baselineBeverageMl + rawActivityAddMl + rawEnvAddMl

        val finalTargetMl = if (profile.prescribedLimitMl in 500L..6000L) {
            profile.prescribedLimitMl.toInt()
        } else if (hasMedicalRestriction) {
            baselineBeverageMl.coerceAtMost(2000)
        } else {
            unclampedTarget.coerceIn(1500, 4500)
        }

        val consumedEffectiveMl = todayLogs.sumOf { it.effectiveHydrationMl.toInt() }
        val consumedRawMl = todayLogs.sumOf { it.amountMl.toInt() }
        val remainingMl = max(0, finalTargetMl - consumedEffectiveMl)
        val completionPercent = if (finalTargetMl > 0) {
            ((consumedEffectiveMl.toDouble() / finalTargetMl.toDouble()) * 100.0).roundToInt().coerceIn(0, 150)
        } else 0

        val wakingHours = max(8, (profile.bedHour - profile.wakeHour).toInt().let { if (it <= 0) it + 24 else it })
        val suggestedSingleServingMl = (finalTargetMl / max(6, wakingHours - 2))
            .let { ((it + 25) / 50) * 50 }
            .coerceIn(150, 300)

        val totalWaterWithAdd = (finalTargetMl / 0.80).roundToInt()

        val explanationAr = if (hasMedicalRestriction) {
            "تم تثبيت هدف المشروبات اليومي عند $finalTargetMl مل احتراماً للقيود الطبية المسجلة (${restriction.titleAr})، مع إيقاف الزيادات التلقائية للحرارة أو الرياضة."
        } else {
            "إجمالي احتياج الماء التقديري (طعام + مشروبات): $totalWaterWithAdd مل. " +
                "حصة المشروبات الأساسية: $baselineBeverageMl مل، " +
                "مع إضافة $rawActivityAddMl مل للنشاط البدني و$rawEnvAddMl مل للتكيف مع حرارة الجو (${ambientTempC.roundToInt()}°م) والتعرض الخارجي."
        }

        val explanationEn = if (hasMedicalRestriction) {
            "Daily beverage target locked at $finalTargetMl ml due to your medical profile (${restriction.titleEn}). Automatic weather/activity increases are disabled."
        } else {
            "Estimated total water need (food + drink): $totalWaterWithAdd ml. " +
                "Baseline beverages: $baselineBeverageMl ml + $rawActivityAddMl ml activity + $rawEnvAddMl ml thermal adaptation (${ambientTempC.roundToInt()}°C)."
        }

        return HydrationEstimate(
            totalWaterNeedsMl = totalWaterWithAdd,
            baselineBeverageMl = baselineBeverageMl,
            activityAdjustmentMl = rawActivityAddMl,
            environmentAdjustmentMl = rawEnvAddMl,
            finalBeverageTargetMl = finalTargetMl,
            consumedEffectiveMl = consumedEffectiveMl,
            consumedRawMl = consumedRawMl,
            remainingMl = remainingMl,
            completionPercent = completionPercent,
            suggestedSingleServingMl = suggestedSingleServingMl,
            confidenceNoteAr = "تقدير إرشادي بنطاق ±250 مل يعتمد على البيانات المدخلة؛ لا يُعد قياساً سريرياً.",
            confidenceNoteEn = "Guideline estimate (±250 ml uncertainty band); not a clinical diagnostic measurement.",
            explanationAr = explanationAr,
            explanationEn = explanationEn,
            medicalLocked = hasMedicalRestriction,
            safetyNoteAr = restriction.cautionAr,
            safetyNoteEn = restriction.cautionEn
        )
    }

    /**
     * Deterministic Safety Rules Engine (Section 11 - Safe Hydration Mode).
     * Protects users from overhydration / hyponatremia, rapid intake, and medical limit breaches.
     */
    fun evaluateSafety(
        profile: UserProfile,
        estimate: HydrationEstimate,
        todayLogs: List<HydrationLog>,
        nowMillis: Long = System.currentTimeMillis()
    ): SafetyAssessment {
        val oneHourAgo = nowMillis - 60 * 60 * 1000L
        val lastHourMl = todayLogs
            .filter { it.loggedAtMillis >= oneHourAgo }
            .sumOf { it.amountMl.toInt() }

        val rapidIntake = lastHourMl >= 850
        val restriction = MedicalRestriction.fromKey(profile.medicalRestrictionType)
        val medicalExceeded = (profile.prescribedLimitMl > 0L && estimate.consumedRawMl > profile.prescribedLimitMl) ||
            (restriction.hasRestriction && estimate.consumedRawMl > estimate.finalBeverageTargetMl)
        val excessiveDaily = estimate.consumedRawMl > max(4200, (estimate.finalBeverageTargetMl * 1.35).roundToInt())

        val warningAr = when {
            medicalExceeded -> "⚠️ تنبيه سلامة طبي: تجاوزت السوائل المسجلة اليوم (${estimate.consumedRawMl} مل) الحد الموصى به لحالتك الصحية (${estimate.finalBeverageTargetMl} مل). توقف عن زيادة السوائل واستشر طبيبك عند الشعور بأي أعراض."
            rapidIntake -> "⚠️ سجلت كمية كبيرة من السوائل ($lastHourMl مل) خلال فترة قصيرة. تجنب شرب كميات إضافية بسرعة للوقاية من نقص صوديوم الدم، واطلب المشورة الطبية إذا ظهرت أعراض غير طبيعية."
            excessiveDaily -> "⚠️ إجمالي استهلاكك اليوم مرتفع جداً (${estimate.consumedRawMl} مل). الإفراط في شرب الماء لا يزيد الفائدة الصحية وقد يخل بتوازن الأملاح."
            else -> null
        }

        val warningEn = when {
            medicalExceeded -> "⚠️ Medical Safety Alert: Logged intake (${estimate.consumedRawMl} ml) exceeds your restricted ceiling (${estimate.finalBeverageTargetMl} ml). Pause fluid intake and follow your physician's advice."
            rapidIntake -> "⚠️ Your recorded fluid intake ($lastHourMl ml in the last hour) is unusually high over a short period. Avoid rapid additional consumption and seek medical advice if symptoms occur."
            excessiveDaily -> "⚠️ Daily fluid intake is unusually high (${estimate.consumedRawMl} ml). Excessive water intake can dilute blood electrolytes (hyponatremia)."
            else -> null
        }

        return SafetyAssessment(
            isSafe = !rapidIntake && !medicalExceeded && !excessiveDaily,
            rapidIntakeDetected = rapidIntake,
            lastHourIntakeMl = lastHourMl,
            excessiveDailyIntake = excessiveDaily,
            medicalLimitExceeded = medicalExceeded,
            warningBannerAr = warningAr,
            warningBannerEn = warningEn,
            educationalNoteAr = "الترطيب الآمن يعني توزيع شرب الماء باعتدال طوال ساعات الاستيقاظ (بمعدل لا يتجاوز 700-800 مل في الساعة)، لأن الكلى السليمة تطرح نحو 0.8 إلى 1.0 لتر في الساعة كحد أقصى.",
            educationalNoteEn = "Safe hydration distributes intake steadily across waking hours (staying below 700-800 ml/hour), respecting normal renal excretion limits."
        )
    }

    /**
     * Validates AI-generated text through the deterministic Safety Rules Engine
     * before displaying to the user (Section 11).
     */
    fun validateAiRecommendation(
        rawAiText: String,
        profile: UserProfile,
        estimate: HydrationEstimate,
        isArabic: Boolean
    ): String {
        val restriction = MedicalRestriction.fromKey(profile.medicalRestrictionType)
        val hasRestriction = restriction.hasRestriction || profile.prescribedLimitMl > 0L
        val prefix = if (hasRestriction) {
            if (isArabic) {
                "🛡️ [تمت المراجعة بواسطة محرك الترطيب الآمن - سقفك الطبي ${estimate.finalBeverageTargetMl} مل]:\n"
            } else {
                "🛡️ [Validated by Safe Hydration Engine - Medical Ceiling ${estimate.finalBeverageTargetMl} ml]:\n"
            }
        } else ""
        return prefix + rawAiText.trim()
    }

    /**
     * Behavioral Habit Memory & Predictive Hydration Analytics (Sections 10 & 15).
     */
    fun analyzeHabitsAndPredict(
        profile: UserProfile,
        estimate: HydrationEstimate,
        allLogs: List<HydrationLog>,
        todayLogs: List<HydrationLog>,
        currentHour: Int
    ): HabitPrediction {
        val wake = profile.wakeHour.toInt().coerceIn(5, 11)
        val bed = profile.bedHour.toInt().coerceIn(20, 23)
        val totalWakingHours = max(10, bed - wake)
        val elapsedHours = (currentHour - wake).coerceIn(1, totalWakingHours)
        val remainingHours = max(0, bed - currentHour)

        val avgServing = if (allLogs.isNotEmpty()) {
            allLogs.map { it.amountMl.toInt() }.average().roundToInt().coerceIn(100, 500)
        } else 250

        // Find peak drinking hour & missed window from historical logs
        val hourCounts = IntArray(24)
        for (log in allLogs) {
            val h = log.hourOfDay.toInt().coerceIn(0, 23)
            hourCounts[h]++
        }

        val peakHour = (wake..bed).maxByOrNull { hourCounts[it] } ?: 9

        // Identify 3-hour window during waking hours with lowest logged frequency
        var lowestWindowStart = 10
        var lowestCount = Int.MAX_VALUE
        for (start in (wake + 1)..max(wake + 1, bed - 3)) {
            val count = hourCounts[start] + hourCounts[(start + 1) % 24] + hourCounts[(start + 2) % 24]
            if (count < lowestCount) {
                lowestCount = count
                lowestWindowStart = start
            }
        }
        val suggestedHour = (lowestWindowStart + 1).coerceIn(8, 21)

        val currentPaceMlPerHour = estimate.consumedEffectiveMl.toDouble() / elapsedHours.toDouble()
        val historicalAvgPerHour = if (allLogs.size >= 4) {
            (estimate.finalBeverageTargetMl * 0.85) / totalWakingHours
        } else {
            (estimate.finalBeverageTargetMl * 0.78) / totalWakingHours
        }

        val blendedFutureRate = (currentPaceMlPerHour * 0.55) + (historicalAvgPerHour * 0.45)
        val expectedEndOfDayMl = (estimate.consumedEffectiveMl + (blendedFutureRate * remainingHours))
            .roundToInt()
            .coerceIn(estimate.consumedEffectiveMl, (estimate.finalBeverageTargetMl * 1.25).roundToInt())

        val rawRatio = if (estimate.finalBeverageTargetMl > 0) {
            expectedEndOfDayMl.toDouble() / estimate.finalBeverageTargetMl.toDouble()
        } else 1.0

        val goalProbability = when {
            estimate.completionPercent >= 100 -> 99
            else -> (rawRatio * 92.0).roundToInt().coerceIn(15, 96)
        }

        val windowStartLabelAr = formatHourAr(lowestWindowStart)
        val windowEndLabelAr = formatHourAr(lowestWindowStart + 3)
        val suggestedHourAr = formatHourAr(suggestedHour)

        val windowStartLabelEn = formatHourEn(lowestWindowStart)
        val windowEndLabelEn = formatHourEn(lowestWindowStart + 3)
        val suggestedHourEn = formatHourEn(suggestedHour)

        val insightAr = if (profile.aiDialect == "IRAQI") {
            "لاحظت إنك غالباً تنسى تشرب مي بين $windowStartLabelAr و$windowEndLabelAr. تحب أضيفلك تذكير ذكي بـ $suggestedHourAr؟"
        } else {
            "لاحظت أن استهلاكك للماء ينخفض عادةً بين $windowStartLabelAr و$windowEndLabelAr. هل ترغب في تفعيل تذكير ذكي عند $suggestedHourAr؟"
        }

        val insightEn = "You often miss hydration reminders between $windowStartLabelEn and $windowEndLabelEn. Would you like an adaptive reminder at $suggestedHourEn?"

        return HabitPrediction(
            goalProbabilityPercent = goalProbability,
            expectedEndOfDayMl = expectedEndOfDayMl,
            frequentlyMissedWindowAr = "$windowStartLabelAr - $windowEndLabelAr",
            frequentlyMissedWindowEn = "$windowStartLabelEn - $windowEndLabelEn",
            suggestedExtraReminderHour = suggestedHour,
            averageServingMl = avgServing,
            peakDrinkingHour = peakHour,
            habitInsightAr = insightAr,
            habitInsightEn = insightEn
        )
    }

    fun getIraqHeatGuidance(
        tempC: Double,
        feelsLikeC: Double,
        profileKey: String
    ): Pair<String, String> {
        val profile = IraqHeatProfile.fromKey(profileKey)
        val maxTemp = max(tempC, feelsLikeC)
        val alertAr = when {
            maxTemp >= 45.0 -> "☀️ درجة الحرارة مرتفعة جداً اليوم (${tempC.roundToInt()}°م). قلّل التعرض المباشر للشمس، وخذ استراحات تبريد منتظمة كل ${profile.coolingBreakMinutes} دقيقة بمكان بارد. انتبه: شرب الماء وحده لا يعالج ضربة الشمس الطارئة."
            maxTemp >= 38.0 -> "☀️ أجواء حارة في منطقتك (${tempC.roundToInt()}°م). وفقاً لـ «${profile.titleAr}»، يُنصح بشرب دفعات معتدلة كل ${profile.coolingBreakMinutes} دقيقة وتجنب أشعة الشمس المباشرة وقت الظهيرة."
            else -> "🌤️ الأجواء معتدلة نسبياً (${tempC.roundToInt()}°م). حافظ على وتيرة شرب منتظمة وفق «${profile.titleAr}»."
        }
        val alertEn = when {
            maxTemp >= 45.0 -> "☀️ Extreme heat today (${tempC.roundToInt()}°C). Limit direct sun exposure and take cooling breaks every ${profile.coolingBreakMinutes} min. Note: water alone cannot treat medical heatstroke."
            maxTemp >= 38.0 -> "☀️ High heat conditions (${tempC.roundToInt()}°C). Under '${profile.titleEn}', sip moderately every ${profile.coolingBreakMinutes} min and seek shade at midday."
            else -> "🌤️ Moderate thermal conditions (${tempC.roundToInt()}°C). Maintain steady hydration under '${profile.titleEn}'."
        }
        return alertAr to alertEn
    }

    private fun formatHourAr(hour24: Int): String {
        val h = ((hour24 % 24) + 24) % 24
        return when {
            h == 0 -> "12 منتصف الليل"
            h < 12 -> "الساعة $h صباحاً"
            h == 12 -> "الساعة 12 ظهراً"
            else -> "الساعة ${h - 12} مساءً"
        }
    }

    private fun formatHourEn(hour24: Int): String {
        val h = ((hour24 % 24) + 24) % 24
        return when {
            h == 0 -> "12:00 AM"
            h < 12 -> "$h:00 AM"
            h == 12 -> "12:00 PM"
            else -> "${h - 12}:00 PM"
        }
    }
}
