package com.campussync.app.feature.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.R
import com.campussync.app.core.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Login — a single glass card floating over a deep indigo gradient with
 * soft ambient light blobs. Trust cues (role badges + a security line)
 * replace the old plain footer text to feel more "product", less "form".
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String) -> Unit
) {
    val context = LocalContext.current
    val state = viewModel.authState.value

    LaunchedEffect(state) {
        if (state is AuthState.Success) onLoginSuccess(state.user.role)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken -> viewModel.loginWithGoogle(idToken) }
                ?: Log.e("Auth", "Google Sign-In failed: ID Token is null")
        } catch (e: ApiException) {
            Log.e("Auth", "Google Sign-In failed", e)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Indigo900, Night950, Indigo900))),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow blobs
        Box(Modifier.size(320.dp).offset(x = (-120).dp, y = (-260).dp).clip(CircleShape).background(Violet500.copy(alpha = 0.20f)).blur(80.dp))
        Box(Modifier.size(280.dp).offset(x = 130.dp, y = 300.dp).clip(CircleShape).background(Cyan500.copy(alpha = 0.14f)).blur(80.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Brand mark
            Box(
                modifier = Modifier.size(72.dp).shadow(16.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Indigo400, Violet500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.School, contentDescription = "CampusSync", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("CampusSync", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(4.dp))
            Text("Multi-College Management Platform", fontSize = 13.sp, color = Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(36.dp))

            // ---- Glass Card ----
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome back", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Principals, Teachers & Students all sign in\nwith their Google account.",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center, lineHeight = 19.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail().build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                launcher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Slate900),
                        enabled = state !is AuthState.Loading
                    ) {
                        if (state is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Indigo500, strokeWidth = 2.5.dp)
                        } else {
                            Text("Continue with Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(
                        visible = state is AuthState.Error,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (state is AuthState.Error) {
                            Column {
                                Spacer(Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Rose500.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = state.message, color = Color(0xFFFCA5A5), fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Role chips — quick trust cue for who this app serves
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Principal", "Teacher", "Student").forEach { role ->
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Text(
                            role, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Secured with Role-Based Access & Data Isolation",
                    color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}
