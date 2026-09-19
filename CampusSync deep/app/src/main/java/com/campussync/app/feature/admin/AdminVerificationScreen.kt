package com.campussync.app.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campussync.app.core.components.GlassCard
import com.campussync.app.core.components.GlassChip
import com.campussync.app.core.components.GlassEmptyState
import com.campussync.app.core.components.GlassMonogram
import com.campussync.app.core.components.GlassPill
import com.campussync.app.core.components.GlassScreen
import com.campussync.app.core.components.glassHairline
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.Error
import com.campussync.app.core.theme.Info
import com.campussync.app.core.theme.Primary
import com.campussync.app.core.theme.Success
import com.campussync.app.core.theme.Warning
import androidx.compose.material3.*

@Composable
fun AdminVerificationScreen(
    currentUser: User,
    onBack: () -> Unit,
    viewModel: AdminVerificationViewModel = viewModel()
) {
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var userToReject by remember { mutableStateOf<User?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(currentUser.collegeId) {
        viewModel.loadPendingUsers(currentUser.collegeId)
    }

    GlassScreen(
        title = "Verify Requests",
        subtitle = "${pendingUsers.size} access request${if (pendingUsers.size == 1) "" else "s"} waiting",
        onBack = onBack
    ) { _ ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (pendingUsers.isEmpty()) {
            GlassEmptyState(
                icon = Icons.Rounded.HowToReg,
                title = "All clear!",
                subtitle = "No pending access requests right now.",
                accent = Success
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingUsers) { user ->
                    PendingUserCard(
                        user = user,
                        onApprove = {
                            viewModel.approveUser(user.userId, user.role, currentUser.collegeId) { _, _ -> }
                        },
                        onReject = {
                            userToReject = user
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (userToReject != null) {
        AlertDialog(
            onDismissRequest = {
                userToReject = null
                rejectReason = ""
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            title = { Text("Reject Request", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Please provide a reason for rejecting ${userToReject?.name ?: "this user"}.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectUser(userToReject!!.userId, userToReject!!.role, currentUser.collegeId, rejectReason) { _, _ -> }
                        userToReject = null
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Reject", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    userToReject = null
                    rejectReason = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PendingUserCard(user: User, onApprove: () -> Unit, onReject: () -> Unit) {
    val roleAccent = when (user.role.lowercase()) {
        "teacher" -> Info
        "admin", "tenant_admin" -> Warning
        "principal" -> Primary
        else -> Success
    }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassMonogram(name = user.name.ifBlank { "U" }, accent = roleAccent)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name.ifBlank { "Unknown Name" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    GlassChip(
                        text = user.role.replaceFirstChar { it.uppercase() },
                        color = roleAccent
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Mail, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Badge, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Class: ${user.classId.ifBlank { "N/A" }}  •  Roll: ${user.rollNo.ifBlank { "N/A" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        androidx.compose.material3.HorizontalDivider(color = glassHairline())
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPill(text = "Reject", color = Error, filled = false, onClick = onReject)
            Spacer(Modifier.width(8.dp))
            GlassPill(
                text = "Approve",
                color = Success,
                filled = true,
                leadingIcon = Icons.Rounded.Check,
                onClick = onApprove
            )
        }
    }
}
