package com.campussync.app.core.repository

import com.campussync.app.core.model.Holiday
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class HolidayRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun getCollection(collegeId: String) =
        firestore.collection("colleges").document(collegeId).collection("holidays")

    /** Add a new holiday */
    suspend fun addHoliday(collegeId: String, holiday: Holiday): Result<String> {
        return try {
            val ref = getCollection(collegeId).document()
            val newHoliday = holiday.copy(id = ref.id)
            ref.set(newHoliday).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Delete a holiday */
    suspend fun deleteHoliday(collegeId: String, holidayId: String): Result<Unit> {
        return try {
            getCollection(collegeId).document(holidayId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get all holidays for a college */
    suspend fun getHolidays(collegeId: String): List<Holiday> {
        return try {
            val snapshot = getCollection(collegeId)
                .orderBy("date")
                .get()
                .await()
            snapshot.toObjects(Holiday::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Listen to holidays in real-time */
    fun listenToHolidays(
        collegeId: String,
        onUpdate: (List<Holiday>) -> Unit
    ): ListenerRegistration {
        return getCollection(collegeId)
            .orderBy("date")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val holidays = snapshot.toObjects(Holiday::class.java)
                onUpdate(holidays)
            }
    }

    /** Check if a specific date is a holiday */
    suspend fun isHoliday(collegeId: String, date: String): Boolean {
        return try {
            val snapshot = getCollection(collegeId)
                .whereEqualTo("date", date)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /** Get all holiday dates as a Set for quick lookup */
    suspend fun getHolidayDates(collegeId: String): Set<String> {
        return try {
            val holidays = getHolidays(collegeId)
            holidays.map { it.date }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** Pre-defined Indian holidays for 2026 */
    fun getDefaultHolidays(): List<Holiday> {
        return listOf(
            Holiday(date = "2026-01-26", title = "Republic Day", description = "National Holiday"),
            Holiday(date = "2026-03-10", title = "Holi", description = "Festival of Colors"),
            Holiday(date = "2026-04-02", title = "Ram Navami", description = "Hindu Festival"),
            Holiday(date = "2026-04-14", title = "Dr. Ambedkar Jayanti", description = "National Holiday"),
            Holiday(date = "2026-05-01", title = "May Day", description = "International Workers' Day"),
            Holiday(date = "2026-08-15", title = "Independence Day", description = "National Holiday"),
            Holiday(date = "2026-08-27", title = "Janmashtami", description = "Hindu Festival"),
            Holiday(date = "2026-10-02", title = "Gandhi Jayanti", description = "National Holiday"),
            Holiday(date = "2026-10-20", title = "Dussehra", description = "Hindu Festival"),
            Holiday(date = "2026-11-08", title = "Diwali", description = "Festival of Lights"),
            Holiday(date = "2026-12-25", title = "Christmas", description = "Christian Festival"),
            Holiday(date = "2026-01-01", title = "New Year", description = "Public Holiday"),
            Holiday(date = "2026-03-30", title = "Id-ul-Fitr", description = "Islamic Festival"),
            Holiday(date = "2026-06-06", title = "Id-ul-Adha", description = "Islamic Festival"),
        )
    }
}
