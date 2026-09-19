package com.campussync.app.core.model

import com.google.firebase.firestore.PropertyName

data class User(
    val userId: String = "",
    val collegeId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student", // "principal", "admin", "teacher", "student"
    val rollNo: String = "",
    val classId: String = "",
    val status: String = "approved", // "pending", "approved"
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true, // Soft Delete Flag
    val createdAt: Long = System.currentTimeMillis()
)

data class Assignment(
    val id: String = "",
    val classId: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "", // YYYY-MM-DD format
    val createdAt: Long = System.currentTimeMillis(),
    val completedBy: List<String> = emptyList()
)

/**
 * One document per class per lecture.
 * Firestore path: colleges/{collegeId}/classes/{classId}/attendance/{date}_{subjectName}
 */
data class Attendance(
    val classId: String = "",
    val date: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val total: Int = 0,
    val present: Int = 0,
    val absent: Int = 0,
    val records: Map<String, String> = emptyMap() // studentId -> "present" / "absent"
)

data class SubjectAssignment(
    val id: String = "",
    val collegeId: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subjectName: String = "",
    val semester: String = ""
)

data class StudentRow(
    val studentId: String = "",
    val name: String = "",
    val rollNo: String = "",
    val status: String = "" // "", "present", "absent"
)

data class Notice(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Holiday(
    val id: String = "",
    val date: String = "", // YYYY-MM-DD format
    val title: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TimetableEntry(
    val id: String = "",
    val classId: String = "",
    val dayOfWeek: String = "", // e.g., "Monday", "Tuesday"
    val subject: String = "",
    val time: String = "",
    val teacherName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Resource(
    val resourceId: String = "",
    val uploaderId: String = "",
    val uploaderName: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val fileType: String = "",
    val branch: String = "",
    val semester: String = "",
    val subject: String = "",
    val tags: List<String> = emptyList(),
    val likesCount: Int = 0,
    val downloadsCount: Int = 0,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val status: String = "approved"
)

data class Event(
    val eventId: String = "",
    val collegeId: String = "",
    val title: String = "",
    val description: String = "",
    val eventDate: Long = 0L,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ActivityLog(
    val logId: String = "",
    val collegeId: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "general", // "fee", "user", "class", "event"
    val timestamp: Long = System.currentTimeMillis()
)
