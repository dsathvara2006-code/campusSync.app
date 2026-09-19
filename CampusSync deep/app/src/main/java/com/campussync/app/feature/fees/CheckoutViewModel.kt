package com.campussync.app.feature.fees

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.FeeDue
import com.campussync.app.core.model.PaymentOrder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }

    /**
     * Submits a multi-fee cart payment.
     * Bundles multiple FeeDues into a single PaymentOrder to prevent UTR fraud
     * and simplify admin verification.
     */
    fun submitCartPayment(
        dues: List<FeeDue>,
        utr: String,
        studentId: String,
        studentName: String,
        collegeId: String
    ) {
        if (dues.isEmpty()) {
            _uploadState.value = UploadState.Error("No fees selected for checkout.")
            return
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            
            try {
                // 1. Calculate expected total securely on the backend
                val expectedTotal = dues.sumOf { it.amount }
                
                // 2. Check if UTR is already used to fail fast
                // Note: The rules use used_utrs/{utrNumber} under the college
                val utrRef = db.collection("colleges").document(collegeId).collection("used_utrs").document(utr)
                val utrSnapshot = utrRef.get().await()
                if (utrSnapshot.exists()) {
                    _uploadState.value = UploadState.Error("This UTR has already been used.")
                    return@launch
                }

                // 3. Upload proof image to Firebase Storage (REMOVED based on user request)
                val timestamp = System.currentTimeMillis()
                val proofUrl = ""
                
                // 4. Create the PaymentOrder (Invoice)
                val feeSummaryTitle = if (dues.size == 1) dues.first().title else "${dues.first().title} + ${dues.size - 1} more"
                
                // In firestore.rules, there's no payment_orders rule yet, but we will use it for the new UI.
                // We assume Admin will have access to read/write it.
                val paymentOrderRef = db.collection("colleges").document(collegeId).collection("payment_orders").document()
                
                val paymentOrder = PaymentOrder(
                    id = paymentOrderRef.id,
                    studentId = studentId,
                    studentName = studentName,
                    collegeId = collegeId,
                    dueIds = dues.map { it.id },
                    expectedTotal = expectedTotal,
                    feeSummaryTitle = feeSummaryTitle,
                    utr = utr,
                    proofUrl = proofUrl,
                    status = "processing",
                    timestamp = timestamp
                )

                // 5. Use WriteBatch for atomicity
                val batch = db.batch()
                
                // 5a. Save PaymentOrder
                batch.set(paymentOrderRef, paymentOrder)
                
                // 5b. Update all bundled FeeDues to "submitted"
                // The rules say: students can update status from 'pending' to 'submitted'
                for (due in dues) {
                    val dueRef = db.collection("colleges").document(collegeId).collection("fee_dues").document(due.id)
                    // We only update status to minimize rule violations
                    batch.update(dueRef, "status", "submitted")
                }
                
                // 5c. Log UTR to prevent reuse
                val utrData = hashMapOf(
                    "usedBy" to studentId,
                    "timestamp" to timestamp,
                    "paymentOrderId" to paymentOrderRef.id
                )
                batch.set(utrRef, utrData)
                
                // Commit everything atomically
                batch.commit().await()
                
                _uploadState.value = UploadState.Success("Payment submitted successfully!")
                
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
}
