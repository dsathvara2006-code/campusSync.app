package com.campussync.app.feature.auth

import com.campussync.app.core.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class NewUser(val email: String, val name: String) : AuthState()
    data class Pending(val user: User) : AuthState()
    data class Rejected(val reason: String?) : AuthState()
    data class Approved(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun loginWithGoogle(idToken: String): AuthState {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return AuthState.Error("Google Sign-In failed")
            val uid = firebaseUser.uid
            val email = firebaseUser.email?.lowercase()
                ?: return AuthState.Error("Email not provided by Google")
            val name = firebaseUser.displayName ?: "User"

            val existingUserSnapshot = firestore.collection("users").document(uid).get().await()
            if (existingUserSnapshot.exists()) {
                val userResult = loadExistingUser(uid, existingUserSnapshot)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()!!
                    val rejectionReason = existingUserSnapshot.getString("rejectionReason")
                    return when (user.status) {
                        "pending" -> AuthState.Pending(user)
                        "rejected" -> AuthState.Rejected(rejectionReason)
                        else -> AuthState.Approved(user)
                    }
                } else {
                    return AuthState.Error(userResult.exceptionOrNull()?.message ?: "Unknown error loading user")
                }
            }

            // User does not exist in the users collection
            // 1. Check if they were personally invited via the new 'invites' system
            val unifiedInvite = firestore.collection("invites").document(email).get().await()
            if (unifiedInvite.exists() && unifiedInvite.getString("status") == "pending") {
                val collegeId = unifiedInvite.getString("collegeId") ?: ""
                val role = unifiedInvite.getString("role")?.lowercase() ?: "student"
                val classId = unifiedInvite.getString("classId") ?: ""
                val rollNo = unifiedInvite.getString("rollNo") ?: ""
                
                if (collegeId.isNotEmpty()) {
                    val batch = firestore.batch()
                    val userRootRef = firestore.collection("users").document(uid)
                    
                    batch.set(userRootRef, hashMapOf(
                        "email" to email,
                        "role" to role,
                        "collegeId" to collegeId,
                        "classId" to classId,
                        "status" to "approved" // Pre-approved because personally invited
                    ))
                    
                    val userProfile = User(
                        userId = uid,
                        collegeId = collegeId,
                        name = name,
                        email = email,
                        role = role,
                        rollNo = rollNo,
                        classId = classId,
                        status = "approved"
                    )
                    
                    val subCollection = when (role) {
                        "principal" -> "principals"
                        "admin" -> "admins"
                        "student" -> "students"
                        else -> "teachers"
                    }
                    
                    val profileRef = firestore.collection("colleges")
                        .document(collegeId)
                        .collection(subCollection)
                        .document(uid)
                        
                    batch.set(profileRef, userProfile)
                    
                    // Mark the invite as claimed
                    batch.update(unifiedInvite.reference, "status", "claimed")
                    
                    batch.commit().await()
                    return AuthState.Approved(userProfile)
                }
            }

            // 2. Fallback for old 'admin_invites' system (just in case)
            val inviteSnapshot = firestore.collection("admin_invites").document(email).get().await()
            if (inviteSnapshot.exists()) {
                val collegeId = inviteSnapshot.getString("collegeId") ?: ""
                if (collegeId.isNotEmpty()) {
                    val batch = firestore.batch()
                    val userRootRef = firestore.collection("users").document(uid)
                    
                    batch.set(userRootRef, hashMapOf(
                        "email" to email,
                        "role" to "admin",
                        "collegeId" to collegeId,
                        "status" to "approved"
                    ))
                    
                    val adminProfile = User(
                        userId = uid,
                        collegeId = collegeId,
                        name = name,
                        email = email,
                        role = "admin",
                        status = "approved"
                    )
                    
                    val profileRef = firestore.collection("colleges")
                        .document(collegeId)
                        .collection("admins")
                        .document(uid)
                        
                    batch.set(profileRef, adminProfile)
                    
                    // Delete the invite so it can't be reused
                    batch.delete(inviteSnapshot.reference)
                    
                    batch.commit().await()
                    return AuthState.Approved(adminProfile)
                }
            }

            // If no invite found, route them to the New User form
            AuthState.NewUser(email = email, name = name)

        } catch (e: Exception) {
            auth.signOut()
            AuthState.Error(e.message ?: "Authentication failed")
        }
    }

    suspend fun requestAccess(
        email: String,
        name: String,
        role: String,
        classId: String,
        rollNo: String,
        inviteCode: String
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

            // Check Before Create: Prevent duplicate requests
            val existingDoc = firestore.collection("users").document(uid).get().await()
            if (existingDoc.exists()) {
                val existingStatus = existingDoc.getString("status")
                if (existingStatus == "approved") {
                    return Result.failure(Exception("You are already registered! Please restart the app to log in."))
                } else if (existingStatus == "pending") {
                    return Result.failure(Exception("Your request is already pending."))
                }
            }

            // 1. Validate Invite Code
            val codeToUse = inviteCode.trim()
            if (codeToUse.isBlank()) {
                return Result.failure(Exception("Please enter a valid Invite Code."))
            }
            val inviteDoc = firestore.collection("invite_codes").document(codeToUse).get().await()
            if (!inviteDoc.exists()) {
                return Result.failure(Exception("Invalid Invite Code. Please check and try again."))
            }
            val collegeId = inviteDoc.getString("collegeId") 
                ?: return Result.failure(Exception("Invite code is corrupted."))

            val userRootRef = firestore.collection("users").document(uid)
            val batch = firestore.batch()

            batch.set(userRootRef, hashMapOf(
                "email" to email,
                "role" to role.lowercase(),
                "collegeId" to collegeId,
                "classId" to classId,
                "status" to "pending"
            ))

            val userProfile = User(
                userId = uid,
                collegeId = collegeId,
                name = name,
                email = email,
                role = role.lowercase(),
                rollNo = rollNo,
                classId = classId,
                status = "pending"
            )

            val subCollection = when (role.lowercase()) {
                "principal" -> "principals"
                "admin" -> "admins"
                "student" -> "students"
                else -> "teachers"
            }
            val profileRef = firestore.collection("colleges")
                .document(collegeId)
                .collection(subCollection)
                .document(uid)
            batch.set(profileRef, userProfile)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToUserStatus(uid: String): Flow<AuthState> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "approved"
                    val reason = snapshot.getString("rejectionReason")
                    val state = when (status) {
                        "pending" -> AuthState.Pending(User(userId = uid, status = "pending"))
                        "rejected" -> AuthState.Rejected(reason)
                        else -> AuthState.Approved(User(userId = uid, status = "approved"))
                    }
                    trySend(state)
                }
            }
        awaitClose { listener.remove() }
    }

    private suspend fun loadExistingUser(
        uid: String,
        userSnapshot: com.google.firebase.firestore.DocumentSnapshot
    ): Result<User> {
        val role = userSnapshot.getString("role")
            ?: return Result.failure(Exception("Invalid user profile: Missing role"))
        val collegeId = userSnapshot.getString("collegeId")
            ?: return Result.failure(Exception("Invalid user profile: Missing collegeId"))
        
        val fallbackClassId = userSnapshot.getString("classId") ?: ""
        val status = userSnapshot.getString("status") ?: "approved" // default for legacy

        val subCollection = when (role.lowercase()) {
            "principal" -> "principals"
            "admin" -> "admins"
            "student" -> "students"
            else -> "teachers"
        }

        val profileDoc = firestore.collection("colleges")
            .document(collegeId)
            .collection(subCollection)
            .document(uid)
            .get().await()

        val user = profileDoc.toObject(User::class.java)
        
        // Security Check: Block Deactivated Users
        if (user != null && !user.isActive) {
            auth.signOut()
            return Result.failure(Exception("Your account has been deactivated by the Administration."))
        }
        
        val email = userSnapshot.getString("email") ?: ""
        
        return Result.success(
            user?.copy(email = email, status = status) ?: User(userId = uid, collegeId = collegeId, classId = fallbackClassId, name = "User", role = role, email = email, status = status)
        )
    }

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = auth.currentUser ?: return Result.success(null)
            val uid = firebaseUser.uid
            val userRootSnapshot = firestore.collection("users").document(uid).get().await()

            if (!userRootSnapshot.exists()) {
                auth.signOut()
                return Result.success(null)
            }

            val result = loadExistingUser(uid, userRootSnapshot)
            if (result.isFailure) {
                auth.signOut()
                return Result.success(null)
            }

            Result.success(result.getOrNull())
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    fun logout() = auth.signOut()
}
