package com.campussync.app.feature.fees

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campussync.app.core.components.GlassCard
import com.campussync.app.core.components.GlassEmptyState
import com.campussync.app.core.components.GlassPill
import com.campussync.app.core.components.GlassScreen
import com.campussync.app.core.components.glassHairline
import com.campussync.app.core.model.FeeType
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.Primary
import com.campussync.app.core.theme.Error
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun FeeTypeScreen(
    currentUser: User,
    onBack: () -> Unit
) {
    val viewModel: FeeTypeViewModel = viewModel()
    val feeTypes by viewModel.feeTypes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(currentUser.collegeId) {
        if (currentUser.collegeId.isNotBlank()) {
            viewModel.loadFeeTypes(currentUser.collegeId)
        }
    }

    GlassScreen(
        title = "Fee Templates",
        subtitle = "${feeTypes.size} template${if (feeTypes.size == 1) "" else "s"} • used when assigning dues",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Primary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Fee Type")
            }
        }
    ) { _ ->
        if (isLoading && feeTypes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (feeTypes.isEmpty()) {
            GlassEmptyState(
                icon = Icons.Rounded.ReceiptLong,
                title = "No fee templates yet",
                subtitle = "Tap + to create your first template (e.g. Hostel Fee ₹12,000).",
                accent = Primary
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(feeTypes) { feeType ->
                    FeeTypeItem(feeType = feeType, onDelete = {
                        viewModel.deleteFeeType(currentUser.collegeId, feeType.id) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "Template Deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateFeeTypeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, amount ->
                if (currentUser.collegeId.isBlank()) {
                    Toast.makeText(context, "College ID missing! Please update Settings.", Toast.LENGTH_LONG).show()
                    return@CreateFeeTypeDialog
                }
                viewModel.createFeeType(currentUser.collegeId, title, desc, amount) { success, msg ->
                    if (success) {
                        showCreateDialog = false
                        Toast.makeText(context, "Template Created!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            },
            isLoading = isLoading,
            context = context
        )
    }
}

@Composable
fun FeeTypeItem(feeType: FeeType, onDelete: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feeType.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = feeType.description.ifBlank { "No description" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${feeType.amount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Primary
                )
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Template",
                        tint = Error,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateFeeTypeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Double) -> Unit,
    isLoading: Boolean,
    context: android.content.Context
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        title = { Text("Create Fee Template", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Fee Title (e.g., Hostel Fee)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (title.isBlank()) {
                        Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (parsedAmount <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    } else {
                        onCreate(title, description, parsedAmount)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isLoading) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
