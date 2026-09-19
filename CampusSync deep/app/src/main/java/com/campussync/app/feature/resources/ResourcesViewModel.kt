package com.campussync.app.feature.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.Resource
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResourcesViewModel(
    private val repository: ResourcesRepository = ResourcesRepository()
) : ViewModel() {

    private val _resources = MutableStateFlow<List<Resource>>(emptyList())
    val resources: StateFlow<List<Resource>> = _resources.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<Result<Unit>?>(null)
    val uploadStatus: StateFlow<Result<Unit>?> = _uploadStatus.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun fetchResources(collegeId: String, classId: String, branch: String, semester: String) {
        // Prevent duplicate listeners
        listenerRegistration?.remove()
        
        if (collegeId.isBlank() || classId.isBlank()) {
            _resources.value = emptyList()
            return
        }

        _isLoading.value = true
        listenerRegistration = repository.listenToResources(
            collegeId = collegeId,
            classId = classId,
            branch = branch,
            semester = semester,
            onUpdate = { updatedList ->
                // The repository already returns a .toList() copy, but we do it again just to be safe
                // StateFlow will only emit if the object reference changes
                _resources.value = updatedList.toList()
                _isLoading.value = false
            }
        )
    }

    fun uploadResource(
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
    ) {
        viewModelScope.launch {
            _uploadStatus.value = null
            // We do NOT set _isLoading.value = true here because it interrupts the real-time listener UI
            val result = repository.uploadResource(
                collegeId, classId, driveLink, uploaderId, uploaderName, title, description, branch, semester, subject, tags, fileType
            )
            _uploadStatus.value = result
            // Real-time listener automatically picks up the new document and updates _resources
        }
    }

    fun resetUploadStatus() {
        _uploadStatus.value = null
    }

    fun likeResource(collegeId: String, classId: String, resourceId: String, branch: String, semester: String) {
        viewModelScope.launch {
            repository.likeResource(collegeId, classId, resourceId)
        }
    }

    fun deleteResource(collegeId: String, classId: String, resourceId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteResource(collegeId, classId, resourceId)
            onDone(res.isSuccess)
        }
    }

    fun updateResource(
        collegeId: String,
        classId: String,
        resourceId: String,
        title: String,
        description: String,
        fileUrl: String,
        fileType: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateResource(collegeId, classId, resourceId, title, description, fileUrl, fileType)
            onDone(res.isSuccess)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Extremely important: Kill the listener to prevent memory leaks and zombie updates
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
