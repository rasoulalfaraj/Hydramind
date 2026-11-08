package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

import org.robolectric.annotation.Config

@Config(sdk = [36])
class HydraMindRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfileAndAddLog_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = HydraMindRepository(firestore)

        val saveProfileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProfile(UserProfile(userId = aliceUid, name = "أحمد", onboardingCompleted = true))
        }
        assertTrue(saveProfileResult.isSuccess)

        val logIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addLog(
                beverageType = "WATER",
                amountMl = 250,
                effectiveHydrationMl = 250,
                dateKey = "2026-11-07",
                hourOfDay = 10,
                loggedAtMillis = System.currentTimeMillis(),
                note = "ماء بارد"
            )
        }
        assertTrue(logIdResult.isSuccess)
        val logId = logIdResult.getOrThrow()

        val emittedLogs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLogs(aliceUid).first { list -> list.any { it.id == logId } }
        }
        assertTrue(emittedLogs.any { it.id == logId })
    }

    @Test
    fun getProfileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = HydraMindRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveProfile(UserProfile(userId = aliceUid, name = "أحمد")).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = HydraMindRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfileById(aliceUid)
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeLogs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = HydraMindRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLogs(aliceUid).first()
            }
            fail("Expected exception for unauthenticated observeLogs")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@hydramind.test"
        const val BOB_EMAIL = "bob@hydramind.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
