package com.campussync.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await

class AdminStaffViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _activeStaff = MutableStateFlow<List<User>>(emptyList())
    val activeStaff: StateFlow<List<User>> = _activeStaff.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deactivatedStaff = MutableStateFlow<List<User>>(emptyList())
    val deactivatedStaff: StateFlow<List<User>> = _deactivatedStaff.asStateFlow()

    private val _teacherSubjects = MutableStateFlow<List<com.campussync.app.core.model.SubjectAssignment>>(emptyList())
    val teacherSubjects: StateFlow<List<com.campussync.app.core.model.SubjectAssignment>> = _teacherSubjects.asStateFlow()

    private val _classes = MutableStateFlow<List<com.campussync.app.feature.attendance.ClassOverview>>(emptyList())
    val classes: StateFlow<List<com.campussync.app.feature.attendance.ClassOverview>> = _classes.asStateFlow()

    fun loadClasses(collegeId: String) {
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
                            needsReload = true
                        }
                    }
                }
                
                val finalSnapshot = if (needsReload) db.collection("colleges").document(collegeId).collection("classes").get().await() else classesSnapshot

                val classList = finalSnapshot.documents.mapNotNull { doc ->
                    com.campussync.app.feature.attendance.ClassOverview(
                        classId = doc.id,
                        name = doc.getString("course") ?: doc.getString("name") ?: doc.id,
                        branch = doc.getString("branch") ?: "",
                        semester = doc.getString("semester") ?: ""
                    )
                }
                _classes.value = classList.sortedBy { it.name }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadTeacherSubjects(collegeId: String, teacherId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("colleges").document(collegeId)
                    .collection("subject_assignments")
                    .whereEqualTo("teacherId", teacherId)
                    .get()
                    .await()
                _teacherSubjects.value = snapshot.documents.mapNotNull { it.toObject(com.campussync.app.core.model.SubjectAssignment::class.java)?.copy(id = it.id) }
            } catch (e: Exception) {
                _teacherSubjects.value = emptyList()
            }
        }
    }

    fun addSubjectAssignment(collegeId: String, classId: String, teacherId: String, teacherName: String, subjectName: String, semester: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val collectionRef = db.collection("colleges").document(collegeId).collection("subject_assignments")
                
                // Duplicate check
                val existing = collectionRef
                    .whereEqualTo("classId", classId)
                    .whereEqualTo("subjectName", subjectName)
                    .get()
                    .await()
                    
                if (!existing.isEmpty) {
                    onComplete(false, "Subject '$subjectName' is already assigned to this class.")
                    return@launch
                }

                val docRef = collectionRef.document()
                val assignment = com.campussync.app.core.model.SubjectAssignment(
                    id = docRef.id,
                    collegeId = collegeId,
                    classId = classId,
                    teacherId = teacherId,
                    teacherName = teacherName,
                    subjectName = subjectName,
                    semester = semester
                )
                docRef.set(assignment).await()
                loadTeacherSubjects(collegeId, teacherId) // refresh
                onComplete(true, "Subject assigned successfully.")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Error assigning subject")
            }
        }
    }

    fun deleteSubjectAssignment(collegeId: String, teacherId: String, subjectId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("colleges").document(collegeId)
                    .collection("subject_assignments").document(subjectId)
                    .delete().await()
                loadTeacherSubjects(collegeId, teacherId) // refresh
                onComplete(true, "Subject unassigned.")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Error unassigning subject")
            }
        }
    }

    fun loadStaff(collegeId: String) = loadAllStaff(collegeId)

    fun inviteAdmin(email: String, name: String, collegeId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val emailLower = email.lowercase().trim()
                val inviteData = hashMapOf(
                    "email" to emailLower,
                    "role" to "admin",
                    "collegeId" to collegeId,
                    "name" to name.trim(),
                    "status" to "pending"
                )
                db.collection("invites").document(emailLower).set(inviteData).await()
                onComplete(true, "Admin invite created successfully!")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to create admin invite")
            }
        }
    }

    fun loadAllStaff(collegeId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val teachersDef = async { db.collection("colleges").document(collegeId).collection("teachers").get().await() }
                val adminsDef = async { db.collection("colleges").document(collegeId).collection("admins").get().await() }
                val studentsDef = async { db.collection("colleges").document(collegeId).collection("students").get().await() }
                
                val allUsers = mutableListOf<User>()
                allUsers.addAll(teachersDef.await().documents.mapNotNull { doc -> doc.toObject(User::class.java)?.copy(userId = doc.id) })
                allUsers.addAll(adminsDef.await().documents.mapNotNull { doc -> doc.toObject(User::class.java)?.copy(userId = doc.id) })
                allUsers.addAll(studentsDef.await().documents.mapNotNull { doc -> doc.toObject(User::class.java)?.copy(userId = doc.id) })

                _activeStaff.value = allUsers.filter { it.isActive }
                _deactivatedStaff.value = allUsers.filter { !it.isActive }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deactivateUser(uid: String, role: String, collegeId: String, onComplete: (Boolean, String) -> Unit) {
        updateUserStatus(uid, role, collegeId, false, onComplete)
    }

    fun activateUser(uid: String, role: String, collegeId: String, onComplete: (Boolean, String) -> Unit) {
        updateUserStatus(uid, role, collegeId, true, onComplete)
    }

    private fun updateUserStatus(uid: String, role: String, collegeId: String, isActive: Boolean, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val userRef = db.collection("users").document(uid)
                batch.update(userRef, "isActive", isActive)
                
                val subCollection = when (role.lowercase()) {
                    "principal" -> "principals"
                    "admin" -> "admins"
                    "student" -> "students"
                    else -> "teachers"
                }
                val profileRef = db.collection("colleges").document(collegeId)
                    .collection(subCollection).document(uid)
                batch.update(profileRef, "isActive", isActive)
                
                batch.commit().await()
                loadAllStaff(collegeId)
                onComplete(true, if (isActive) "User activated" else "User deactivated")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to update status")
            }
        }
    }

    fun promoteToAdmin(user: User, collegeId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                
                // 1. Update root users collection
                batch.update(db.collection("users").document(user.userId), "role", "admin")
                
                // 2. Add to admins collection
                val adminProfile = user.copy(role = "admin")
                batch.set(
                    db.collection("colleges").document(collegeId)
                        .collection("admins").document(user.userId),
                    adminProfile
                )
                
                // 3. Remove from old role collection
                val oldSubCollection = when (user.role.lowercase()) {
                    "principal" -> "principals"
                    "student" -> "students"
                    else -> "teachers"
                }
                if (oldSubCollection != "admins") {
                    batch.delete(
                        db.collection("colleges").document(collegeId)
                            .collection(oldSubCollection).document(user.userId)
                    )
                }
                
                batch.commit().await()
                loadAllStaff(collegeId)
                onComplete(true, "Successfully promoted  to Admin.")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to promote user.")
            }
        }
    }
}
