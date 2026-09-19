package com.campussync.app.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.Holiday
import com.campussync.app.core.repository.HolidayRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HolidayViewModel(
    private val repository: HolidayRepository = HolidayRepository()
) : ViewModel() {

    private val _holidays = MutableStateFlow<List<Holiday>>(emptyList())
    val holidays: StateFlow<List<Holiday>> = _holidays.asStateFlow()

    private val _holidayDates = MutableStateFlow<Set<String>>(emptySet())
    val holidayDates: StateFlow<Set<String>> = _holidayDates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    /** Listen to holidays in real-time */
    fun listenToHolidays(collegeId: String) {
        listenerRegistration?.remove()
        listenerRegistration = repository.listenToHolidays(collegeId) { holidayList ->
            _holidays.value = holidayList
            _holidayDates.value = holidayList.map { it.date }.toSet()
        }
    }

    /** Add a new holiday */
    fun addHoliday(collegeId: String, holiday: Holiday, onResult: (Boolean) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.addHoliday(collegeId, holiday)
            _isLoading.value = false
            onResult(result.isSuccess)
        }
    }

    /** Delete a holiday */
    fun deleteHoliday(collegeId: String, holidayId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteHoliday(collegeId, holidayId)
            onResult(result.isSuccess)
        }
    }

    /** Initialize default holidays if none exist */
    fun initializeDefaultHolidays(collegeId: String) {
        viewModelScope.launch {
            val existing = repository.getHolidays(collegeId)
            if (existing.isEmpty()) {
                val defaults = repository.getDefaultHolidays()
                defaults.forEach { holiday ->
                    repository.addHoliday(collegeId, holiday.copy(createdBy = "system"))
                }
            }
        }
    }

    /** Check if a date is a holiday */
    fun isHoliday(date: String): Boolean {
        return _holidayDates.value.contains(date)
    }

    /** Check if a date is Sunday */
    fun isSunday(date: String): Boolean {
        return try {
            val localDate = java.time.LocalDate.parse(date)
            localDate.dayOfWeek == java.time.DayOfWeek.SUNDAY
        } catch (e: Exception) {
            false
        }
    }

    /** Check if a date should be excluded from attendance (Sunday or Holiday) */
    fun shouldExcludeFromAttendance(date: String): Boolean {
        return isSunday(date) || isHoliday(date)
    }

    /** Get excluded dates for a month */
    fun getExcludedDatesForMonth(yearMonth: String): Set<String> {
        return try {
            val year = yearMonth.substring(0, 4).toInt()
            val month = yearMonth.substring(5, 7).toInt()
            val firstDay = java.time.LocalDate.of(year, month, 1)
            val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())

            val excludedDates = mutableSetOf<String>()
            var current = firstDay

            while (!current.isAfter(lastDay)) {
                val dateStr = current.toString()
                if (shouldExcludeFromAttendance(dateStr)) {
                    excludedDates.add(dateStr)
                }
                current = current.plusDays(1)
            }
            excludedDates
        } catch (e: Exception) {
            emptySet()
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
