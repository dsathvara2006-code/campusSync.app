package com.campussync.app.feature.fees

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campussync.app.core.model.FeeDue
import com.campussync.app.core.model.User
import com.campussync.app.ui.components.antiSpamClick
import com.campussync.app.ui.theme.*
import com.campussync.app.core.components.EmptyState
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeeCheckoutScreen(
    currentUser: User,
    onBack: () -> Unit
) {
    val viewModel: SparkFeeViewModel = viewModel()
    val feeDues by viewModel.feeDues.collectAsState()
    val feeSettings by viewModel.feeSettings.collectAsState()
    val checkoutState by viewModel.checkoutState.collectAsState()
    val context = LocalContext.current

    // Cart State
    val selectedFees = remember { mutableStateListOf<FeeDue>() }
    val totalAmount = selectedFees.sumOf { it.amount }
    
    // UTR Input State
    var utrNumber by remember { mutableStateOf("") }
    
    // QR Code generation
    val qrBitmap = remember(selectedFees.size, feeSettings) {
        if (selectedFees.isNotEmpty() && feeSettings != null) {
            generateUpiQrCode(
                upiId = feeSettings!!.upiId,
                name = feeSettings!!.collegeName,
                amount = "%.2f".format(totalAmount)
            )
        } else null
    }

    // Load data on start
    LaunchedEffect(currentUser) {
        viewModel.loadStudentFees(currentUser.collegeId, currentUser.userId)
    }

    // Handle Checkout State changes
    LaunchedEffect(checkoutState) {
        when (checkoutState) {
            is CheckoutState.Success -> {
                Toast.makeText(context, (checkoutState as CheckoutState.Success).message, Toast.LENGTH_LONG).show()
                selectedFees.clear()
                utrNumber = ""
                viewModel.resetState()
            }
            is CheckoutState.Error -> {
                Toast.makeText(context, (checkoutState as CheckoutState.Error).errorMsg, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {} // Idle or Loading
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Fee Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                        
                        if (qrBitmap != null && feeSettings != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Scan QR to Pay via UPI", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "UPI QR Code",
                                        modifier = Modifier.size(160.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Paying to: ${feeSettings!!.upiId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = utrNumber,
                            onValueChange = { newValue ->
                                // Restrict input to digits and exactly 12 length
                                if (newValue.length <= 12 && newValue.all { it.isDigit() }) {
                                    utrNumber = newValue
                                }
                            },
                            label = { Text("12-Digit UTR Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp), // Let it Breathe: rounded corners
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val isLoading = checkoutState is CheckoutState.Loading
                        Button(
                            onClick = {
                                if (utrNumber.length == 12 && selectedFees.isNotEmpty()) {
                                    viewModel.submitFeeWithoutStorage(
                                        collegeId = currentUser.collegeId,
                                        studentId = currentUser.userId,
                                        studentName = currentUser.name,
                                        selectedDues = selectedFees.toList(),
                                        totalAmount = totalAmount,
                                        utrNumber = utrNumber
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .antiSpamClick(delayMillis = 1500L) {
                                    if (utrNumber.length == 12 && selectedFees.isNotEmpty()) {
                                        viewModel.submitFeeWithoutStorage(
                                            collegeId = currentUser.collegeId,
                                            studentId = currentUser.userId,
                                            studentName = currentUser.name,
                                            selectedDues = selectedFees.toList(),
                                            totalAmount = totalAmount,
                                            utrNumber = utrNumber
                                        )
                                    }
                                },
                            enabled = !isLoading && utrNumber.length == 12 && selectedFees.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Pay & Submit UTR", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Let it Breathe: Extra spacing
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            val pendingFees = feeDues.filter { it.status == "pending" }
            val completedFees = feeDues.filter { it.status != "pending" }

            if (pendingFees.isEmpty() && completedFees.isEmpty()) {
                item {
                    EmptyState(
                        text = "No fees assigned to you.",
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
            } else {
                if (pendingFees.isNotEmpty()) {
                    item {
                        Text("Pending Dues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(pendingFees) { fee ->
                        val isSelected = selectedFees.any { it.id == fee.id }
                        SparkFeeCard(
                            feeItem = fee,
                            isSelected = isSelected,
                            onSelectionChange = { checked ->
                                if (checked) selectedFees.add(fee) else selectedFees.removeIf { it.id == fee.id }
                            }
                        )
                    }
                }

                if (completedFees.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Past Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(completedFees) { fee ->
                        SparkFeeCard(
                            feeItem = fee,
                            isSelected = false,
                            onSelectionChange = null, // Read-only past records
                            onDownloadReceipt = {
                                viewModel.downloadReceipt(fee, currentUser.name, currentUser.collegeId, context)
                            }
                        )
                    }
                }
            }
            
            // Add extra space at bottom if cart is visible to avoid overlapping
            item { 
                Spacer(modifier = Modifier.height(if (selectedFees.isNotEmpty()) 240.dp else 40.dp)) 
            }
        }
    }
}

@Composable
fun SparkFeeCard(
    feeItem: FeeDue,
    isSelected: Boolean,
    onSelectionChange: ((Boolean) -> Unit)?,
    onDownloadReceipt: (() -> Unit)? = null
) {
    val isPending = feeItem.status == "pending"
    
    val icon = if (isPending) Icons.Rounded.List else Icons.Rounded.CheckCircle
    val iconColor = if (isPending) StatusWarning else StatusSuccess
    val bgColor = if (isPending) StatusWarningBackground else StatusSuccessBackground

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Let it Breathe
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BrandPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Let it Breathe: more inner padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPending && onSelectionChange != null) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feeItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Due: ${feeItem.dueDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        text = "Pay Now",
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
