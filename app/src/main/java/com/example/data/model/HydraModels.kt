package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "مستخدم هايدرا",
    val age: Long = 28L,
    val weightKg: Long = 72L,
    val heightCm: Long = 172L,
    val biologicalSex: String = "MALE",
    val activityLevel: String = "MODERATE",
    val workEnvironment: String = "INDOOR",
    val outdoorMinutes: Long = 30L,
    val exerciseMinutes: Long = 30L,
    val sweatingLevel: String = "MODERATE",
    val wakeHour: Long = 7L,
    val bedHour: Long = 23L,
    val medicalRestrictionType: String = "NONE",
    val prescribedLimitMl: Long = 0L,
    val iraqHeatProfile: String = "GENERAL_SUMMER",
    val selectedCity: String = "Baghdad",
    val languageCode: String = "ar",
    val aiDialect: String = "MSA",
    val habitLearningEnabled: Boolean = true,
    val reminderIntervalMinutes: Long = 60L,
    val remindersEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val streakDays: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class HydrationLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val beverageType: String = "WATER",
    val amountMl: Long = 250L,
    val effectiveHydrationMl: Long = 250L,
    val dateKey: String = "",
    val hourOfDay: Long = 10L,
    val loggedAtMillis: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class BeverageCategory(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val hydrationFactor: Double,
    val emoji: String,
    val noteAr: String,
    val noteEn: String
) {
    WATER(
        key = "WATER",
        nameAr = "ماء نقي",
        nameEn = "Pure Water",
        hydrationFactor = 1.0,
        emoji = "💧",
        noteAr = "امتصاص كامل ومباشر بنسبة 100% دون سعرات أو كافيين.",
        noteEn = "100% direct hydration efficiency with zero calories or caffeine."
    ),
    TEA(
        key = "TEA",
        nameAr = "شاي",
        nameEn = "Tea",
        hydrationFactor = 0.90,
        emoji = "🍵",
        noteAr = "يساهم بنسبة ~90% في الترطيب مع تأثير مدر خفيف بسبب الكافيين.",
        noteEn = "Contributes ~90% net hydration due to mild caffeine diuresis."
    ),
    COFFEE(
        key = "COFFEE",
        nameAr = "قهوة",
        nameEn = "Coffee",
        hydrationFactor = 0.82,
        emoji = "☕",
        noteAr = "تساهم بنسبة ~82% في الترطيب؛ لا تعوض الماء النقي بالكامل خاصة بالحر.",
        noteEn = "Contributes ~82% net hydration; does not fully replace pure water in heat."
    ),
    MILK(
        key = "MILK",
        nameAr = "حليب / لبن",
        nameEn = "Milk / Laban",
        hydrationFactor = 0.95,
        emoji = "🥛",
        noteAr = "ترطيب ممتاز (~95%) بفضل الإلكتروليتات الطبيعية التي تطيل بقاء السوائل.",
        noteEn = "High retention (~95%) thanks to natural electrolytes and nutrients."
    ),
    JUICE(
        key = "JUICE",
        nameAr = "عصير طبيعي",
        nameEn = "Natural Juice",
        hydrationFactor = 0.88,
        emoji = "🍊",
        noteAr = "يساهم بنسبة ~88% في الترطيب مع الانتباه لمحتوى السكريات.",
        noteEn = "Contributes ~88% net hydration; be mindful of natural sugars."
    ),
    OTHER(
        key = "OTHER",
        nameAr = "مشروب آخر",
        nameEn = "Other Beverage",
        hydrationFactor = 0.85,
        emoji = "🥤",
        noteAr = "مساهمة تقديرية بنسبة ~85% في الترطيب اليومي.",
        noteEn = "Estimated ~85% contribution toward daily fluid intake."
    );

    fun computeEffectiveMl(amountMl: Int): Int {
        return (amountMl * hydrationFactor).roundToInt().coerceAtLeast(5)
    }

    companion object {
        fun fromKey(key: String): BeverageCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: WATER
    }
}

