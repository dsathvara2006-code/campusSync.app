package com.campussync.app.feature.attendance

import com.campussync.app.core.model.StudentRow
import com.campussync.app.core.model.Attendance
import com.campussync.app.core.model.SubjectAssignment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class AttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // [MODIFIED] Added subjectName and teacherId parameters for Per-Lecture architecture
    suspend fun submitAttendance(
        collegeId: String, 
        classId: String, 
        date: String, 
        subjectName: String = "", 
        teacherId: String = "", 
        records: Map<String, String>
    ): Result<Unit> {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) {
            return Result.failure(Exception("Cannot save attendance: Missing college or class assignment."))
        }

        return try {
            val present = records.values.count { it == "present" }
            val attendance = Attendance(
                classId = cleanClassId,
                date = date,
                subjectName = subjectName,
                teacherId = teacherId,
                total = records.size,
                present = present,
                absent = records.size - present,
                records = records
            )
            
            // [MODIFIED] Document ID format changed to include subjectName for per-lecture granularity
            val docId = if (subjectName.isNotBlank()) "${date.trim()}_${subjectName}" else "${date.trim()}_"
            
            val docRef = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance").document(docId)
                
            docRef.set(attendance).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // [NEW] Fetch subjects assigned to a specific teacher
    suspend fun fetchAssignedSubjects(collegeId: String, teacherId: String): List<SubjectAssignment> {
        val cleanCollegeId = collegeId.trim()
        val cleanTeacherId = teacherId.trim()

        if (cleanCollegeId.isBlank() || cleanTeacherId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("colleges").document(cleanCollegeId)
                .collection("subject_assignments")
                .whereEqualTo("teacherId", cleanTeacherId)
                .get()
                .await()
                
            snapshot.documents.mapNotNull { it.toObject(SubjectAssignment::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // [MODIFIED] Added subjectName parameter
    fun listenAttendance(collegeId: String, classId: String, date: String, subjectName: String = ""): Flow<Attendance?> {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) {
            return emptyFlow()
        }

        return callbackFlow {
            val docId = if (subjectName.isNotBlank()) "${date.trim()}_${subjectName}" else "${date.trim()}_"
            val registration = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance").document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(Attendance::class.java))
                }
            awaitClose { registration.remove() }
        }
    }

    // [MODIFIED] Added subjectName parameter
    suspend fun getAttendanceForDate(collegeId: String, classId: String, date: String, subjectName: String = ""): Attendance? {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) return null

        return try {
            val docId = if (subjectName.isNotBlank()) "${date.trim()}_${subjectName}" else "${date.trim()}_"
            val snapshot = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance").document(docId)
                .get()
                .await()
            snapshot.toObject(Attendance::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // [NEW] Fetch all attendance records (lectures) for a specific class on a specific date
    suspend fun getAllAttendancesForDate(collegeId: String, classId: String, date: String): List<Attendance> {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance")
                .whereEqualTo("date", date.trim())
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMarkedDatesForMonth(collegeId: String, classId: String, yearMonth: String): Map<String, Boolean> {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) return emptyMap()

        return try {
            val snapshot = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance")
                .get()
                .await()
            val resultMap = mutableMapOf<String, Boolean>()
            for (doc in snapshot.documents) {
                val date = doc.getString("date") ?: continue
                if (date.startsWith(yearMonth)) {
                    resultMap[date] = true
                }
            }
            resultMap
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun fetchClassRoster(collegeId: String, classId: String): List<StudentRow> {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("colleges").document(cleanCollegeId).collection("students")
                .whereEqualTo("classId", cleanClassId)
                .get()
                .await()
                
            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val rollNo = doc.getString("rollNo") ?: "Roll-${doc.id.take(4)}"
                StudentRow(
                    studentId = doc.id,
                    name = name,
                    rollNo = if (rollNo.isBlank()) "Roll-${doc.id.take(4)}" else rollNo,
                    status = ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOverallPercentage(
        collegeId: String,
        classId: String,
        studentId: String,
        holidayDates: Set<String> = emptySet()
    ): Int {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) return 0

        return try {
            val snapshot = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance")
                .get()
                .await()
            var totalDays = 0
            var presentDays = 0
            for (doc in snapshot.documents) {
                val date = doc.getString("date") ?: doc.id
                val isSunday = try {
                    java.time.LocalDate.parse(date).dayOfWeek == java.time.DayOfWeek.SUNDAY
                } catch (e: Exception) { false }
                val isHoliday = holidayDates.contains(date)
                if (isSunday || isHoliday) continue

                val status = doc.getString("records.$studentId") ?: continue
                totalDays++
                if (status == "present") presentDays++
            }
            if (totalDays == 0) 0 else (presentDays * 100) / totalDays
        } catch (e: Exception) {
            0
        }
    }

    fun listenToOverallPercentage(
        collegeId: String,
        classId: String,
        studentId: String,
        holidayDates: Set<String> = emptySet(),
        onUpdate: (Int, Int, Int) -> Unit
    ): ListenerRegistration? {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) {
            onUpdate(0, 0, 0)
            return null
        }

        return try {
            firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    var totalDays = 0
                    var presentDays = 0
                    for (doc in snapshot.documents) {
                        val date = doc.getString("date") ?: doc.id
                        val isSunday = try {
                            java.time.LocalDate.parse(date).dayOfWeek == java.time.DayOfWeek.SUNDAY
                        } catch (e: Exception) { false }
                        val isHoliday = holidayDates.contains(date)
                        if (isSunday || isHoliday) continue

                        val status = doc.getString("records.$studentId") ?: continue
                        totalDays++
                        if (status == "present") presentDays++
                    }
                    val percentage = if (totalDays == 0) 0 else (presentDays * 100) / totalDays
                    onUpdate(percentage, presentDays, totalDays)
                }
        } catch (e: Exception) {
            null
        }
    }

    // [NEW] Real-time listener for subject-wise attendance breakdown
    fun listenToSubjectWisePercentage(
        collegeId: String,
        classId: String,
        studentId: String,
        holidayDates: Set<String> = emptySet(),
        onUpdate: (Map<String, Int>) -> Unit
    ): ListenerRegistration? {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) {
            onUpdate(emptyMap())
            return null
        }

        return try {
            firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    val subjectTotals = mutableMapOf<String, Int>()
                    val subjectPresents = mutableMapOf<String, Int>()
                    
                    for (doc in snapshot.documents) {
                        val date = doc.getString("date") ?: doc.id
                        val subjectName = doc.getString("subjectName") ?: "General"
                        
                        val isSunday = try {
                            java.time.LocalDate.parse(date).dayOfWeek == java.time.DayOfWeek.SUNDAY
                        } catch (e: Exception) { false }
                        val isHoliday = holidayDates.contains(date)
                        if (isSunday || isHoliday) continue

                        val status = doc.getString("records.$studentId") ?: continue
                        
                        subjectTotals[subjectName] = subjectTotals.getOrDefault(subjectName, 0) + 1
                        if (status == "present") {
                            subjectPresents[subjectName] = subjectPresents.getOrDefault(subjectName, 0) + 1
                        }
                    }
                    
                    val subjectPercentages = mutableMapOf<String, Int>()
                    for ((subject, total) in subjectTotals) {
                        val present = subjectPresents.getOrDefault(subject, 0)
                        subjectPercentages[subject] = if (total == 0) 0 else (present * 100) / total
                    }
                    
                    onUpdate(subjectPercentages)
                }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecentAttendanceForClass(collegeId: String, classId: String, limit: Long = 7): List<Attendance> {
        val cleanCollegeId = collegeId.trim()
        val cleanClassId = classId.trim()

        if (cleanCollegeId.isBlank() || cleanClassId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("colleges").document(cleanCollegeId)
                .collection("classes").document(cleanClassId)
                .collection("attendance")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
