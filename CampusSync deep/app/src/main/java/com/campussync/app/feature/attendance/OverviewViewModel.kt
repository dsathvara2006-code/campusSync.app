package com.campussync.app.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class OverviewStats(
    val studentCount: Int = 0,
    val teacherCount: Int = 0,
    val classCount: Int = 0,
    val pendingUsersCount: Int = 0,
    val totalFeeDues: Int = 0,
    val pendingFeeCount: Int = 0,
    val paidFeeCount: Int = 0,
    val averageAttendancePercent: Int = 0,
    val noticeCount: Int = 0,
    val upcomingEventCount: Int = 0
)

data class OverviewClassRow(
    val classId: String = "",
    val name: String = "",
    val semester: String = "",
    val studentCount: Int = 0,
    val attendancePercent: Int = -1,
    val hasNoData: Boolean = false
)

data class OverviewNotice(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

data class OverviewEvent(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val eventDate: Long = 0L
)

data class OverviewActivity(
    val logId: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val timestamp: Long = 0L
)

class OverviewViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _stats = MutableStateFlow(OverviewStats())
    val stats: StateFlow<OverviewStats> = _stats.asStateFlow()

    private val _classes = MutableStateFlow<List<OverviewClassRow>>(emptyList())
    val classes: StateFlow<List<OverviewClassRow>> = _classes.asStateFlow()

    private val _notices = MutableStateFlow<List<OverviewNotice>>(emptyList())
    val notices: StateFlow<List<OverviewNotice>> = _notices.asStateFlow()

    private val _events = MutableStateFlow<List<OverviewEvent>>(emptyList())
    val events: StateFlow<List<OverviewEvent>> = _events.asStateFlow()

    private val _activities = MutableStateFlow<List<OverviewActivity>>(emptyList())
    val activities: StateFlow<List<OverviewActivity>> = _activities.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(collegeId: String) {
        if (collegeId.isBlank()) return
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val collegeRef = db.collection("colleges").document(collegeId)

                // Parallel fetch all top-level collections
                val studentsDef   = async { runCatching { collegeRef.collection("students").get().await() } }
                val teachersDef   = async { runCatching { collegeRef.collection("teachers").get().await() } }
                val classesDef    = async { runCatching { collegeRef.collection("classes").get().await() } }
                val pendingDef    = async { runCatching { db.collection("users").whereEqualTo("collegeId", collegeId).whereEqualTo("status", "pending").get().await() } }
                val feeDuesDef    = async { runCatching { collegeRef.collection("fee_dues").get().await() } }
                val noticesDef    = async { runCatching { collegeRef.collection("notices").orderBy("timestamp", Query.Direction.DESCENDING).limit(5).get().await() } }
                val eventsDef     = async { runCatching { collegeRef.collection("events").whereGreaterThanOrEqualTo("eventDate", System.currentTimeMillis() - 86400000L).orderBy("eventDate", Query.Direction.ASCENDING).limit(5).get().await() } }
                val activitiesDef = async { runCatching { collegeRef.collection("activity_logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(6).get().await() } }

                val studentsSnap   = studentsDef.await().getOrNull()
                val teachersSnap   = teachersDef.await().getOrNull()
                val classesSnap    = classesDef.await().getOrNull()
                val pendingSnap    = pendingDef.await().getOrNull()
                val feeDuesSnap    = feeDuesDef.await().getOrNull()
                val noticesSnap    = noticesDef.await().getOrNull()
                val eventsSnap     = eventsDef.await().getOrNull()
                val activitiesSnap = activitiesDef.await().getOrNull()

                val studentCount = studentsSnap?.size() ?: 0
                val teacherCount = teachersSnap?.size() ?: 0
                val classCount   = classesSnap?.size() ?: 0
                val pendingCount = pendingSnap?.size() ?: 0
                val totalFees    = feeDuesSnap?.size() ?: 0
                val pendingFees  = feeDuesSnap?.documents?.count { it.getString("status") == "pending" } ?: 0
                val paidFees     = feeDuesSnap?.documents?.count { it.getString("status") == "paid" || it.getString("status") == "approved" } ?: 0
                val noticeCount  = noticesSnap?.size() ?: 0
                val eventCount   = eventsSnap?.size() ?: 0

                // Per-class: student count + last attendance %
                val classRows = mutableListOf<OverviewClassRow>()
                classesSnap?.documents?.forEach { classDoc ->
                    val cid      = classDoc.id
                    val course   = classDoc.getString("course") ?: cid
                    val semester = classDoc.getString("semester") ?: ""
                    val classStudentCount = studentsSnap?.documents?.count { it.getString("classId") == cid } ?: 0

                    val attSnap = runCatching {
                        collegeRef.collection("classes").document(cid)
                            .collection("attendance")
                            .orderBy("date", Query.Direction.DESCENDING)
                            .limit(1)
                            .get().await()
                    }.getOrNull()

                    val lastAtt = attSnap?.documents?.firstOrNull()
                    val attPercent = if (lastAtt != null) {
                        val total   = lastAtt.getLong("total")?.toInt() ?: 0
                        val present = lastAtt.getLong("present")?.toInt() ?: 0
                        if (total > 0) (present * 100) / total else -1
                    } else -1

                    classRows.add(
                        OverviewClassRow(
                            classId = cid,
                            name = course,
                            semester = semester,
                            studentCount = classStudentCount,
                            attendancePercent = attPercent,
                            hasNoData = attPercent == -1
                        )
                    )
                }

                val validClasses = classRows.filter { !it.hasNoData }
                val avgAtt = if (validClasses.isNotEmpty()) validClasses.sumOf { it.attendancePercent } / validClasses.size else 0

                _stats.value = OverviewStats(
                    studentCount = studentCount,
                    teacherCount = teacherCount,
                    classCount = classCount,
                    pendingUsersCount = pendingCount,
                    totalFeeDues = totalFees,
                    pendingFeeCount = pendingFees,
                    paidFeeCount = paidFees,
                    averageAttendancePercent = avgAtt,
                    noticeCount = noticeCount,
                    upcomingEventCount = eventCount
                )

                _classes.value = classRows.sortedBy { it.name }

                _notices.value = noticesSnap?.documents?.map { doc ->
                    OverviewNotice(
                        id = doc.id,
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()

                _events.value = eventsSnap?.documents?.map { doc ->
                    OverviewEvent(
                        eventId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        eventDate = doc.getLong("eventDate") ?: 0L
                    )
                } ?: emptyList()

                _activities.value = activitiesSnap?.documents?.map { doc ->
                    OverviewActivity(
                        logId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        type = doc.getString("type") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()

            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error loading overview"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
