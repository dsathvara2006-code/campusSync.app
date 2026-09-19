package com.campussync.app.feature.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.FeeDue
import com.campussync.app.core.model.FeeType
import com.campussync.app.core.model.User
import com.campussync.app.feature.auth.CollegeClass
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AssignFeeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _feeTypes = MutableStateFlow<List<FeeType>>(emptyList())
    val feeTypes: StateFlow<List<FeeType>> = _feeTypes.asStateFlow()

    private val _students = MutableStateFlow<List<User>>(emptyList())
    val students: StateFlow<List<User>> = _students.asStateFlow()

    private val _classes = MutableStateFlow<List<CollegeClass>>(emptyList())
    val classes: StateFlow<List<CollegeClass>> = _classes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadInitialData(collegeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load Fee Types
                val typesSnapshot = db.collection("colleges").document(collegeId)
                    .collection("fee_types").get().await()
                _feeTypes.value = typesSnapshot.documents.mapNotNull { doc -> doc.toObject(FeeType::class.java)?.copy(id = doc.id) }

                // Load Students from the college's secure subcollection
                val studentsSnapshot = db.collection("colleges").document(collegeId)
                    .collection("students")
                    .get().await()
                _students.value = studentsSnapshot.documents.mapNotNull { doc -> doc.toObject(User::class.java)?.copy(userId = doc.id) }
                
                // Load Classes
                val classesSnapshot = db.collection("colleges").document(collegeId)
                    .collection("classes")
                    .get().await()
                _classes.value = classesSnapshot.documents.mapNotNull { doc -> doc.toObject(CollegeClass::class.java)?.copy(classId = doc.id) }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignFeeToStudent(
        collegeId: String, 
        student: User, 
        feeType: FeeType, 
        dueDate: Long, 
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dueId = UUID.randomUUID().toString()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                val feeDue = FeeDue(
                    id = dueId,
                    collegeId = collegeId,
                    studentId = student.userId,
                    classId = student.classId,
                    feeTypeId = feeType.id,
                    title = feeType.title,
                    amount = feeType.amount,
                    dueDate = dateFormat.format(Date(dueDate)),
                    status = "pending",
                    assignedAt = System.currentTimeMillis()
                )
                
                db.collection("colleges").document(collegeId)
                    .collection("fee_dues").document(dueId)
                    .set(feeDue).await()
                
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignFeeToClass(
        collegeId: String,
        classId: String,
        className: String,
        feeType: FeeType,
        dueDate: Long,
        onComplete: (Boolean, String?) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Find all students in this class
                val targetStudents = _students.value.filter { it.classId == classId }
                if (targetStudents.isEmpty()) {
                    _isLoading.value = false
                    onComplete(false, "No students found in $className.")
                    return@launch
                }

                val batch = db.batch()
                val duesRef = db.collection("colleges").document(collegeId).collection("fee_dues")
                val activityLogRef = db.collection("colleges").document(collegeId).collection("activity_logs").document()

                targetStudents.forEach { student ->
                    val newDueRef = duesRef.document()
                    val feeDue = FeeDue(
                        id = newDueRef.id,
                        collegeId = collegeId,
                        studentId = student.userId,
                        classId = student.classId,
                        feeTypeId = feeType.id,
                        title = feeType.title,
                        amount = feeType.amount,
                        dueDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(dueDate)),
                        status = "pending",
                        assignedAt = System.currentTimeMillis()
                    )
                    batch.set(newDueRef, feeDue)
                }
                
                val logData = hashMapOf(
                    "logId" to activityLogRef.id,
                    "collegeId" to collegeId,
                    "title" to "Class Fee Assigned",
                    "description" to "Assigned ₹${feeType.amount} (${feeType.title}) to ${targetStudents.size} students in $className.",
                    "type" to "fee",
                    "timestamp" to System.currentTimeMillis()
                )
                batch.set(activityLogRef, logData)

                batch.commit().await()
                _isLoading.value = false
                onComplete(true, "Fee successfully assigned to ${targetStudents.size} students.")
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onComplete(false, e.message)
            }
        }
    }
}
