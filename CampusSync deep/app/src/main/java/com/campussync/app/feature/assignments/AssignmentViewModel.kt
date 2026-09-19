package com.campussync.app.feature.assignments

import com.campussync.app.core.model.StudentRow
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.Assignment
import com.campussync.app.feature.assignments.AssignmentRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AssignmentViewModel(
    application: Application,
) : AndroidViewModel(application) {
    
    private val repository = AssignmentRepository()

    private val _assignments = MutableStateFlow<List<Assignment>>(emptyList())
    val assignments: StateFlow<List<Assignment>> = _assignments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun loadAssignments(collegeId: String, classId: String) {
        if (_assignments.value.isEmpty()) {
            _isLoading.value = true
        }
        listenerRegistration?.remove()
        
        listenerRegistration = repository.listenToAssignments(
            collegeId = collegeId,
            classId = classId,
            onUpdate = { newAssignments ->
                _assignments.value = newAssignments
                _isLoading.value = false
            }
        )
    }

    fun addAssignment(collegeId: String, assignment: Assignment, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.addAssignment(collegeId, assignment)
            onDone(result.isSuccess)
        }
    }

    fun deleteAssignment(collegeId: String, classId: String, assignmentId: String) {
        viewModelScope.launch {
            repository.deleteAssignment(collegeId, classId, assignmentId)
        }
    }

    private val _classRoster = MutableStateFlow<List<StudentRow>>(emptyList())
    val classRoster: StateFlow<List<StudentRow>> = _classRoster.asStateFlow()

    fun loadClassRoster(collegeId: String) {
        viewModelScope.launch {
            try {
                // Fetch directly from users collection for simplicity
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("colleges").document(collegeId).collection("students")
                    .get()
                    .await()
                
                val students = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "Unknown"
                    val roll = doc.getString("rollNo") ?: ""
                    StudentRow(
                        studentId = doc.id,
                        name = name,
                        rollNo = roll
                    )
                }
                _classRoster.value = students.sortedBy { it.rollNo }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun toggleAssignmentStatus(collegeId: String, classId: String, assignmentId: String, studentId: String, isDone: Boolean) {
        viewModelScope.launch {
            repository.markAssignmentDone(collegeId, classId, assignmentId, studentId, isDone)
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

