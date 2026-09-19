package com.campussync.app.feature.fees

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campussync.app.core.model.FeeDue
import com.campussync.app.ui.theme.BrandPrimary
import com.campussync.app.ui.theme.StatusError
import com.campussync.app.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentUploadBottomSheet(
    checkoutViewModel: CheckoutViewModel,
    selectedDues: List<FeeDue>,
    studentId: String,
    studentName: String,
    collegeId: String,
    totalAmount: Double,
    onDismiss: () -> Unit,
    sparkViewModel: SparkFeeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    var utr by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe fee settings for QR generation
    val feeSettings by sparkViewModel.feeSettings.collectAsState()
    val qrBitmap = remember(feeSettings, totalAmount) {
        if (feeSettings != null && totalAmount > 0) {
            generateUpiQrCode(
                upiId = feeSettings!!.upiId,
                name = feeSettings!!.collegeName,
                amount = String.format("%.2f", totalAmount)
            )
        } else null
    }

    // Load fee settings if not already loaded
    LaunchedEffect(collegeId) {
        if (feeSettings == null) {
            sparkViewModel.loadStudentFees(collegeId, studentId)
        }
    }

    // Strict UI State Observation
    val uploadState by checkoutViewModel.uploadState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {
            checkoutViewModel.resetState()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Step 1: Pay ──
            Text(
                text = "Step 1: Pay ₹${String.format("%.0f", totalAmount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // QR Code Display
            if (qrBitmap != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scan this QR with any UPI app",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = "(GPay, PhonePe, Paytm, BHIM etc.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // UPI ID with copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPI ID: ${feeSettings?.upiId ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(feeSettings?.upiId ?: ""))
                            android.widget.Toast.makeText(context, "UPI ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            modifier = Modifier.size(16.dp),
                            tint = BrandPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

            } else {
                // Loading state for QR
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = BrandPrimary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading QR Code...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Step 2: Enter UTR ──
            Text(
                text = "Step 2: Enter UTR after payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = utr,
                onValueChange = { utr = it },
                label = { Text("UTR / Transaction ID") },
                placeholder = { Text("Enter 12-digit UTR number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            val isLoading = uploadState is UploadState.Loading
            Button(
                onClick = {
                    checkoutViewModel.submitCartPayment(
                        dues = selectedDues,
                        utr = utr,
                        studentId = studentId,
                        studentName = studentName,
                        collegeId = collegeId
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading && utr.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Submitting...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Submit UTR", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reactive UI Feedback
            when (val state = uploadState) {
                is UploadState.Success -> {
                    Text(
                        text = state.message,
                        color = StatusSuccess,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                is UploadState.Error -> {
                    Text(
                        text = state.errorMsg,
                        color = StatusError,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                else -> { /* Idle or Loading */ }
            }
        }
    }
}
