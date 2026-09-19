package com.campussync.app.feature.attendance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.Attendance
import com.campussync.app.core.model.StudentRow
import com.campussync.app.core.model.SubjectAssignment
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class AttendanceViewModel(
    private val repository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    // Base roster before date-specific statuses
    private var baseRoster: List<StudentRow> = emptyList()

    private val _students = MutableStateFlow<List<StudentRow>>(emptyList())
    val students: StateFlow<List<StudentRow>> = _students.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isLoadingDate = MutableStateFlow(false)
    val isLoadingDate: StateFlow<Boolean> = _isLoadingDate.asStateFlow()

    private val _markedDates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val markedDates: StateFlow<Map<String, Boolean>> = _markedDates.asStateFlow()
    
    // [NEW] State for teacher's assigned subjects
    private val _teacherSubjects = MutableStateFlow<List<SubjectAssignment>>(emptyList())
    val teacherSubjects: StateFlow<List<SubjectAssignment>> = _teacherSubjects.asStateFlow()

    // [NEW] Fetch subjects assigned to teacher
    // [NEW] Fetch ALL subjects assigned to teacher
    fun loadTeacherSubjects(collegeId: String, teacherId: String) {
        viewModelScope.launch {
            val subjects = repository.fetchAssignedSubjects(collegeId, teacherId)
            _teacherSubjects.value = subjects
        }
    }

    /** Load existing attendance for specific date from Firestore, and fetch latest signed up student roster. */
    fun loadAttendanceForDate(collegeId: String, classId: String, date: String, subjectName: String = "") {
        if (_students.value.isEmpty()) {
            _isLoadingDate.value = true
        }
        viewModelScope.launch {
            // Dynamically fetch latest signed-up student roster from Firestore
            val freshRoster = repository.fetchClassRoster(collegeId, classId)
            baseRoster = freshRoster

            val existing = repository.getAttendanceForDate(collegeId, classId, date, subjectName)
            if (existing != null && existing.records.isNotEmpty()) {
                _students.value = baseRoster.map { student ->
                    val status = existing.records[student.studentId] ?: ""
                    student.copy(status = status)
                }
            } else {
                _students.value = baseRoster.map { it.copy(status = "") }
            }
            _isLoadingDate.value = false
        }
    }

    /** Fetch dates in yearMonth ("YYYY-MM") that have saved attendance in Firestore. */
    fun loadMarkedDatesForMonth(collegeId: String, classId: String, yearMonth: String) {
        viewModelScope.launch {
            val map = repository.getMarkedDatesForMonth(collegeId, classId, yearMonth)
            _markedDates.value = map
        }
    }

    fun markStatus(studentId: String, status: String) {
        _students.value = _students.value.map {
            if (it.studentId == studentId) {
                val newStatus = if (it.status == status) "" else status
                it.copy(status = newStatus)
            } else it
        }
    }

    fun markAllPresent() {
        _students.value = _students.value.map { it.copy(status = "present") }
    }

    fun markAllAbsent() {
        _students.value = _students.value.map { it.copy(status = "absent") }
    }

    // [MODIFIED] Added subjectName and teacherId parameters with defaults for backwards compatibility
    fun submitAttendance(
        collegeId: String, 
        classId: String, 
        date: String, 
        subjectName: String = "",
        teacherId: String = "",
        onDone: (Boolean, String?) -> Unit
    ) {
        val records = _students.value.associate { it.studentId to (it.status.ifBlank { "absent" }) }
        _isSubmitting.value = true
        viewModelScope.launch {
            val result = repository.submitAttendance(
                collegeId = collegeId, 
                classId = classId, 
                date = date, 
                subjectName = subjectName, 
                teacherId = teacherId, 
                records = records
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    // Update marked dates cache immediately
                    _markedDates.value += (date to true)
                    onDone(true, null)
                },
                onFailure = { onDone(false, it.message) }
            )
        }
    }

    // --- Student side ---
    private val _liveAttendance = MutableStateFlow<Attendance?>(null)
    val liveAttendance: StateFlow<Attendance?> = _liveAttendance.asStateFlow()

    private val _overallPercentage = MutableStateFlow<Int?>(null)
    val overallPercentage: StateFlow<Int?> = _overallPercentage.asStateFlow()

    private val _presentDays = MutableStateFlow(0)
    val presentDays: StateFlow<Int> = _presentDays.asStateFlow()

    private val _totalDays = MutableStateFlow(0)
    val totalDays: StateFlow<Int> = _totalDays.asStateFlow()
    
    // [NEW] Subject-wise percentage state
    private val _subjectWisePercentage = MutableStateFlow<Map<String, Int>>(emptyMap())
    val subjectWisePercentage: StateFlow<Map<String, Int>> = _subjectWisePercentage.asStateFlow()

    private var percentageListenerRegistration: ListenerRegistration? = null
    private var subjectPercentageListenerRegistration: ListenerRegistration? = null

    // [MODIFIED] Added subjectName parameter with default
    fun listenToClassAttendance(collegeId: String, classId: String, date: String, subjectName: String = "") {
        viewModelScope.launch {
            repository.listenAttendance(collegeId, classId, date, subjectName).collect { attendance ->
                _liveAttendance.value = attendance
            }
        }
    }

    fun loadOverallPercentage(
        collegeId: String,
        classId: String,
        studentId: String,
        holidayDates: Set<String> = emptySet()
    ) {
        percentageListenerRegistration?.remove()
        percentageListenerRegistration = repository.listenToOverallPercentage(
            collegeId = collegeId,
            classId = classId,
            studentId = studentId,
            holidayDates = holidayDates,
            onUpdate = { newPercentage, newPresent, newTotal ->
                _overallPercentage.value = newPercentage
                _presentDays.value = newPresent
                _totalDays.value = newTotal
            }
        )
    }

    // [NEW] Load subject-wise percentage for detailed student view
    fun loadSubjectWisePercentage(
        collegeId: String,
        classId: String,
        studentId: String,
        holidayDates: Set<String> = emptySet()
    ) {
        subjectPercentageListenerRegistration?.remove()
        subjectPercentageListenerRegistration = repository.listenToSubjectWisePercentage(
            collegeId = collegeId,
            classId = classId,
            studentId = studentId,
            holidayDates = holidayDates,
            onUpdate = { subjectPercentages ->
                _subjectWisePercentage.value = subjectPercentages
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        percentageListenerRegistration?.remove()
        subjectPercentageListenerRegistration?.remove()
    }
}
