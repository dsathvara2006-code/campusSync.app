package com.campussync.app.feature.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.PaymentOrder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ApprovePaymentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pendingOrders = MutableStateFlow<List<PaymentOrder>>(emptyList())
    val pendingOrders: StateFlow<List<PaymentOrder>> = _pendingOrders.asStateFlow()

    private val _historyOrders = MutableStateFlow<List<PaymentOrder>>(emptyList())
    val historyOrders: StateFlow<List<PaymentOrder>> = _historyOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPendingOrders(collegeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("colleges").document(collegeId)
                    .collection("payment_orders")
                    .whereEqualTo("status", "processing")
                    .get().await()
                
                _pendingOrders.value = snapshot.documents.mapNotNull { it.toObject(PaymentOrder::class.java) }
                    .sortedBy { it.timestamp }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHistoryOrders(collegeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("colleges").document(collegeId)
                    .collection("payment_orders")
                    .whereIn("status", listOf("approved", "rejected"))
                    .get().await()
                
                _historyOrders.value = snapshot.documents.mapNotNull { it.toObject(PaymentOrder::class.java) }
                    .sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrderStatus(collegeId: String, order: PaymentOrder, isApproved: Boolean, adminNote: String = "", onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newStatus = if (isApproved) "approved" else "rejected"
                val orderUpdates = mapOf(
                    "status" to newStatus,
                    "adminNote" to adminNote
                )

                val batch = db.batch()
                
                // 1. Update the Payment Order itself
                batch.update(
                    db.collection("colleges").document(collegeId)
                        .collection("payment_orders").document(order.id),
                    orderUpdates
                )

                // 2. Update all associated Fee Dues
                // If rejected, dues go back to "pending" so the student can try again
                val dueStatus = if (isApproved) "approved" else "pending"
                val dueUpdates = mapOf(
                    "status" to dueStatus,
                    "adminNote" to if (isApproved) "" else adminNote
                )
                order.dueIds.forEach { dueId ->
                    batch.update(
                        db.collection("colleges").document(collegeId)
                            .collection("fee_dues").document(dueId),
                        dueUpdates
                    )
                }

                // 3. If rejected, release the UTR so it can be used again
                if (!isApproved) {
                    batch.delete(
                        db.collection("colleges").document(collegeId)
                            .collection("used_utrs").document(order.utr)
                    )
                }

                batch.commit().await()

                loadPendingOrders(collegeId)
                loadHistoryOrders(collegeId)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
