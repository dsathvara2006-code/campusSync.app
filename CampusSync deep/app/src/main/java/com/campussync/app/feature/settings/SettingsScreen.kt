package com.campussync.app.feature.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.components.*
import com.campussync.app.core.theme.*
import com.campussync.app.core.theme.ThemeManager
@Composable
fun SettingsScreen(
    currentUser: com.campussync.app.core.model.User?,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {        // User profile passed via navigation
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showFeeSetupDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val feeSettings by viewModel.feeSettings.collectAsState()
    
    LaunchedEffect(currentUser?.collegeId) {
        if (currentUser?.collegeId != null && (currentUser.role == "admin" || currentUser.role == "principal")) {
            viewModel.loadFeeSettings(currentUser.collegeId)
            viewModel.loadInviteCode(currentUser.collegeId)
        }
    }

    val userEmail = currentUser?.email?.ifBlank { "No email" } ?: "No email"

    val gradientBg = Brush.verticalGradient(
        colors = listOf(PrimaryDark, Primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background),
        startY = 0f, endY = 500f
    )

    val currentThemeMode by ThemeManager.preferences.themeMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceVariant),
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Settings & Profile",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            // Settings Container Card
            CustomCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                elevation = 4.dp
            ) {
                // Section: User Profile Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.name ?: "Campus User",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = userEmail,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = currentUser?.role?.replaceFirstChar { it.uppercase() } ?: "User",
                                        color = Primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (!currentUser?.classId.isNullOrEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${currentUser?.classId} | Roll No: ${currentUser?.rollNo}",
                                        fontSize = 11.sp,
                                        color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                // Group: Admin Controls
                if (currentUser?.role == "admin" || currentUser?.role == "principal") {
                    Text(
                        text = "Admin Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val currentInviteCode by viewModel.inviteCode.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    SettingClickRow(
                        icon = Icons.Rounded.Person,
                        title = "College Invite Code",
                        subtitle = currentInviteCode ?: "Tap to Generate",
                        onClick = {
                            if (currentInviteCode != null) {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Join our college on CampusSync!\n\nUse this Invite Code: $currentInviteCode")
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Invite Code"))
                            } else {
                                viewModel.generateInviteCode(currentUser.collegeId) { success, code ->
                                    if(success) {
                                        android.widget.Toast.makeText(context, "Code Generated: $code", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Error generating code", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    SettingClickRow(
                        icon = Icons.Rounded.Settings,
                        title = "College Fee Setup",
                        subtitle = "Configure Zero-Commission UPI",
                        onClick = { showFeeSetupDialog = true }
                    )

                    Spacer(Modifier.height(24.dp))
                }

                // Group: App Preferences
                Text(
                    text = "App Preferences",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                SettingClickRow(
                    icon = Icons.Rounded.Settings,
                    title = "Theme Mode",
                    subtitle = when (currentThemeMode) {
                        "dark" -> "Dark Mode"
                        "light" -> "Light Mode"
                        else -> "System Default"
                    },
                    onClick = { showThemeDialog = true }
                )

                Spacer(Modifier.height(24.dp))

                // Group: Account
                Text(
                    text = "Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                SettingClickRow(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    title = "Log Out",
                    subtitle = "Sign out from this device",
                    onClick = { showLogoutDialog = true }
                )

                Spacer(Modifier.height(24.dp))

                // Group: Legal & Support
                Text(
                    text = "Legal & Support",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                SettingClickRow(
                    icon = Icons.Rounded.Info,
                    title = "Help Center",
                    subtitle = "FAQs and Guides",
                    onClick = { onNavigate("help_center") }
                )
                
                Spacer(Modifier.height(8.dp))
                
                SettingClickRow(
                    icon = Icons.Rounded.Person,
                    title = "Contact Support",
                    subtitle = "Get help from our team",
                    onClick = { onNavigate("support") }
                )
                
                Spacer(Modifier.height(8.dp))
                
                SettingClickRow(
                    icon = Icons.Rounded.Lock,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = { onNavigate("privacy_policy") }
                )
                
                Spacer(Modifier.height(8.dp))
                
                SettingClickRow(
                    icon = Icons.Rounded.Info,
                    title = "Terms of Service",
                    subtitle = "App usage terms",
                    onClick = { onNavigate("terms_of_service") }
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "CampusSync Modern Native v2.1.0",
                    fontSize = 11.sp,
                    color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showLogoutDialog) {
        com.campussync.app.core.components.PremiumWarningDialog(
            title = "Log Out?",
            message = "You will need to sign in again with your Google account to access your campus dashboard.",
            confirmText = "Log Out",
            cancelText = "Cancel",
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onCancel = { showLogoutDialog = false }
        )
    }

    // Theme Mode Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeOptionRow("Light Mode", currentThemeMode == "light") {
                        ThemeManager.preferences.setThemeMode("light")
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Dark Mode", currentThemeMode == "dark") {
                        ThemeManager.preferences.setThemeMode("dark")
                        showThemeDialog = false
                    }
                    ThemeOptionRow("System Default", currentThemeMode == "system") {
                        ThemeManager.preferences.setThemeMode("system")
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = Primary)
                }
            }
        )
    }

    // Fee Setup Dialog
    if (showFeeSetupDialog && currentUser != null) {
        var upiId by remember { mutableStateOf(feeSettings?.upiId ?: "") }
        var collegeName by remember { mutableStateOf(feeSettings?.collegeName ?: "") }
        var upiError by remember { mutableStateOf<String?>(null) }
        val isLoading by viewModel.isLoading.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = { showFeeSetupDialog = false },
            title = { Text("College Fee Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Enable Zero-Commission payments directly to your bank.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = collegeName,
                        onValueChange = { collegeName = it },
                        label = { Text("College Name (Visible to students)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { 
                            upiId = it
                            upiError = null
                        },
                        label = { Text("Official UPI ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = upiError != null,
                        supportingText = if (upiError != null) { { Text(upiError!!, color = MaterialTheme.colorScheme.error) } } else null
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val upiRegex = "^[a-zA-Z0-9.\\\\-_]{2,256}@[a-zA-Z]{2,64}$".toRegex()
                        if (collegeName.isBlank()) {
                            upiError = "College Name is required"
                            return@Button
                        }
                        if (!upiRegex.matches(upiId)) {
                            upiError = "Invalid UPI ID format"
                            return@Button
                        }
                        viewModel.saveFeeSettings(currentUser.collegeId, collegeName, upiId) { success, msg ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Settings Saved!", android.widget.Toast.LENGTH_SHORT).show()
                                showFeeSetupDialog = false
                            } else {
                                upiError = msg ?: "Failed to save"
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (isLoading) "Saving..." else "Save Setup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeeSetupDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary
                )
            )
        }
    }
}

@Composable
private fun SettingClickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Primary)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,            color = MaterialTheme.colorScheme.onBackground
        )
    }
}




