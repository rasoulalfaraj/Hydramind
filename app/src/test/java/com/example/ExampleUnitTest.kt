package com.example

import com.example.data.model.BeverageCategory
import com.example.data.model.HydrationLog
import com.example.data.model.UserProfile
import com.example.domain.HydrationCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun hydrationEngine_medicalRestriction_locksAutomaticHeatIncreases() {
        val restrictedProfile = UserProfile(
            weightKg = 80L,
            age = 45L,
            activityLevel = "HIGH",
            outdoorMinutes = 120L,
            medicalRestrictionType = "KIDNEY",
            prescribedLimitMl = 1500L
        )
        val estimate = HydrationCalculationEngine.calculateHydration(
            profile = restrictedProfile,
            ambientTempC = 46.0,
            feelsLikeC = 48.0,
            humidityPercent = 30,
            todayLogs = emptyList()
        )
        assertTrue(estimate.medicalLocked)
        assertEquals(0, estimate.activityAdjustmentMl)
        assertEquals(0, estimate.environmentAdjustmentMl)
        assertEquals(1500, estimate.finalBeverageTargetMl)
    }

    @Test
    fun safetyEngine_detectsRapidFluidIntakeWithinOneHour() {
        val now = System.currentTimeMillis()
        val profile = UserProfile(weightKg = 70L, age = 28L)
        val logs = listOf(
            HydrationLog(amountMl = 500L, effectiveHydrationMl = 500L, loggedAtMillis = now - 10 * 60 * 1000L),
            HydrationLog(amountMl = 450L, effectiveHydrationMl = 450L, loggedAtMillis = now - 25 * 60 * 1000L)
        )
        val estimate = HydrationCalculationEngine.calculateHydration(
            profile = profile,
            ambientTempC = 35.0,
            feelsLikeC = 36.0,
            humidityPercent = 25,
            todayLogs = logs
        )
        val safety = HydrationCalculationEngine.evaluateSafety(profile, estimate, logs, now)
        assertFalse(safety.isSafe)
        assertTrue(safety.rapidIntakeDetected)
        assertNotNull(safety.warningBannerAr)
    }

    @Test
    fun beverageCategory_computesDifferentPhysiologicalContributions() {
        assertEquals(250, BeverageCategory.WATER.computeEffectiveMl(250))
        assertTrue(BeverageCategory.COFFEE.computeEffectiveMl(250) < 250)
        assertTrue(BeverageCategory.MILK.computeEffectiveMl(250) > BeverageCategory.COFFEE.computeEffectiveMl(250))
    }
}
