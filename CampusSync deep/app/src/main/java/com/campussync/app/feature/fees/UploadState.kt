package com.campussync.app.feature.fees

/**
 * Sealed class for strict UI State Management of Payment Proof Uploads.
 */
sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Success(val message: String) : UploadState()
    data class Error(val errorMsg: String) : UploadState()
}
