package com.campussync.app.feature.fees

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campussync.app.ui.components.bounceClick
import com.campussync.app.ui.components.shimmerEffect
import com.campussync.app.ui.theme.*
import com.campussync.app.core.model.FeeDue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
@Composable
fun StudentFeesScreen(
    currentUser: com.campussync.app.core.model.User,
    checkoutViewModel: CheckoutViewModel = viewModel(),
    sparkViewModel: SparkFeeViewModel = viewModel(),
    studentViewModel: StudentFeeViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val feeDues by studentViewModel.feeDues.collectAsState()
    val isLoading by studentViewModel.isLoading.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Cart State
    val selectedFees = remember { mutableStateListOf<FeeDue>() }
    val totalAmount = selectedFees.sumOf { it.amount }

    LaunchedEffect(currentUser) {
        studentViewModel.loadStudentFees(currentUser.collegeId, currentUser.userId)
    }

    if (showBottomSheet) {
        PaymentUploadBottomSheet(
            checkoutViewModel = checkoutViewModel,
            selectedDues = selectedFees,
            studentId = currentUser.userId,
            studentName = currentUser.name,
            collegeId = currentUser.collegeId,
            totalAmount = totalAmount,
            onDismiss = { 
                showBottomSheet = false 
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedFees.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedFees.size} Items Selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ₹$totalAmount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                        Button(
                            onClick = { showBottomSheet = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Checkout", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isLoading && selectedFees.isEmpty()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                FloatingActionButton(
                    onClick = {
                        android.widget.Toast.makeText(context, "QR Scanner coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    containerColor = BrandPrimary,
                    contentColor = SurfaceWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.bounceClick {
                        android.widget.Toast.makeText(context, "QR Scanner coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = "Scan to Pay")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Page Title
            item {
                Text(
                    text = "My Fees",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // Summary Section
            item {
                val pendingFeesList = feeDues.filter { it.status == "pending" || it.status == "rejected" }
                val paidFeesList = feeDues.filter { it.status == "approved" || it.status == "submitted" }
                val totalPendingAmount = pendingFeesList.sumOf { it.amount }
                val totalPaidAmount = paidFeesList.sumOf { it.amount }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeeSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Pending",
                        amount = "₹${totalPendingAmount}",
                        icon = Icons.Rounded.Warning,
                        color = StatusError,
                        bgColor = StatusErrorBackground,
                        isLoading = isLoading
                    )
                    FeeSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Paid",
                        amount = "₹${totalPaidAmount}",
                        icon = Icons.Rounded.CheckCircle,
                        color = StatusSuccess,
                        bgColor = StatusSuccessBackground,
                        isLoading = isLoading
                    )
                }
            }

            // Pending Fees Section
            item {
                Text(
                    text = "Pending Fees",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading) {
                items(2) { FeeItemSkeleton() }
            } else {
                // Using real FeeDue models for the cart
                val pendingFees = feeDues.filter { it.status == "pending" || it.status == "rejected" }
                if (pendingFees.isEmpty()) {
                    item {
                        Text(
                            text = "No pending fees!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(pendingFees) { fee ->
                    val isSelected = selectedFees.any { it.id == fee.id }
                    FeeDetailCard(
                        feeItem = fee,
                        isSelected = isSelected,
                        onSelectionChange = { checked ->
                            if (checked) selectedFees.add(fee) else selectedFees.removeIf { it.id == fee.id }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Receipts Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Receipts",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading) {
                items(2) { FeeItemSkeleton() }
            } else {
                val paidFees = feeDues.filter { it.status == "approved" || it.status == "submitted" }
                if (paidFees.isEmpty()) {
                    item {
                        Text(
                            text = "No recent receipts",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(paidFees) { fee ->
                    FeeDetailCard(
                        feeItem = fee,
                        onDownloadReceipt = {
                            sparkViewModel.downloadReceipt(fee, currentUser.name, currentUser.collegeId, context)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            item { Spacer(modifier = Modifier.height(120.dp)) } // Padding for BottomBar / FAB
        }
    }
}

@Composable
fun FeeSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    isLoading: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.bounceClick {
            android.widget.Toast.makeText(context, "$title details coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(28.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            } else {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = color // Colorizing the amount makes it pop
                )
            }
        }
    }
}

@Composable
fun FeeDetailCard(
    feeItem: FeeDue,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    onDownloadReceipt: (() -> Unit)? = null
) {
    val isPending = feeItem.status == "pending" || feeItem.status == "rejected"
    val icon = if (isPending) Icons.Rounded.List else Icons.Rounded.CheckCircle
    val iconColor = if (isPending) StatusWarning else StatusSuccess
    val bgColor = if (isPending) StatusWarningBackground else StatusSuccessBackground

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { 
                if (isPending && onSelectionChange != null) {
                    onSelectionChange(!isSelected)
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPending && onSelectionChange != null) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feeItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = feeItem.dueDate,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${feeItem.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPending) TextPrimary else StatusSuccess
                )
                if (isPending) {
                    Text(
                        text = if (isSelected) "Selected" else "Select to Pay",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandPrimary
                    )
                } else if (onDownloadReceipt != null) {
                    TextButton(
                        onClick = onDownloadReceipt, 
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Receipt", style = MaterialTheme.typography.labelSmall, color = BrandPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun FeeItemSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).shimmerEffect())
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.height(20.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(16.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
        }
    }
}
