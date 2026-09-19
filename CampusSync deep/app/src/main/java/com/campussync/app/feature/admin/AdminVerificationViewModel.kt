package com.campussync.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminVerificationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _pendingUsers = MutableStateFlow<List<User>>(emptyList())
    val pendingUsers: StateFlow<List<User>> = _pendingUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPendingUsers(collegeId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // RUN AUTO-MIGRATION HERE TO ENSURE IT HAPPENS
                val classesSnapshot = db.collection("colleges").document(collegeId).collection("classes").get().await()
                for (doc in classesSnapshot.documents) {
                    if (!doc.id.contains("-") && doc.id.length >= 20) {
                        val course = doc.getString("course") ?: doc.getString("name") ?: ""
                        val sem = doc.getString("semester") ?: ""
                        if (course.isNotBlank() && sem.isNotBlank()) {
                            val newId = "${course}-${sem}"
                            db.collection("colleges").document(collegeId).collection("classes").document(newId).set(doc.data!!).await()
                            db.collection("colleges").document(collegeId).collection("classes").document(doc.id).delete().await()
                            
                            val students = db.collection("colleges").document(collegeId).collection("students").whereEqualTo("classId", doc.id).get().await()
                            for (sDoc in students.documents) {
                                db.collection("colleges").document(collegeId).collection("students").document(sDoc.id).update("classId", newId).await()
                            }
                            val subjects = db.collection("colleges").document(collegeId).collection("subject_assignments").whereEqualTo("classId", doc.id).get().await()
                            for (sDoc in subjects.documents) {
                                db.collection("colleges").document(collegeId).collection("subject_assignments").document(sDoc.id).update("classId", newId).await()
                            }
                            val rootUsers = db.collection("users").whereEqualTo("classId", doc.id).get().await()
                            for (uDoc in rootUsers.documents) {
                                db.collection("users").document(uDoc.id).update("classId", newId).await()
                            }
                        }
                    }
                }
                
                // Load actual pending users
                val teachers = db.collection("colleges").document(collegeId).collection("teachers").whereEqualTo("status", "pending").get().await()
                val students = db.collection("colleges").document(collegeId).collection("students").whereEqualTo("status", "pending").get().await()
                val admins = db.collection("colleges").document(collegeId).collection("admins").whereEqualTo("status", "pending").get().await()

                val allPending = mutableListOf<User>()
                allPending.addAll(teachers.documents.mapNotNull { it.toObject(User::class.java) })
                allPending.addAll(students.documents.mapNotNull { it.toObject(User::class.java) })
                allPending.addAll(admins.documents.mapNotNull { it.toObject(User::class.java) })

                _pendingUsers.value = allPending.sortedBy { it.createdAt }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveUser(uid: String, role: String, collegeId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val userRef = db.collection("users").document(uid)
                batch.update(userRef, "status", "approved")
                
                val subCollection = when (role.lowercase()) {
                    "principal" -> "principals"
                    "admin" -> "admins"
                    "student" -> "students"
                    else -> "teachers"
                }
                val profileRef = db.collection("colleges").document(collegeId)
                    .collection(subCollection).document(uid)
                batch.update(profileRef, "status", "approved")
                
                batch.commit().await()
                onComplete(true, "User approved successfully.")
                loadPendingUsers(collegeId)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Failed to approve user.")
            }
        }
    }

    fun rejectUser(uid: String, role: String, collegeId: String, reason: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val userRef = db.collection("users").document(uid)
                batch.update(userRef, mapOf(
                    "status" to "rejected",
                    "rejectionReason" to reason
                ))
                
                val subCollection = when (role.lowercase()) {
                    "principal" -> "principals"
                    "admin" -> "admins"
                    "student" -> "students"
                    else -> "teachers"
                }
                val profileRef = db.collection("colleges").document(collegeId)
                    .collection(subCollection).document(uid)
                batch.update(profileRef, mapOf(
                    "status" to "rejected",
                    "rejectionReason" to reason
                ))
                
                batch.commit().await()
                onComplete(true, "User rejected.")
                loadPendingUsers(collegeId)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Failed to reject user.")
            }
        }
    }
}
