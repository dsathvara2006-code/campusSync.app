package com.campussync.app.feature.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campussync.app.core.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    val authState = mutableStateOf<AuthState>(AuthState.Idle)
    var currentUser = mutableStateOf<User?>(null)
        private set
    var isCheckingAuth = mutableStateOf(true)
        private set

    private val _userStatus = MutableStateFlow<AuthState?>(null)
    val userStatus: StateFlow<AuthState?> = _userStatus.asStateFlow()

    init {
        checkAutoLogin()
    }

    fun checkAutoLogin(onDone: ((User?) -> Unit)? = null) {
        isCheckingAuth.value = true
        viewModelScope.launch {
            val result = repository.getCurrentUser()
            val user = result.getOrNull()
            currentUser.value = user
            if (user != null) {
                val state = when (user.status) {
                    "pending" -> AuthState.Pending(user)
                    "rejected" -> AuthState.Rejected("Your access request was rejected.")
                    else -> AuthState.Approved(user)
                }
                authState.value = state
            } else {
                authState.value = AuthState.Idle
            }
            isCheckingAuth.value = false
            onDone?.invoke(user)
        }
    }



    fun loginWithGoogle(idToken: String) {
        authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.loginWithGoogle(idToken)
            authState.value = result
            if (result is AuthState.Approved) {
                currentUser.value = result.user
            } else if (result is AuthState.Pending) {
                currentUser.value = result.user
            }
        }
    }

    fun requestAccess(
        email: String,
        name: String,
        role: String,
        classId: String,
        rollNo: String,
        inviteCode: String
    ) {
        authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.requestAccess(email, name, role, classId, rollNo, inviteCode)
            if (result.isSuccess) {
                val tempUser = User(
                    email = email,
                    name = name,
                    role = role,
                    classId = classId,
                    rollNo = rollNo,
                    collegeId = "", // Filled backend side based on invite code
                    status = "pending"
                )
                currentUser.value = tempUser
                authState.value = AuthState.Pending(tempUser)
            } else {
                authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Failed to request access")
            }
        }
    }

    fun startListeningToStatus(uid: String) {
        viewModelScope.launch {
            repository.listenToUserStatus(uid).collect { state ->
                _userStatus.value = state
                if (state is AuthState.Approved || state is AuthState.Rejected) {
                    authState.value = state
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        repository.logout()
        currentUser.value = null
        authState.value = AuthState.Idle
        onLoggedOut()
    }
}
