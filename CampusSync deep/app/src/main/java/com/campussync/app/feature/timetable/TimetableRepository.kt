package com.campussync.app.feature.timetable

import com.campussync.app.core.model.TimetableEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class TimetableRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getTimetableForClass(collegeId: String, classId: String): List<TimetableEntry> {
        if (classId.isBlank()) return emptyList()
        return try {
            val snapshot = firestore.collection("colleges").document(collegeId).collection("classes")
                .document(classId)
                .collection("timetable")
                .get()
                .await()
            snapshot.toObjects(TimetableEntry::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTimetableEntry(collegeId: String, entry: TimetableEntry): Result<Unit> {
        return try {
            val ref = firestore.collection("colleges").document(collegeId).collection("classes").document(entry.classId).collection("timetable").document()
            val newEntry = entry.copy(id = ref.id)
            ref.set(newEntry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTimetableEntry(collegeId: String, classId: String, id: String): Result<Unit> {
        return try {
            firestore.collection("colleges").document(collegeId).collection("classes").document(classId).collection("timetable").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToTimetable(collegeId: String, classId: String, onUpdate: (List<TimetableEntry>) -> Unit): ListenerRegistration? {
        if (classId.isBlank()) {
            onUpdate(emptyList())
            return null
        }
        return firestore.collection("colleges").document(collegeId).collection("classes")
            .document(classId)
            .collection("timetable")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val entries = snapshot.toObjects(TimetableEntry::class.java).sortedBy { it.time }
                onUpdate(entries)
            }
    }
}

