package com.campussync.app.feature.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campussync.app.core.components.GlassCard
import com.campussync.app.core.components.GlassChip
import com.campussync.app.core.components.GlassEmptyState
import com.campussync.app.core.components.GlassMonogram
import com.campussync.app.core.components.GlassPill
import com.campussync.app.core.components.GlassScreen
import com.campussync.app.core.components.glassHairline
import com.campussync.app.core.model.PaymentOrder
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.Error
import com.campussync.app.core.theme.Primary
import com.campussync.app.core.theme.Success
import com.campussync.app.core.theme.Warning
import androidx.compose.ui.layout.ContentScale

@Composable
fun ApprovePaymentScreen(
    currentUser: User,
    onBack: () -> Unit
) {
    val viewModel: ApprovePaymentViewModel = viewModel()
    val pendingOrders by viewModel.pendingOrders.collectAsState()
    val historyOrders by viewModel.historyOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showRejectDialogFor by remember { mutableStateOf<PaymentOrder?>(null) }
    var proofToShow by remember { mutableStateOf<PaymentOrder?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(currentUser.collegeId) {
        viewModel.loadPendingOrders(currentUser.collegeId)
        viewModel.loadHistoryOrders(currentUser.collegeId)
    }

    val currentOrders = if (selectedTabIndex == 0) pendingOrders else historyOrders

    GlassScreen(
        title = "Approve Payments",
        subtitle = if (selectedTabIndex == 0)
            "${pendingOrders.size} UTR${if (pendingOrders.size == 1) "" else "s"} waiting for review"
        else "${historyOrders.size} past decision${if (historyOrders.size == 1) "" else "s"}",
        onBack = onBack
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented filter — same style as dashboard Finance Suite
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(5.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Pending" to Warning, "History" to Primary).forEachIndexed { index, (label, color) ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { selectedTabIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            when {
                isLoading && currentOrders.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                currentOrders.isEmpty() -> {
                    GlassEmptyState(
                        icon = Icons.Rounded.Payments,
                        title = if (selectedTabIndex == 0) "Nothing to review!" else "No history yet",
                        subtitle = if (selectedTabIndex == 0)
                            "All student UTR payments are processed. 🎉"
                        else "Approved & rejected payments will appear here.",
                        accent = if (selectedTabIndex == 0) Success else Primary
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentOrders) { order ->
                            PaymentReviewCard(
                                order = order,
                                isHistory = selectedTabIndex == 1,
                                onApprove = {
                                    viewModel.updateOrderStatus(currentUser.collegeId, order, true) { success, msg ->
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Payment Approved", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Error: $msg", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onReject = { showRejectDialogFor = order },
                                onViewProof = { proofToShow = order },
                                isLoading = isLoading
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    // Reject dialog
    if (showRejectDialogFor != null) {
        AlertDialog(
            onDismissRequest = {
                showRejectDialogFor = null
                rejectReason = ""
            },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Reject Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Rejecting ${showRejectDialogFor?.studentName} (₹${showRejectDialogFor?.expectedTotal?.toInt()}). Student will be asked to re-submit.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Fake screenshot, incorrect amount") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val order = showRejectDialogFor!!
                        viewModel.updateOrderStatus(currentUser.collegeId, order, false, adminNote = rejectReason) { success, msg ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Payment Rejected", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Error: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        showRejectDialogFor = null
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    enabled = rejectReason.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRejectDialogFor = null
                    rejectReason = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Proof viewer
    if (proofToShow != null) {
        Dialog(onDismissRequest = { proofToShow = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Proof", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        IconButton(onClick = { proofToShow = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                    Text(
                        text = "${proofToShow?.studentName} • ₹${proofToShow?.expectedTotal?.toInt()}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground
                    )
                    Text("UTR: ${proofToShow?.utr}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Primary)
                    Spacer(Modifier.height(14.dp))
                    if (proofToShow?.proofUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No receipt image provided", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        AsyncImage(
                            model = proofToShow?.proofUrl,
                            contentDescription = "Receipt Screenshot",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { proofToShow = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Done") }
                }
            }
        }
    }
}

@Composable
fun PaymentReviewCard(
    order: PaymentOrder,
    isHistory: Boolean = false,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewProof: () -> Unit = {},
    isLoading: Boolean
) {
    val statusColor = when {
        order.status == "approved" -> Success
        order.status == "rejected" -> Error
        else -> Warning
    }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassMonogram(name = order.studentName.ifBlank { "S" }, accent = statusColor, size = 40, fontSize = 15)

            Spacer(Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.studentName.ifBlank { "Student" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = order.feeSummaryTitle.ifBlank { "College Fees" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (isHistory) {
                    GlassChip(text = order.status.uppercase(), color = statusColor)
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    "₹${order.expectedTotal.toInt()}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = glassHairline())
        Spacer(Modifier.height(12.dp))

        // UTR block — centered, monospace, tap to copy
        val clipboardContext = androidx.compose.ui.platform.LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable {
                    if (order.utr.isNotBlank()) {
                        val clipboard = clipboardContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("UTR", order.utr))
                        android.widget.Toast.makeText(clipboardContext, "UTR copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "UTR NUMBER — CROSS-CHECK WITH BANK",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tag, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = order.utr.ifBlank { "NO-UTR" },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (isHistory) {
            if (order.status == "rejected" && !order.adminNote.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Reason: ${order.adminNote}",
                    color = Error,
                    fontSize = 12.sp
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (order.proofUrl.isNotBlank()) {
                    GlassPill(
                        text = "Proof",
                        color = Primary,
                        filled = false,
                        leadingIcon = Icons.Rounded.Image,
                        onClick = onViewProof,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                GlassPill(
                    text = "Reject",
                    color = Error,
                    filled = false,
                    leadingIcon = Icons.Rounded.Close,
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                )
                GlassPill(
                    text = "Approve",
                    color = Success,
                    filled = true,
                    leadingIcon = Icons.Rounded.CheckCircle,
                    onClick = onApprove,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}
