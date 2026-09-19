package com.campussync.app.feature.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

// Dark Theme Colors
private val SurfaceDark = Color(0xFF0F0F13)
private val GlassWhite = Color.White.copy(alpha = 0.05f)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.6f)
private val AccentColor = Color(0xFF6366F1) // Indigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToWaitingRoom: () -> Unit,
    onLoginSuccess: (role: String) -> Unit
) {
    val context = LocalContext.current
    val state = viewModel.authState.value
    
    // Request Access Form State
    var selectedRole by remember { mutableStateOf("Student") }
    var department by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    // Course and Semester State
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var semesterDropdownExpanded by remember { mutableStateOf(false) }
    
    val courseOptions = listOf("BCA", "BBA", "BCom", "BSc", "BTech", "MCA", "MBA", "MSc", "BA", "MA")
    val semesterOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    val classId = if (selectedRole == "Student") {
        if (course.isNotBlank() && semester.isNotBlank()) "$course-$semester" else ""
    } else {
        department
    }

    // Error state for form validation when user tries to request access but form is empty
    var formError by remember { mutableStateOf<String?>(null) }

    // UI Mode State
    var isSignupMode by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (state) {
            is AuthState.Approved -> onLoginSuccess(state.user.role)
            is AuthState.Pending -> onNavigateToWaitingRoom()
            is AuthState.NewUser -> {
                isSignupMode = true // Force signup mode if they are new
                if (inviteCode.isNotBlank() && classId.isNotBlank() && rollNo.isNotBlank()) {
                    // Form is filled! Send request immediately.
                    formError = null
                    viewModel.requestAccess(state.email, state.name, selectedRole, classId, rollNo, inviteCode)
                } else {
                    // Form is NOT filled. Show error if they tried to auto-submit, else just let them fill it.
                    if (formError != null || inviteCode.isNotBlank()) {
                        formError = "Please fill out all fields to complete your request."
                    }
                }
            }
            else -> {}
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken -> viewModel.loginWithGoogle(idToken) }
        } catch (e: ApiException) {
            Log.e("Auth", "Google Sign-In failed", e)
        }
    }

    val launchGoogleAuth = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail().build()
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut().addOnCompleteListener {
            launcher.launch(client.signInIntent)
        }
    }

    if (state is AuthState.Rejected) {
        RejectedScreen(
            reason = state.reason ?: "Your access request was rejected by the administration.",
            onResubmit = {
                viewModel.logout()
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        // Soft Glow
        Box(Modifier.size(300.dp).align(Alignment.TopStart).offset(x = (-100).dp, y = (-100).dp).clip(CircleShape).background(AccentColor.copy(alpha = 0.15f)).blur(100.dp))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Text("CampusSync", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, letterSpacing = (-1.5).sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("The modern campus OS.", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (isSignupMode) {
                // --- NEW USER FORM ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (state is AuthState.NewUser) "Complete Your Request" else "Request Access",
                        color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (state is AuthState.NewUser) "Logged in as ${state.email}" else "Fill this out before signing in.",
                        color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Role Selector
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Student", "Teacher").forEach { role ->
                            val isSelected = selectedRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) AccentColor else GlassWhite)
                                    .clickable { selectedRole = role },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = role,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Inputs
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Invite Code (Ask your Admin)", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = GlassWhite,
                            focusedContainerColor = GlassWhite,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val isStudent = selectedRole == "Student"

                    if (isStudent) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Course Dropdown
                            ExposedDropdownMenuBox(
                                expanded = courseDropdownExpanded,
                                onExpandedChange = { courseDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = course,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Course", color = TextSecondary, fontSize = 13.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = GlassWhite,
                                        focusedContainerColor = GlassWhite,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = courseDropdownExpanded,
                                    onDismissRequest = { courseDropdownExpanded = false }
                                ) {
                                    courseOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = TextPrimary) },
                                            onClick = {
                                                course = option
                                                courseDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Semester Dropdown
                            ExposedDropdownMenuBox(
                                expanded = semesterDropdownExpanded,
                                onExpandedChange = { semesterDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = semester,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sem", color = TextSecondary, fontSize = 13.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = GlassWhite,
                                        focusedContainerColor = GlassWhite,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = semesterDropdownExpanded,
                                    onDismissRequest = { semesterDropdownExpanded = false }
                                ) {
                                    semesterOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, color = TextPrimary) },
                                            onClick = {
                                                semester = option
                                                semesterDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department (e.g. CS Dept)", color = TextSecondary, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = GlassWhite,
                                focusedContainerColor = GlassWhite,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = rollNo,
                        onValueChange = { rollNo = it },
                        label = { Text(if (isStudent) "Roll Number" else "Employee ID (Optional)", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = GlassWhite,
                            focusedContainerColor = GlassWhite,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Form Errors
                if (formError != null) {
                    Text(formError!!, color = Color(0xFFF87171), fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp), textAlign = TextAlign.Center)
                }

                // Submit Form Button
                Button(
                    onClick = { 
                        formError = null
                        if (inviteCode.isBlank() || classId.isBlank() || rollNo.isBlank()) {
                            formError = "Please fill out all fields before submitting."
                            return@Button
                        }
                        if (state is AuthState.NewUser) {
                            viewModel.requestAccess(state.email, state.name, selectedRole, classId, rollNo, inviteCode)
                        } else {
                            launchGoogleAuth()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = Color.White)
                ) {
                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (state is AuthState.NewUser) "Submit Request" else "Securely Submit via Google", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                if (state !is AuthState.NewUser) {
                    Text(
                        "Back to Login",
                        color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isSignupMode = false }.padding(8.dp)
                    )
                }
            } else {
                // --- DEFAULT LOGIN VIEW ---
                Spacer(modifier = Modifier.height(32.dp))
                
                if (state is AuthState.Error) {
                    Text(state.message, color = Color(0xFFF87171), fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp), textAlign = TextAlign.Center)
                }

                Button(
                    onClick = { 
                        formError = null
                        launchGoogleAuth() 
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = SurfaceDark)
                ) {
                    if (state is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SurfaceDark, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In with Google", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        "Request Access",
                        color = AccentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isSignupMode = true }.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaitingRoomScreen(
    viewModel: AuthViewModel,
    onApproved: () -> Unit
) {
    val userStatus by viewModel.userStatus.collectAsState()
    val currentUser by viewModel.currentUser
    
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(currentUser) {
        val uid = currentUser?.userId
        if (uid != null) {
            viewModel.startListeningToStatus(uid)
        }
    }

    LaunchedEffect(userStatus) {
        if (userStatus is AuthState.Approved) {
            onApproved()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(GlassWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text("You're in line.", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Waiting for an admin to approve your request. This screen will automatically update.",
                fontSize = 16.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun RejectedScreen(
    reason: String,
    onResubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF87171).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text("Access Rejected", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                reason,
                fontSize = 16.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onResubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = SurfaceDark)
            ) {
                Text("Logout & Resubmit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
