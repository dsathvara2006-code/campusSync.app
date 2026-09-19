package com.campussync.app.feature.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.Attendance
import com.campussync.app.feature.auth.CollegeClass
import com.campussync.app.feature.auth.PrincipalRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailySummaryUiState(
    val isLoading: Boolean = true,
    val selectedDate: String = "",
    val records: List<Pair<CollegeClass, Attendance>> = emptyList(),
    val errorMessage: String? = null
)

class DailySummaryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val attendanceRepository = AttendanceRepository()
    private val principalRepository = PrincipalRepository(application)
    
    private val _uiState = MutableStateFlow(DailySummaryUiState())
    val uiState: StateFlow<DailySummaryUiState> = _uiState.asStateFlow()

    init {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(selectedDate = today) }
    }

    fun loadSummary(collegeId: String, date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedDate = date) }
            try {
                // 1. Fetch all classes for this college
                val classesResult = principalRepository.getClasses(collegeId)
                if (classesResult.isFailure) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = classesResult.exceptionOrNull()?.message ?: "Failed to load classes"
                        ) 
                    }
                    return@launch
                }
                
                val classes = classesResult.getOrDefault(emptyList())
                if (classes.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, records = emptyList()) }
                    return@launch
                }

                // 2. Concurrently fetch attendance for all classes for the specified date
                val deferredAttendances = classes.map { collegeClass ->
                    async {
                        val attendances = attendanceRepository.getAllAttendancesForDate(
                            collegeId = collegeId,
                            classId = collegeClass.classId,
                            date = date
                        )
                        // Pair each attendance record with its corresponding class
                        attendances.map { attendance -> Pair(collegeClass, attendance) }
                    }
                }
                
                val allRecords = deferredAttendances.awaitAll().flatten()
                
                // Sort by time/subject name (or whatever logic is preferred, we'll sort by class name then subject)
                val sortedRecords = allRecords.sortedWith(
                    compareBy({ it.first.className }, { it.second.subjectName })
                )

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        records = sortedRecords
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "An unexpected error occurred"
                    ) 
                }
            }
        }
    }
}
