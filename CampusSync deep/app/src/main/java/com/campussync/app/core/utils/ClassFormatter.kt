package com.campussync.app.core.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

object ClassFormatter {
    private val cache = ConcurrentHashMap<String, String>()
    
    suspend fun getDisplayName(collegeId: String, classId: String): String {
        if (classId.isBlank()) return ""
        if (classId.contains("-") && classId.length < 20) return classId // It's already in BCA-1 format
        
        cache[classId]?.let { return it }
        
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("colleges").document(collegeId).collection("classes").document(classId).get().await()
            val course = doc.getString("course") ?: ""
            val sem = doc.getString("semester") ?: ""
            
            val formatted = if (course.isNotBlank() && sem.isNotBlank()) {
                "$course-$sem"
            } else {
                classId // fallback to raw id
            }
            
            cache[classId] = formatted
            formatted
        } catch (e: Exception) {
            classId
        }
    }
}
