package com.campussync.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val pendingActionCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

class StudentDashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Example of user details that would normally be passed in or fetched from AuthRepo
    private val currentUserCollegeId = "example_college_id"
    private val currentUserId = "example_student_id"

    /**
     * RULE 1: The Auto-Kill Listener (callbackFlow + awaitClose)
     * We wrap the Firebase snapshot listener inside a callbackFlow.
     * When the flow is cancelled/stopped, awaitClose guarantees the listener is destroyed.
     */
    private val dashboardUpdatesFlow = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            // Send initial loading state
            trySend(DashboardState(isLoading = true))

            val query = db.collection("colleges").document(currentUserCollegeId)
                .collection("students").document(currentUserId)
                .collection("pending_actions")

            // Attach Real-Time Listener
            listener = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(DashboardState(isLoading = false, error = e.message))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val count = snapshot.documents.size
                    trySend(DashboardState(pendingActionCount = count, isLoading = false))
                }
            }
        } catch (e: Exception) {
            trySend(DashboardState(isLoading = false, error = e.message))
        }

        // CRITICAL: Explicitly kill the listener when the flow collection stops!
        awaitClose { 
            listener?.remove() 
        }
    }

    /**
     * RULE 2: The 5-Second Sleep Rule (WhileSubscribed)
     * By converting the callbackFlow to a StateFlow using WhileSubscribed(5000),
     * if the UI (StudentDashboardScreen) is completely hidden or destroyed for more than 5 seconds,
     * this StateFlow pauses, which halts the underlying callbackFlow, 
     * which in turn triggers awaitClose and kills the Firebase listener!
     * 
     * When the user returns to the screen, the listener restarts automatically.
     */
    val dashboardState: StateFlow<DashboardState> = dashboardUpdatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // 5-Second Grace Period
        initialValue = DashboardState(isLoading = true)
    )
}
