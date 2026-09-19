package com.campussync.app.feature.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.FeeDue
import com.campussync.app.core.model.FeeSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class CheckoutState {
    object Idle : CheckoutState()
    object Loading : CheckoutState()
    data class Success(val message: String) : CheckoutState()
    data class Error(val errorMsg: String) : CheckoutState()
}

class SparkFeeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()
    
    private val _feeDues = MutableStateFlow<List<FeeDue>>(emptyList())
    val feeDues: StateFlow<List<FeeDue>> = _feeDues.asStateFlow()

    private val _feeSettings = MutableStateFlow<FeeSettings?>(null)
    val feeSettings: StateFlow<FeeSettings?> = _feeSettings.asStateFlow()

    fun loadStudentFees(collegeId: String, studentId: String) {
        viewModelScope.launch {
            try {
                // Load Fee Settings for College UPI
                val settingsDoc = db.collection("colleges").document(collegeId)
                    .collection("settings").document("fee_settings").get().await()
                if (settingsDoc.exists()) {
                    _feeSettings.value = settingsDoc.toObject(FeeSettings::class.java)
                }

                val duesSnapshot = db.collection("colleges").document(collegeId)
                    .collection("fee_dues")
                    .whereEqualTo("studentId", studentId)
                    .get().await()
                
                _feeDues.value = duesSnapshot.documents.mapNotNull { it.toObject(FeeDue::class.java) }
                    .sortedByDescending { it.assignedAt }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Submits a multi-fee checkout using ONLY the UTR number (No Firebase Storage).
     * This is heavily optimized for speed and the Firebase Spark Plan.
     * Uses WriteBatch for 100% atomicity.
     */
    fun submitFeeWithoutStorage(
        collegeId: String,
        studentId: String,
        studentName: String,
        selectedDues: List<FeeDue>,
        totalAmount: Double,
        utrNumber: String
    ) {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading
            try {
                // 1. Check if UTR is already used to prevent duplicate spam
                val utrRef = db.collection("colleges").document(collegeId)
                    .collection("used_utrs").document(utrNumber)
                    
                val utrSnapshot = utrRef.get().await()
                if (utrSnapshot.exists()) {
                    _checkoutState.value = CheckoutState.Error("This UTR has already been used.")
                    return@launch
                }

                // 2. Prepare Atomic Batch
                val batch = db.batch()
                val paymentId = UUID.randomUUID().toString()

                // Lock UTR
                val utrData = hashMapOf(
                    "usedBy" to studentId,
                    "paymentId" to paymentId,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                batch.set(utrRef, utrData)

                // Create Unified Payment Order
                val orderRef = db.collection("colleges").document(collegeId)
                    .collection("payment_orders").document(paymentId)
                
                val dueIds = selectedDues.map { it.id }
                val summaryTitle = selectedDues.joinToString(" + ") { it.title }

                val orderData = hashMapOf(
                    "id" to paymentId,
                    "studentId" to studentId,
                    "studentName" to studentName,
                    "dueIds" to dueIds,
                    "feeSummaryTitle" to summaryTitle,
                    "expectedTotal" to totalAmount,
                    "utr" to utrNumber,
                    "status" to "processing", // Admin will cross-reference bank statement
                    "timestamp" to System.currentTimeMillis()
                )
                batch.set(orderRef, orderData)

                // Mark selected fee_dues as submitted
                selectedDues.forEach { due ->
                    val dueRef = db.collection("colleges").document(collegeId)
                        .collection("fee_dues").document(due.id)
                    batch.update(dueRef, "status", "submitted")
                }

                // Commit the transaction securely
                batch.commit().await()
                
                _checkoutState.value = CheckoutState.Success("Checkout successful! Please wait for Admin verification.")
                loadStudentFees(collegeId, studentId) // Refresh list
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error(e.message ?: "Failed to submit fee")
            }
        }
    }

    fun downloadReceipt(feeDue: FeeDue, studentName: String, collegeId: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                // Fetch the UTR from the payment order
                var utr = "Verified"
                var timestamp = System.currentTimeMillis()
                try {
                    val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val orderSnapshot = db.collection("colleges").document(collegeId)
                        .collection("payment_orders")
                        .whereEqualTo("studentId", authUid)
                        .whereArrayContains("dueIds", feeDue.id)
                        .limit(1)
                        .get().await()
                    
                    if (!orderSnapshot.isEmpty) {
                        utr = orderSnapshot.documents[0].getString("utr") ?: "Verified"
                        timestamp = orderSnapshot.documents[0].getLong("timestamp") ?: timestamp
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReceiptDownload", "Failed to fetch UTR: ${e.message}")
                }

                // Fetch College Name
                var collegeName = "Campus Sync College"
                try {
                    val collegeSnapshot = db.collection("colleges").document(collegeId).get().await()
                    if (collegeSnapshot.exists()) {
                        collegeName = collegeSnapshot.getString("name") ?: collegeName
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReceiptDownload", "Failed to fetch college name: ${e.message}")
                }

                val pdfBuilder = com.campussync.app.core.utils.ReceiptPdfBuilder(context)
                val currentDate = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                
                val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                val shortId = feeDue.id.substring(0, 4).uppercase()
                val receiptNumber = "REC-$dateStr-$shortId"

                val receiptData = com.campussync.app.core.utils.ReceiptData(
                    studentName = studentName,
                    receiptNumber = receiptNumber,
                    amount = "₹${feeDue.amount}",
                    feeType = feeDue.title,
                    utr = utr,
                    date = currentDate,
                    collegeName = collegeName
                )
                
                val result = pdfBuilder.generateReceiptPdf(receiptData)
                result.onSuccess { uriString ->
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Receipt Downloaded: CampusSync folder", android.widget.Toast.LENGTH_LONG).show()
                        
                        // Attempt to open the PDF
                        try {
                            val uri = if (uriString.startsWith("content://")) {
                                android.net.Uri.parse(uriString)
                            } else {
                                androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    context.packageName + ".provider",
                                    java.io.File(uriString)
                                )
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("ReceiptDownload", "No app found to open PDF", e)
                        }
                    }
                }.onFailure { e ->
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Failed to download receipt: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Unexpected Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }
}
