package com.campussync.app.feature.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.FeeType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FeeTypeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _feeTypes = MutableStateFlow<List<FeeType>>(emptyList())
    val feeTypes: StateFlow<List<FeeType>> = _feeTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadFeeTypes(collegeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("colleges").document(collegeId)
                    .collection("fee_types")
                    .get().await()
                
                val types = snapshot.documents.mapNotNull { it.toObject(FeeType::class.java) }
                _feeTypes.value = types.sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createFeeType(collegeId: String, title: String, description: String, amount: Double, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val feeId = UUID.randomUUID().toString()
                val feeType = FeeType(
                    id = feeId,
                    collegeId = collegeId,
                    title = title,
                    description = description,
                    amount = amount,
                    createdAt = System.currentTimeMillis()
                )
                
                db.collection("colleges").document(collegeId)
                    .collection("fee_types").document(feeId)
                    .set(feeType).await()
                
                loadFeeTypes(collegeId) // Reload the list
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFeeType(collegeId: String, feeId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("colleges").document(collegeId)
                    .collection("fee_types").document(feeId)
                    .delete().await()
                
                loadFeeTypes(collegeId) // Reload the list
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
