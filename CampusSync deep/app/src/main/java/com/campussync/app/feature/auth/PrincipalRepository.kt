package com.campussync.app.feature.auth

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class CollegeClass(
    val classId: String = "",
    val course: String = "",
    val semester: String = "",
    val className: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class PrincipalRepository(private val context: Context) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ══════════════════════════════════════════════════
    // SECURITY: Input Sanitization
    // ══════════════════════════════════════════════════

    private fun sanitize(input: String, maxLength: Int = 100): String {
        return input.trim()
            .take(maxLength)
            .replace(Regex("[<>\"';&|`\\\\]"), "") // Strip dangerous chars
    }

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.lowercase().trim()
        return trimmed.isNotBlank() 
            && trimmed.contains("@") 
            && trimmed.contains(".")
            && trimmed.length <= 254
            && !trimmed.contains("..")
    }

    // ══════════════════════════════════════════════════
    // CLASS MANAGEMENT
    // ══════════════════════════════════════════════════

    suspend fun createClass(collegeId: String, course: String, semester: String): Result<String> {
        return try {
            val cleanCollegeId = collegeId.trim()
            val cleanCourse = sanitize(course, maxLength = 50)
            val cleanSemester = sanitize(semester, maxLength = 50)
            
            if (cleanCourse.isBlank() || cleanSemester.isBlank()) {
                return Result.failure(Exception("Course and Semester cannot be empty"))
            }
            
            val classId = "$cleanCourse-$cleanSemester"
            
            val classData = hashMapOf(
                "course" to cleanCourse,
                "semester" to cleanSemester,
                "createdAt" to System.currentTimeMillis()
            )
            
            firestore.collection("colleges")
                .document(cleanCollegeId)
                .collection("classes")
                .document(classId)
                .set(classData)
                .await()
                
            Result.success(classId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClasses(collegeId: String): Result<List<CollegeClass>> {
        return try {
            val snapshot = firestore.collection("colleges")
                .document(collegeId.trim())
                .collection("classes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val classes = snapshot.documents.map { doc ->
                CollegeClass(
                    classId = doc.id,
                    course = doc.getString("course") ?: "",
                    semester = doc.getString("semester") ?: "",
                    className = doc.getString("className") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.success(classes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════
    // INVITE MANAGEMENT
    // ══════════════════════════════════════════════════

    suspend fun createStudentInvite(
        email: String,
        name: String,
        rollNo: String,
        classId: String,
        collegeId: String
    ): Result<Unit> {
        return try {
            val emailLower = email.lowercase().trim()
            if (!isValidEmail(emailLower)) {
                return Result.failure(Exception("Invalid email address"))
            }
            val inviteData = hashMapOf(
                "email" to emailLower,
                "role" to "student",
                "collegeId" to collegeId,
                "classId" to classId,
                "name" to sanitize(name, maxLength = 80),
                "rollNo" to sanitize(rollNo, maxLength = 20),
                "status" to "pending"
            )
            firestore.collection("invites").document(emailLower).set(inviteData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a teacher invite.
     * Enforced classId assignment so it gets copied automatically during onboarding.
     */
    suspend fun createTeacherInvite(
        email: String,
        name: String,
        classId: String,
        collegeId: String
    ): Result<Unit> {
        return try {
            val emailLower = email.lowercase().trim()
            if (!isValidEmail(emailLower)) {
                return Result.failure(Exception("Invalid email address"))
            }
            val inviteData = hashMapOf(
                "email" to emailLower,
                "role" to "teacher",
                "collegeId" to collegeId,
                "classId" to classId,
                "name" to sanitize(name, maxLength = 80),
                "status" to "pending"
            )
            firestore.collection("invites").document(emailLower).set(inviteData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ══════════════════════════════════════════════════
    // ADMIN INVITE (New for 4-role system)
    // ══════════════════════════════════════════════════

    suspend fun createAdminInvite(
        email: String,
        name: String,
        collegeId: String
    ): Result<Unit> {
        return try {
            val emailLower = email.lowercase().trim()
            if (!isValidEmail(emailLower)) {
                return Result.failure(Exception("Invalid email address"))
            }
            val inviteData = hashMapOf(
                "email" to emailLower,
                "role" to "admin",
                "collegeId" to collegeId,
                "name" to sanitize(name, maxLength = 80),
                "status" to "pending"
            )
            firestore.collection("invites").document(emailLower).set(inviteData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ══════════════════════════════════════════════════
    // EVENTS & ACTIVITY LOGS
    // ══════════════════════════════════════════════════

    fun logActivity(collegeId: String, title: String, description: String, type: String = "general") {
        try {
            val logRef = firestore.collection("colleges").document(collegeId).collection("activity_logs").document()
            val logData = hashMapOf(
                "logId" to logRef.id,
                "collegeId" to collegeId,
                "title" to sanitize(title),
                "description" to sanitize(description, maxLength = 250),
                "type" to type,
                "timestamp" to System.currentTimeMillis()
            )
            logRef.set(logData).addOnFailureListener { e -> e.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRecentActivities(collegeId: String): Result<List<com.campussync.app.core.model.ActivityLog>> {
        return try {
            val snapshot = firestore.collection("colleges").document(collegeId)
                .collection("activity_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get().await()
            val logs = snapshot.documents.mapNotNull { it.toObject(com.campussync.app.core.model.ActivityLog::class.java) }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEvent(collegeId: String, title: String, description: String, eventDate: Long, createdBy: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("colleges").document(collegeId).collection("events").document()
            val event = com.campussync.app.core.model.Event(
                eventId = eventRef.id,
                collegeId = collegeId,
                title = sanitize(title),
                description = sanitize(description, maxLength = 500),
                eventDate = eventDate,
                createdBy = createdBy
            )
            eventRef.set(event).await()
            logActivity(collegeId, "New Event Created", "Event '$title' has been scheduled.", "event")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpcomingEvents(collegeId: String): Result<List<com.campussync.app.core.model.Event>> {
        return try {
            val snapshot = firestore.collection("colleges").document(collegeId)
                .collection("events")
                .whereGreaterThanOrEqualTo("eventDate", System.currentTimeMillis() - 86400000) // from today
                .orderBy("eventDate", Query.Direction.ASCENDING)
                .limit(10)
                .get().await()
            val events = snapshot.documents.mapNotNull { it.toObject(com.campussync.app.core.model.Event::class.java) }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(collegeId: String, eventId: String): Result<Unit> {
        return try {
            firestore.collection("colleges").document(collegeId)
                .collection("events").document(eventId).delete().await()
            logActivity(collegeId, "Event Cancelled", "An event has been removed from the institutional schedule.", "event")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

