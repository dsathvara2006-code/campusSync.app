package com.campussync.app.feature.fees

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.FeeDue
import com.campussync.app.core.model.FeePayment
import com.campussync.app.core.model.FeeSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudentFeeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _feeDues = MutableStateFlow<List<FeeDue>>(emptyList())
    val feeDues: StateFlow<List<FeeDue>> = _feeDues.asStateFlow()
    
    private val _feeSettings = MutableStateFlow<FeeSettings?>(null)
    val feeSettings: StateFlow<FeeSettings?> = _feeSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStudentFees(collegeId: String, studentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load Fee Settings for College UPI
                val settingsDoc = db.collection("colleges").document(collegeId)
                    .collection("settings").document("fee_settings").get().await()
                if (settingsDoc.exists()) {
                    _feeSettings.value = settingsDoc.toObject(FeeSettings::class.java)
                }

                // Load dues assigned to this student
                val duesSnapshot = db.collection("colleges").document(collegeId)
                    .collection("fee_dues")
                    .whereEqualTo("studentId", studentId)
                    .get().await()
                
                _feeDues.value = duesSnapshot.documents.mapNotNull { it.toObject(FeeDue::class.java) }
                    .sortedByDescending { it.assignedAt }
                    
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitPaymentProof(
        collegeId: String,
        studentId: String,
        studentName: String,
        feeDue: FeeDue,
        utrNumber: String,
        proofUri: Uri,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val paymentId = UUID.randomUUID().toString()

                // === STEP 1: Upload image to Firebase Storage ===
                android.util.Log.d("FeeUpload", "[S1] URI = $proofUri")
                val storageRef = storage.reference.child("fee_proofs/$collegeId/$studentId/$paymentId.jpg")
                val uploadTask = try {
                    storageRef.putFile(proofUri).await()
                } catch (storageEx: com.google.firebase.storage.StorageException) {
                    val code = storageEx.errorCode
                    android.util.Log.e("FeeUpload", "[S1 FAIL] code=$code msg=${storageEx.message}")
                    onComplete(false, "[Upload Failed - Code $code]: ${storageEx.message}")
                    return@launch
                }
                android.util.Log.d("FeeUpload", "[S1 OK] bytes=${uploadTask.bytesTransferred}")

                // === STEP 2: Get download URL ===
                val downloadUrl = try {
                    storageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    android.util.Log.e("FeeUpload", "[S2 FAIL] ${e.message}")
                    onComplete(false, "[URL Fetch Failed]: ${e.message}")
                    return@launch
                }
                android.util.Log.d("FeeUpload", "[S2 OK] url=$downloadUrl")

                // === STEP 3: Firestore references ===
                val paymentRef = db.collection("colleges").document(collegeId)
                    .collection("fee_payments").document(paymentId)
                val dueRef = db.collection("colleges").document(collegeId)
                    .collection("fee_dues").document(feeDue.id)
                val utrRef = db.collection("colleges").document(collegeId)
                    .collection("used_utrs").document(utrNumber)

                val payment = FeePayment(
                    id = paymentId,
                    dueId = feeDue.id,
                    studentId = studentId,
                    studentName = studentName,
                    collegeId = collegeId,
                    feeTitle = feeDue.title,
                    amount = feeDue.amount,
                    utr = utrNumber,
                    proofUrl = downloadUrl,
                    status = "submitted",
                    timestamp = System.currentTimeMillis()
                )

                // === STEP 4: Atomic Firestore transaction ===
                android.util.Log.d("FeeUpload", "[S3] Starting Firestore transaction...")
                try {
                    db.runTransaction { tx ->
                        if (tx.get(utrRef).exists()) {
                            throw FirebaseFirestoreException(
                                "This UTR is already used!",
                                FirebaseFirestoreException.Code.ABORTED
                            )
                        }
                        val utrData = hashMapOf(
                            "usedBy" to studentId,
                            "paymentId" to paymentId,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        tx.set(utrRef, utrData)
                        tx.set(paymentRef, payment)
                        tx.update(dueRef, "status", "submitted")
                        null
                    }.await()
                    android.util.Log.d("FeeUpload", "[S3 OK] Transaction complete!")
                } catch (e: FirebaseFirestoreException) {
                    android.util.Log.e("FeeUpload", "[S3 FAIL] ${e.message}")
                    onComplete(false, "[Firestore Error]: " + if (e.message?.contains("already used") == true)
                        "This UTR is already used! Duplicate submissions are strictly blocked."
                    else e.message)
                    return@launch
                }

                loadStudentFees(collegeId, studentId)
                onComplete(true, null)
            } catch (e: Exception) {
                android.util.Log.e("FeeUpload", "[OUTER CATCH] ${e.javaClass.simpleName}: ${e.message}")
                onComplete(false, "[${e.javaClass.simpleName}]: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
