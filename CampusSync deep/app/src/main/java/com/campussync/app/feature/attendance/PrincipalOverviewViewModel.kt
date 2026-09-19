package com.campussync.app.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ClassOverview(
    val classId: String,
    val name: String,
    val branch: String,
    val semester: String
)

data class ClassRecentAttendance(
    val date: String,
    val subjectName: String,
    val total: Int,
    val present: Int,
    val absent: Int
)

class PrincipalOverviewViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val repository = AttendanceRepository(db)

    private val _classes = MutableStateFlow<List<ClassOverview>>(emptyList())
    val classes: StateFlow<List<ClassOverview>> = _classes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadClasses(collegeId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Auto-migrate legacy classes
                val classesSnapshot = db.collection("colleges").document(collegeId).collection("classes").get().await()
                var needsReload = false
                for (doc in classesSnapshot.documents) {
                    if (!doc.id.contains("-") && doc.id.length >= 20) {
                        val course = doc.getString("course") ?: doc.getString("name") ?: ""
                        val sem = doc.getString("semester") ?: ""
                        if (course.isNotBlank() && sem.isNotBlank()) {
                            val newId = "${course}-${sem}"
                            // Create new class
                            db.collection("colleges").document(collegeId).collection("classes").document(newId).set(doc.data!!).await()
                            // Delete old class
                            db.collection("colleges").document(collegeId).collection("classes").document(doc.id).delete().await()
                            
                            // Migrate students
                            val students = db.collection("colleges").document(collegeId).collection("students").whereEqualTo("classId", doc.id).get().await()
                            for (sDoc in students.documents) {
                                db.collection("colleges").document(collegeId).collection("students").document(sDoc.id).update("classId", newId).await()
                            }
                            // Migrate teachers (subject_assignments)
                            val subjects = db.collection("colleges").document(collegeId).collection("subject_assignments").whereEqualTo("classId", doc.id).get().await()
                            for (sDoc in subjects.documents) {
                                db.collection("colleges").document(collegeId).collection("subject_assignments").document(sDoc.id).update("classId", newId).await()
                            }
                            // Migrate root users
                            val rootUsers = db.collection("users").whereEqualTo("classId", doc.id).get().await()
                            for (uDoc in rootUsers.documents) {
                                db.collection("users").document(uDoc.id).update("classId", newId).await()
                            }
                            needsReload = true
                        }
                    }
                }
                
                val finalSnapshot = if (needsReload) db.collection("colleges").document(collegeId).collection("classes").get().await() else classesSnapshot

                val classList = finalSnapshot.documents.mapNotNull { doc ->
                    ClassOverview(
                        classId = doc.id,
                        name = doc.getString("course") ?: doc.getString("name") ?: doc.id,
                        branch = doc.getString("branch") ?: "",
                        semester = doc.getString("semester") ?: ""
                    )
                }
                _classes.value = classList.sortedBy { it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _selectedClassAttendance = MutableStateFlow<List<ClassRecentAttendance>>(emptyList())
    val selectedClassAttendance: StateFlow<List<ClassRecentAttendance>> = _selectedClassAttendance.asStateFlow()
    
    private val _selectedClassStudents = MutableStateFlow<List<com.campussync.app.core.model.StudentRow>>(emptyList())
    val selectedClassStudents: StateFlow<List<com.campussync.app.core.model.StudentRow>> = _selectedClassStudents.asStateFlow()

    fun loadClassDetails(collegeId: String, classId: String) {
        viewModelScope.launch {
            try {
                // Fetch attendance
                val snapshot = db.collection("colleges").document(collegeId)
                    .collection("classes").document(classId)
                    .collection("attendance")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()
                
                val recent = snapshot.documents.mapNotNull { doc ->
                    ClassRecentAttendance(
                        date = doc.getString("date") ?: "",
                        subjectName = doc.getString("subjectName") ?: "General",
                        total = doc.getLong("total")?.toInt() ?: 0,
                        present = doc.getLong("present")?.toInt() ?: 0,
                        absent = doc.getLong("absent")?.toInt() ?: 0
                    )
                }
                _selectedClassAttendance.value = recent
                
                // Fetch students
                val roster = repository.fetchClassRoster(collegeId, classId)
                _selectedClassStudents.value = roster
            } catch (e: Exception) {
                _selectedClassAttendance.value = emptyList()
                _selectedClassStudents.value = emptyList()
            }
        }
    }
}
