package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.HydrationLog
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HydraMindRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getProfileById(userId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            val profile = if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(profile: UserProfile): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to profile.name.ifBlank { "مستخدم هايدرا" }.take(80),
                    "age" to profile.age.coerceIn(5L, 120L),
                    "weightKg" to profile.weightKg.coerceIn(15L, 300L),
                    "heightCm" to profile.heightCm.coerceIn(50L, 250L),
                    "biologicalSex" to profile.biologicalSex.ifBlank { "MALE" },
                    "activityLevel" to profile.activityLevel.ifBlank { "MODERATE" },
                    "workEnvironment" to profile.workEnvironment.ifBlank { "INDOOR" },
                    "outdoorMinutes" to profile.outdoorMinutes.coerceIn(0L, 1440L),
                    "exerciseMinutes" to profile.exerciseMinutes.coerceIn(0L, 600L),
                    "sweatingLevel" to profile.sweatingLevel.ifBlank { "MODERATE" },
                    "wakeHour" to profile.wakeHour.coerceIn(0L, 23L),
                    "bedHour" to profile.bedHour.coerceIn(0L, 23L),
                    "medicalRestrictionType" to profile.medicalRestrictionType.ifBlank { "NONE" },
                    "prescribedLimitMl" to profile.prescribedLimitMl.coerceIn(0L, 10000L),
                    "iraqHeatProfile" to profile.iraqHeatProfile.ifBlank { "GENERAL_SUMMER" },
                    "selectedCity" to profile.selectedCity.ifBlank { "Baghdad" },
                    "languageCode" to profile.languageCode.ifBlank { "ar" },
                    "aiDialect" to profile.aiDialect.ifBlank { "MSA" },
                    "habitLearningEnabled" to profile.habitLearningEnabled,
                    "reminderIntervalMinutes" to profile.reminderIntervalMinutes.coerceIn(15L, 240L),
                    "remindersEnabled" to profile.remindersEnabled,
                    "onboardingCompleted" to profile.onboardingCompleted,
                    "streakDays" to profile.streakDays.coerceIn(0L, 10000L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val existingCreatedAt = existingSnap.getTimestamp("createdAt")
                val updatedPayload = mapOf(
                    "userId" to uid,
                    "name" to profile.name.ifBlank { "مستخدم هايدرا" }.take(80),
                    "age" to profile.age.coerceIn(5L, 120L),
                    "weightKg" to profile.weightKg.coerceIn(15L, 300L),
                    "heightCm" to profile.heightCm.coerceIn(50L, 250L),
                    "biologicalSex" to profile.biologicalSex.ifBlank { "MALE" },
                    "activityLevel" to profile.activityLevel.ifBlank { "MODERATE" },
                    "workEnvironment" to profile.workEnvironment.ifBlank { "INDOOR" },
                    "outdoorMinutes" to profile.outdoorMinutes.coerceIn(0L, 1440L),
                    "exerciseMinutes" to profile.exerciseMinutes.coerceIn(0L, 600L),
                    "sweatingLevel" to profile.sweatingLevel.ifBlank { "MODERATE" },
                    "wakeHour" to profile.wakeHour.coerceIn(0L, 23L),
                    "bedHour" to profile.bedHour.coerceIn(0L, 23L),
                    "medicalRestrictionType" to profile.medicalRestrictionType.ifBlank { "NONE" },
                    "prescribedLimitMl" to profile.prescribedLimitMl.coerceIn(0L, 10000L),
                    "iraqHeatProfile" to profile.iraqHeatProfile.ifBlank { "GENERAL_SUMMER" },
                    "selectedCity" to profile.selectedCity.ifBlank { "Baghdad" },
                    "languageCode" to profile.languageCode.ifBlank { "ar" },
                    "aiDialect" to profile.aiDialect.ifBlank { "MSA" },
                    "habitLearningEnabled" to profile.habitLearningEnabled,
                    "reminderIntervalMinutes" to profile.reminderIntervalMinutes.coerceIn(15L, 240L),
                    "remindersEnabled" to profile.remindersEnabled,
                    "onboardingCompleted" to profile.onboardingCompleted,
                    "streakDays" to profile.streakDays.coerceIn(0L, 10000L),
                    "createdAt" to (existingCreatedAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(updatedPayload).await()
            }
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeLogs(userId: String = requireUserId()): Flow<List<HydrationLog>> = flow {
        val path = "users/$userId/logs"
        emitAll(
            db.collection("users").document(userId).collection("logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(HydrationLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.loggedAtMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserLogs(): Result<List<HydrationLog>> {
        val uid = requireUserId()
        val path = "users/$uid/logs"
        return try {
            val snapshot = db.collection("users").document(uid).collection("logs")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.toObjects(HydrationLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.loggedAtMillis }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addLog(
        beverageType: String,
        amountMl: Int,
        effectiveHydrationMl: Int,
        dateKey: String,
        hourOfDay: Int,
        loggedAtMillis: Long,
        note: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("logs").document()
        val payload = mapOf(
            "userId" to uid,
            "beverageType" to beverageType.ifBlank { "WATER" }.take(30),
            "amountMl" to amountMl.toLong().coerceIn(10L, 5000L),
            "effectiveHydrationMl" to effectiveHydrationMl.toLong().coerceIn(5L, 5000L),
            "dateKey" to dateKey.take(12),
            "hourOfDay" to hourOfDay.toLong().coerceIn(0L, 23L),
            "loggedAtMillis" to loggedAtMillis.coerceAtLeast(1L),
            "note" to note.take(200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteLog(logId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("logs").document(logId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(): Result<Unit> {
        val uid = requireUserId()
        val logsPath = "users/$uid/logs"
        return try {
            val logsSnap = db.collection("users").document(uid).collection("logs")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in logsSnap.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, logsPath)
            Result.failure(e)
        }
    }
}
