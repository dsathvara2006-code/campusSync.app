package com.campussync.app.feature.resources

import android.net.Uri
import com.campussync.app.core.model.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ResourcesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun getCollection(collegeId: String, classId: String) = 
        firestore.collection("colleges").document(collegeId)
            .collection("classes").document(classId)
            .collection("uploads")
    
    suspend fun uploadResource(
        collegeId: String,
        classId: String,
        driveLink: String,
        uploaderId: String,
        uploaderName: String,
        title: String,
        description: String,
        branch: String,
        semester: String,
        subject: String,
        tags: List<String>,
        fileType: String
    ): Result<Unit> {
        return try {
            val resourceId = UUID.randomUUID().toString()
            val resource = Resource(
                resourceId = resourceId,
                uploaderId = uploaderId,
                uploaderName = uploaderName,
                title = title,
                description = description,
                fileUrl = driveLink,
                fileType = fileType,
                branch = branch,
                semester = semester,
                subject = subject,
                tags = tags
            )
            val docRef = getCollection(collegeId, classId).document(resourceId)
            docRef.set(resource).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getResources(collegeId: String, classId: String, branch: String, semester: String): List<Resource> {
        return try {
            val snapshot = getCollection(collegeId, classId)
                .whereEqualTo("branch", branch)
                .whereEqualTo("semester", semester)
                .get()
                .await()
            snapshot.documents
                .mapNotNull { it.toObject(Resource::class.java) }
                .sortedByDescending { it.uploadTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun listenToResources(
        collegeId: String,
        classId: String,
        branch: String,
        semester: String,
        onUpdate: (List<Resource>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        if (collegeId.isBlank() || classId.isBlank()) {
            onUpdate(emptyList())
            return null
        }

        return try {
            // INCLUDE MetadataChanges to trigger listener immediately on local writes before server sync
            getCollection(collegeId, classId)
                .whereEqualTo("branch", branch)
                .whereEqualTo("semester", semester)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    
                    val list = snapshot.documents
                        .mapNotNull { it.toObject(Resource::class.java) }
                        .sortedByDescending { it.uploadTimestamp }
                    
                    // Always return a fresh list instance to trigger StateFlow recomposition
                    onUpdate(list.toList())
                }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun deleteResource(collegeId: String, classId: String, resourceId: String): Result<Unit> {
        return try {
            getCollection(collegeId, classId).document(resourceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateResource(
        collegeId: String,
        classId: String,
        resourceId: String,
        title: String,
        description: String,
        fileUrl: String,
        fileType: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to title,
                "description" to description,
                "fileUrl" to fileUrl,
                "fileType" to fileType
            )
            getCollection(collegeId, classId).document(resourceId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeResource(collegeId: String, classId: String, resourceId: String): Result<Unit> {
        return try {
            val docRef = getCollection(collegeId, classId).document(resourceId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val newLikes = (snapshot.getLong("likesCount") ?: 0) + 1
                transaction.update(docRef, "likesCount", newLikes)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
