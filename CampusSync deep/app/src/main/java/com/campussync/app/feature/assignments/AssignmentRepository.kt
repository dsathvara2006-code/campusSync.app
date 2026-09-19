package com.campussync.app.feature.assignments

import com.campussync.app.core.model.Assignment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AssignmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getAssignmentsForClass(collegeId: String, classId: String): List<Assignment> {
        return try {
            val snapshot = firestore.collection("colleges").document(collegeId).collection("classes")
                .document(classId)
                .collection("assignments")
                .get()
                .await()
            snapshot.toObjects(Assignment::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addAssignment(collegeId: String, assignment: Assignment): Result<Unit> {
        return try {
            val ref = firestore
                .collection("colleges").document(collegeId).collection("classes")
                .document(assignment.classId)
                .collection("assignments")
                .document()
            val newAssignment = assignment.copy(id = ref.id)
            ref.set(newAssignment).await()  // await ensures data reaches Firestore server
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAssignment(collegeId: String, classId: String, assignmentId: String): Result<Unit> {
        return try {
            firestore.collection("colleges").document(collegeId).collection("classes")
                .document(classId)
                .collection("assignments")
                .document(assignmentId)
                .delete()
                .await()  // await ensures delete reaches Firestore server
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Real-time listener — fires immediately on load AND whenever any change happens on server
    fun listenToAssignments(
        collegeId: String,
        classId: String,
        onUpdate: (List<Assignment>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("colleges").document(collegeId).collection("classes")
            .document(classId)
            .collection("assignments")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val assignments = snapshot
                    .toObjects(Assignment::class.java)
                    .sortedByDescending { it.createdAt }
                onUpdate(assignments)
            }
    }

    suspend fun markAssignmentDone(collegeId: String, classId: String, assignmentId: String, studentId: String, isDone: Boolean): Result<Unit> {
        return try {
            val ref = firestore.collection("colleges").document(collegeId).collection("classes").document(classId).collection("assignments").document(assignmentId)
            val update = if (isDone) {
                com.google.firebase.firestore.FieldValue.arrayUnion(studentId)
            } else {
                com.google.firebase.firestore.FieldValue.arrayRemove(studentId)
            }
            ref.update("completedBy", update).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

