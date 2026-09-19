package com.campussync.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.FeeSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _feeSettings = MutableStateFlow<FeeSettings?>(null)
    val feeSettings: StateFlow<FeeSettings?> = _feeSettings.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadFeeSettings(collegeId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("colleges").document(collegeId)
                    .collection("settings").document("fee_settings")
                    .get().await()
                
                if (doc.exists()) {
                    _feeSettings.value = doc.toObject(FeeSettings::class.java)
                } else {
                    _feeSettings.value = FeeSettings(collegeId = collegeId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveFeeSettings(collegeId: String, collegeName: String, upiId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val settings = FeeSettings(
                    collegeId = collegeId,
                    upiId = upiId,
                    collegeName = collegeName,
                    lastUpdated = System.currentTimeMillis()
                )
                db.collection("colleges").document(collegeId)
                    .collection("settings").document("fee_settings")
                    .set(settings).await()
                
                _feeSettings.value = settings
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadInviteCode(collegeId: String) {
        viewModelScope.launch {
            try {
                val query = db.collection("invite_codes")
                    .whereEqualTo("collegeId", collegeId)
                    .limit(1)
                    .get().await()
                if (!query.isEmpty) {
                    _inviteCode.value = query.documents[0].id
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateInviteCode(collegeId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
                var newCode = ""
                var isUnique = false
                
                // Keep generating until we find a code that doesn't exist yet
                while (!isUnique) {
                    val part1 = (1..4).map { kotlin.random.Random.nextInt(0, charPool.size) }.map(charPool::get).joinToString("")
                    val part2 = (1..4).map { kotlin.random.Random.nextInt(0, charPool.size) }.map(charPool::get).joinToString("")
                    newCode = "$part1-$part2"
                        
                    val existingDoc = db.collection("invite_codes").document(newCode).get().await()
                    if (!existingDoc.exists()) {
                        isUnique = true
                    }
                }

                val inviteData = hashMapOf(
                    "collegeId" to collegeId,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("invite_codes").document(newCode).set(inviteData).await()
                
                _inviteCode.value = newCode
                onComplete(true, newCode)
            } catch (e: Exception) {
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
