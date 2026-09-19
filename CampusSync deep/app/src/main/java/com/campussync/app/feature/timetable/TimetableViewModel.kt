package com.campussync.app.feature.timetable

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.TimetableEntry
import com.campussync.app.feature.timetable.TimetableRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class TimetableViewModel(
    private val repository: TimetableRepository = TimetableRepository()
) : ViewModel() {

    var timetable = mutableStateOf<List<TimetableEntry>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    private var listenerRegistration: ListenerRegistration? = null

    fun loadTimetable(collegeId: String, classId: String) {
        if (timetable.value.isEmpty()) {
            isLoading.value = true
        }
        listenerRegistration?.remove()
        
        listenerRegistration = repository.listenToTimetable(
            collegeId = collegeId,
            classId = classId,
            onUpdate = { newTimetable ->
                timetable.value = newTimetable
                isLoading.value = false
            }
        )
    }

    fun addTimetableEntry(collegeId: String, entry: TimetableEntry, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.addTimetableEntry(collegeId, entry)
            if (result.isSuccess) {
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }
    
    fun deleteTimetableEntry(collegeId: String, classId: String, id: String) {
        viewModelScope.launch {
            repository.deleteTimetableEntry(collegeId, classId, id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

