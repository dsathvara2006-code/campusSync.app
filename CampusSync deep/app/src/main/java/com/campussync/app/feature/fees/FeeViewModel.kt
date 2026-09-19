package com.campussync.app.feature.fees

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.campussync.app.core.utils.ReceiptData
import com.campussync.app.core.utils.ReceiptPdfBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Sealed class for premium UI State Management
sealed class FeeApprovalState {
    object Idle : FeeApprovalState()
    object Loading : FeeApprovalState()
    data class Success(val receiptUri: String) : FeeApprovalState()
    data class Error(val message: String) : FeeApprovalState()
}

class FeeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val pdfBuilder = ReceiptPdfBuilder(application.applicationContext)

    private val _approvalState = MutableStateFlow<FeeApprovalState>(FeeApprovalState.Idle)
    val approvalState: StateFlow<FeeApprovalState> = _approvalState.asStateFlow()
    
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    /**
     * Approves the fee, updates Firestore, and generates the PDF receipt.
     */
    fun approvePayment(
        feeId: String,
        studentName: String,
        amount: String,
        feeType: String,
        utr: String
    ) {
        viewModelScope.launch {
            _approvalState.value = FeeApprovalState.Loading
            
            try {
                // Step 1: Update Firestore status
                val feeRef = db.collection("fees").document(feeId)
                
                // Using a map to simulate the update; ensures atomic field changes
                val updates = hashMapOf<String, Any>(
                    "status" to "approved",
                    "approvedAt" to System.currentTimeMillis()
                )
                
                feeRef.update(updates).await()

                // Step 2: Prepare data for the Receipt Generator
                val currentDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
                val receiptNumber = "REC-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"

                val receiptData = ReceiptData(
                    studentName = studentName,
                    receiptNumber = receiptNumber,
                    amount = amount,
                    feeType = feeType,
                    utr = utr,
                    date = currentDate,
                    collegeName = "Campus Sync College"
                )

                // Step 3: Generate the Local PDF
                val result = pdfBuilder.generateReceiptPdf(receiptData)

                result.onSuccess { uri ->
                    // Step 4: Update UI State to Success
                    _approvalState.value = FeeApprovalState.Success(uri)
                }.onFailure { error ->
                    _approvalState.value = FeeApprovalState.Error("DB updated, but PDF generation failed: ${error.message}")
                }

            } catch (e: Exception) {
                // Handle Network or Firestore errors
                _approvalState.value = FeeApprovalState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
    
    // Reset state after showing the Snackbar/Dialog
    fun resetState() {
        _approvalState.value = FeeApprovalState.Idle
        _uploadState.value = UploadState.Idle
    }
    
    /**
     * Executes the Zero-Fail Upload Architecture sequence.
     * Uploads the payment proof image to Firebase Storage, retrieves the download URL,
     * and performs a batch write to Firestore (saving the payment doc and marking the UTR as used).
     */
    fun submitPaymentProof(
        uri: Uri,
        studentId: String,
        utr: String,
        amount: String
    ) {
        viewModelScope.launch {
            // a) Emit UploadState.Loading
            _uploadState.value = UploadState.Loading
            
            try {
                // Check if UTR is already used first to fail fast
                val utrRef = db.collection("used_utrs").document(utr)
                val utrSnapshot = utrRef.get().await()
                if (utrSnapshot.exists()) {
                    _uploadState.value = UploadState.Error("This UTR has already been used.")
                    return@launch
                }

                // b) Upload the Uri image to Firebase Storage
                val timestamp = System.currentTimeMillis()
                val imageRef = storage.reference.child("payments/${studentId}_${timestamp}.jpg")
                
                // c) await() the upload and retrieve the downloadUrl
                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()
                
                // d) Prepare a Firestore HashMap
                val paymentDoc = hashMapOf(
                    "studentId" to studentId,
                    "utr" to utr,
                    "amount" to amount,
                    "screenshotUrl" to downloadUrl,
                    "status" to "pending",
                    "submittedAt" to timestamp
                )
                
                // e) Use a Firestore WriteBatch
                val batch = db.batch()
                
                // 1. Save the payment document
                val paymentRef = db.collection("fees_payments").document()
                batch.set(paymentRef, paymentDoc)
                
                // 2. Log the UTR in used_utrs to prevent duplicates
                val utrData = hashMapOf(
                    "usedBy" to studentId,
                    "timestamp" to timestamp,
                    "paymentId" to paymentRef.id
                )
                batch.set(utrRef, utrData)
                
                // Commit the batch
                batch.commit().await()
                
                // f) Emit UploadState.Success
                _uploadState.value = UploadState.Success("Payment proof submitted successfully!")
                
            } catch (e: Exception) {
                // g) Catch any exceptions and emit UploadState.Error
                _uploadState.value = UploadState.Error(e.message ?: "An unknown error occurred during upload.")
            }
        }
    }
}