enum class IraqHeatProfile(
    val key: String,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val coolingBreakMinutes: Int
) {
    GENERAL_SUMMER(
        key = "GENERAL_SUMMER",
        titleAr = "الوضع الصيفي العام",
        titleEn = "General Summer Mode",
        subtitleAr = "حماية يومية متوازنة لأجواء الصيف الحارة في العراق.",
        subtitleEn = "Balanced daily protection for hot Iraqi summer days.",
        coolingBreakMinutes = 90
    ),
    OUTDOOR_WORKER(
        key = "OUTDOOR_WORKER",
        titleAr = "وضع العمل الميداني والخارجي",
        titleEn = "Outdoor Worker Mode",
        subtitleAr = "تذكيرات مكثفة واستراحات ظل وتبريد كل 45 دقيقة لتفادي الإجهاد الحراري.",
        subtitleEn = "Frequent hydration and shade breaks every 45 min against heat stress.",
        coolingBreakMinutes = 45
    ),
    ATHLETE(
        key = "ATHLETE",
        titleAr = "وضع الرياضيين والنشاط العالي",
        titleEn = "Athlete Mode",
        subtitleAr = "تعويض مدروس للسوائل والتعرق مع تنبيهات توازن الأملاح.",
        subtitleEn = "Structured sweat replacement and electrolyte balance awareness.",
        coolingBreakMinutes = 40
    ),
    COMMUTER(
        key = "COMMUTER",
        titleAr = "وضع التنقل والمواصلات",
        titleEn = "Commuter Mode",
        subtitleAr = "تنبيهات لحمل مطارة الماء المبردة قبل الخروج في أوقات الذروة (11 ص - 4 م).",
        subtitleEn = "Pre-commute insulated bottle alerts during peak UV hours (11 AM - 4 PM).",
        coolingBreakMinutes = 60
    ),
    INDOOR_OFFICE(
        key = "INDOOR_OFFICE",
        titleAr = "وضع المكاتب والتكييف",
        titleEn = "Indoor Office Mode",
        subtitleAr = "معالجة الجفاف الصامت الناتج عن هواء التكييف الجاف وقلة الشعور بالعطش.",
        subtitleEn = "Counteracts dry air-conditioning dehydration and blunted thirst cues.",
        coolingBreakMinutes = 75
    );

    companion object {
        fun fromKey(key: String): IraqHeatProfile =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: GENERAL_SUMMER
    }
}

enum class MedicalRestriction(
    val key: String,
    val titleAr: String,
    val titleEn: String,
    val cautionAr: String,
    val cautionEn: String
) {
    NONE(
        key = "NONE",
        titleAr = "لا توجد قيود طبية",
        titleEn = "No Medical Restrictions",
        cautionAr = "تقديرات علمية عامة للأشخاص الأصحاء.",
        cautionEn = "Standard science-based estimates for healthy individuals."
    ),
    KIDNEY(
        key = "KIDNEY",
        titleAr = "قيود متعلقة بالكلى",
        titleEn = "Kidney-Related Restriction",
        cautionAr = "⚠️ تنبيه طبي: لن يقوم التطبيق بزيادة هدف السوائل تلقائياً بسبب الحرارة أو النشاط. التزم بتوجيهات طبيبك المختص.",
        cautionEn = "Medical Notice: Automatic heat/activity target increases are disabled. Follow your nephrologist's guidance."
    ),
    HEART(
        key = "HEART",
        titleAr = "قيود متعلقة بالقلب",
        titleEn = "Heart-Related Restriction",
        cautionAr = "⚠️ تنبيه طبي: تم إيقاف الزيادات التلقائية للسوائل لحماية توازن السوائل لديك.",
        cautionEn = "Medical Notice: Automatic fluid increases are locked to protect fluid balance."
    ),
    FLUID_RETENTION(
        key = "FLUID_RETENTION",
        titleAr = "احتباس السوائل",
        titleEn = "Fluid Retention Condition",
        cautionAr = "⚠️ تنبيه طبي: يتم احترام الحد الأعلى للسوائل الموصى به طبياً دون تجاوز.",
        cautionEn = "Medical Notice: Strict adherence to your safe fluid ceiling."
    ),
    PHYSICIAN_LIMIT(
        key = "PHYSICIAN_LIMIT",
        titleAr = "حد يومي محدد من الطبيب",
        titleEn = "Physician-Prescribed Daily Limit",
        cautionAr = "⚠️ يتم تطبيق الحد الطبي الموصوف كسقف صارم لا يمكن للذكاء الاصطناعي تجاوزه.",
        cautionEn = "Your physician-prescribed limit is enforced as a strict ceiling."
    );

    val hasRestriction: Boolean
        get() = this != NONE

    companion object {
        fun fromKey(key: String): MedicalRestriction =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: NONE
    }
}
