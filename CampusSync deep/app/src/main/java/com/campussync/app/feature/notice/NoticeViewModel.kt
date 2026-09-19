package com.campussync.app.feature.notice

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.Notice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoticeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    var notices = mutableStateOf<List<Notice>>(emptyList())
        private set

    // Init removed so we can pass collegeId manually

    fun listenToNotices(collegeId: String) {
        listenerRegistration?.remove()
        listenerRegistration = firestore.collection("colleges").document(collegeId).collection("notices")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val fetchedNotices = snapshot.toObjects(Notice::class.java)
                notices.value = fetchedNotices
            }
    }

    fun addNotice(collegeId: String, message: String, onComplete: (Boolean) -> Unit) {
        val sanitizedMessage = message.trim().take(500) // Max 500 chars
        if (sanitizedMessage.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            try {
                val ref = firestore.collection("colleges").document(collegeId).collection("notices").document()
                val notice = Notice(id = ref.id, message = sanitizedMessage)
                ref.set(notice).await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

