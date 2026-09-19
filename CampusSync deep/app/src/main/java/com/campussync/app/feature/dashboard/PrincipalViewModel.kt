package com.campussync.app.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.feature.auth.CollegeClass
import com.campussync.app.feature.auth.PrincipalRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PrincipalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrincipalRepository(application)
    private val firestore = FirebaseFirestore.getInstance()

    private val _studentCount = MutableStateFlow(0)
    val studentCount: StateFlow<Int> = _studentCount.asStateFlow()

    private val _teacherCount = MutableStateFlow(0)
    val teacherCount: StateFlow<Int> = _teacherCount.asStateFlow()

    private val _averageAttendance = MutableStateFlow(0f)
    val averageAttendance: StateFlow<Float> = _averageAttendance.asStateFlow()

    private val _classWiseAttendance = MutableStateFlow<Map<String, Int>>(emptyMap())
    val classWiseAttendance: StateFlow<Map<String, Int>> = _classWiseAttendance.asStateFlow()

    private val attendanceRepository = com.campussync.app.feature.attendance.AttendanceRepository()

    private val _classCount = MutableStateFlow(0)
    val classCount: StateFlow<Int> = _classCount.asStateFlow()

    private val _recentActivities = MutableStateFlow<List<com.campussync.app.core.model.ActivityLog>>(emptyList())
    val recentActivities: StateFlow<List<com.campussync.app.core.model.ActivityLog>> = _recentActivities.asStateFlow()

    private val _upcomingEvents = MutableStateFlow<List<com.campussync.app.core.model.Event>>(emptyList())
    val upcomingEvents: StateFlow<List<com.campussync.app.core.model.Event>> = _upcomingEvents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _classes = MutableStateFlow<List<CollegeClass>>(emptyList())
    val classes: StateFlow<List<CollegeClass>> = _classes.asStateFlow()

    private val _classesLoading = MutableStateFlow(false)
    val classesLoading: StateFlow<Boolean> = _classesLoading.asStateFlow()

    fun loadDashboardStats(collegeId: String) {
        viewModelScope.launch {
            try {
                val students = firestore.collection("colleges").document(collegeId)
                    .collection("students").count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                _studentCount.value = students.count.toInt()

                val teachers = firestore.collection("colleges").document(collegeId)
                    .collection("teachers").count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                _teacherCount.value = teachers.count.toInt()

                val classes = firestore.collection("colleges").document(collegeId)
                    .collection("classes").count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                _classCount.value = classes.count.toInt()

                // Fetch recent activities
                val activitiesResult = repository.getRecentActivities(collegeId)
                activitiesResult.onSuccess { _recentActivities.value = it }

                // Fetch upcoming events
                val eventsResult = repository.getUpcomingEvents(collegeId)
                eventsResult.onSuccess { _upcomingEvents.value = it }

            } catch (e: Exception) { }
        }
    }

    fun loadClasses(collegeId: String) {
        if (_classes.value.isEmpty()) {
            _classesLoading.value = true
        }
        viewModelScope.launch {
            val result = repository.getClasses(collegeId)
            result.onSuccess { 
                _classes.value = it 
                loadAttendanceAnalytics(collegeId, it)
            }
            _classesLoading.value = false
        }
    }

    private fun loadAttendanceAnalytics(collegeId: String, classesList: List<CollegeClass>) {
        viewModelScope.launch {
            val heatmapData = mutableMapOf<String, Int>()
            var totalCollegePresent = 0
            var totalCollegeCount = 0

            for (collegeClass in classesList) {
                // Fetch last 7 days of attendance
                val recentRecords = attendanceRepository.getRecentAttendanceForClass(collegeId, collegeClass.classId, 7)
                if (recentRecords.isNotEmpty()) {
                    val classPresent = recentRecords.sumOf { it.present }
                    val classTotal = recentRecords.sumOf { it.total }
                    
                    if (classTotal > 0) {
                        val percentage = (classPresent * 100) / classTotal
                        heatmapData[collegeClass.className] = percentage
                        totalCollegePresent += classPresent
                        totalCollegeCount += classTotal
                    }
                } else {
                    heatmapData[collegeClass.className] = -1 // Indicates no data
                }
            }
            
            _classWiseAttendance.value = heatmapData
            _averageAttendance.value = if (totalCollegeCount > 0) (totalCollegePresent.toFloat() * 100 / totalCollegeCount) else 0f
        }
    }

    fun createClass(
        collegeId: String,
        course: String,
        semester: String,
        onComplete: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createClass(collegeId, course, semester)
            if (result.isSuccess) {
                repository.logActivity(collegeId, "Class Created", "New class $course - Sem $semester was created.", "class")
                loadDashboardStats(collegeId)
            }
            onComplete(result)
        }
    }

    fun createEvent(
        collegeId: String,
        title: String,
        description: String,
        eventDate: Long,
        createdBy: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.createEvent(collegeId, title, description, eventDate, createdBy)
            if (result.isSuccess) {
                loadDashboardStats(collegeId) // Refresh events and logs
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun deleteEvent(
        collegeId: String,
        eventId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.deleteEvent(collegeId, eventId)
            if (result.isSuccess) {
                loadDashboardStats(collegeId)
            }
            _isLoading.value = false
            onResult(result)
        }
    }

    fun createStudentInvite(
        email: String,
        name: String,
        rollNo: String,
        classId: String,
        collegeId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.createStudentInvite(email, name, rollNo, classId, collegeId)
            _isLoading.value = false
            onResult(result)
            if (result.isSuccess) {
                loadDashboardStats(collegeId)
            }
        }
    }

    fun createTeacherInvite(
        email: String,
        name: String,
        classId: String,
        collegeId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.createTeacherInvite(email, name, classId, collegeId)
            _isLoading.value = false
            onResult(result)
            if (result.isSuccess) {
                loadDashboardStats(collegeId)
            }
        }
    }

    // ══════════════════════════════════════════════════
    // ADMIN INVITE (New for 4-role system)
    // ══════════════════════════════════════════════════

    fun createAdminInvite(
        email: String,
        name: String,
        collegeId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.createAdminInvite(email, name, collegeId)
            _isLoading.value = false
            onResult(result)
            if (result.isSuccess) {
                loadDashboardStats(collegeId)
            }
        }
    }

    fun bulkUploadUsersFromExcel(
        context: android.content.Context,
        uri: android.net.Uri,
        collegeId: String,
        onComplete: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Could not open file")
                
                // Fetch classes to map human-readable names to Firestore IDs
                val classesSnapshot = firestore.collection("colleges").document(collegeId).collection("classes").get().await()
                val classMap = mutableMapOf<String, String>()
                for (doc in classesSnapshot.documents) {
                    val course = doc.getString("course") ?: ""
                    val semester = doc.getString("semester") ?: ""
                    val cName = doc.getString("className") ?: ""
                    
                    if (cName.isNotBlank()) classMap[cName.lowercase()] = doc.id
                    if (course.isNotBlank()) classMap[course.lowercase()] = doc.id
                    val combinedName = "$course $semester".trim().lowercase()
                    val combinedDashName = "$course - $semester".trim().lowercase()
                    if (combinedName.isNotBlank()) classMap[combinedName] = doc.id
                    if (combinedDashName.isNotBlank()) classMap[combinedDashName] = doc.id
                    classMap[doc.id.lowercase()] = doc.id
                }

                val rows = com.campussync.app.core.utils.XlsxParser.parse(inputStream)
                
                var processedCount = 0
                val batch = firestore.batch()
                var currentBatchSize = 0
                
                var isFirstLine = true
                for (row in rows) {
                    // Skip header row
                    if (isFirstLine) {
                        isFirstLine = false
                        val header = row.joinToString("").lowercase()
                        if (header.contains("role") || header.contains("email")) {
                            continue
                        }
                    }
                    isFirstLine = false
                    
                    if (row.isNotEmpty() && row.any { it.isNotBlank() }) {
                        if (row.size >= 3) {
                            val role = row[0].trim().lowercase()
                            val email = row[1].trim().lowercase()
                            val name = row[2].trim()
                            val rollNo = if (row.size > 3) row[3].trim() else ""
                            val rawClassId = if (row.size > 4) row[4].trim() else ""
                            val classId = classMap[rawClassId.lowercase()] ?: rawClassId
                            
                            if (email.isNotBlank() && name.isNotBlank() && role in listOf("student", "teacher", "admin")) {
                                val inviteData = hashMapOf(
                                    "email" to email,
                                    "name" to name,
                                    "role" to role,
                                    "collegeId" to collegeId,
                                    "status" to "pending"
                                )
                                if (role == "student") {
                                    inviteData["rollNo"] = rollNo
                                    inviteData["classId"] = classId
                                } else if (role == "teacher") {
                                    inviteData["classId"] = classId
                                }
                                
                                val inviteRef = firestore.collection("invites").document(email)
                                batch.set(inviteRef, inviteData)
                                currentBatchSize++
                                processedCount++
                                
                                if (currentBatchSize >= 490) {
                                    batch.commit().await()
                                    currentBatchSize = 0
                                }
                            }
                        }
                    }
                }
                
                if (currentBatchSize > 0) {
                    batch.commit().await()
                }
                
                onComplete(Result.success(processedCount))
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(Result.failure(e))
            }
        }
    }
}
