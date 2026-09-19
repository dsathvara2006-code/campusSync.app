package com.campussync.app.feature.fees

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.campussync.app.core.model.FeeDue
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.Primary
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeeScreen(
    currentUser: User,
    onBack: () -> Unit
) {
    val viewModel: StudentFeeViewModel = viewModel()
    val feeDues by viewModel.feeDues.collectAsState()
    val feeSettings by viewModel.feeSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val antiSpamManager = remember { com.campussync.app.core.utils.AntiSpamManager(context) }

    var selectedDueForPayment by remember { mutableStateOf<FeeDue?>(null) }
    
    LaunchedEffect(currentUser) {
        viewModel.loadStudentFees(currentUser.collegeId, currentUser.userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Fees", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (isLoading && feeDues.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (feeDues.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pending fees! 🎉", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(feeDues) { due ->
                    FeeDueCard(due = due, onPayClicked = { selectedDueForPayment = due })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedDueForPayment != null && feeSettings != null) {
        PaymentBottomSheet(
            feeDue = selectedDueForPayment!!,
            upiId = feeSettings!!.upiId,
            collegeName = feeSettings!!.collegeName,
            onDismiss = { selectedDueForPayment = null },
            onSubmitProof = { utr, uri ->
                if (antiSpamManager.canSubmitPayment()) {
                    viewModel.submitPaymentProof(
                        collegeId = currentUser.collegeId,
                        studentId = currentUser.userId,
                        studentName = currentUser.name,
                        feeDue = selectedDueForPayment!!,
                        utrNumber = utr,
                        proofUri = uri
                    ) { success, msg ->
                        if (success) {
                            selectedDueForPayment = null
                            android.widget.Toast.makeText(context, "Payment submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, msg ?: "Failed to submit payment", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Security Alert: You have reached the maximum 3 payment submissions for today. Try again tomorrow.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            },
            isLoading = isLoading
        )
    }
}

@Composable
fun FeeDueCard(due: FeeDue, onPayClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (due.status == "approved") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = due.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text("₹${due.amount}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Primary)
            }
            Spacer(Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusColor, statusIcon, statusText) = when (due.status) {
                    "pending" -> Triple(Color(0xFFE65100), Icons.Rounded.Warning, "Pending Payment")
                    "submitted" -> Triple(Color(0xFF1976D2), Icons.Rounded.CheckCircle, "Verification Pending")
                    "approved" -> Triple(Color(0xFF388E3C), Icons.Rounded.CheckCircle, "Paid Successfully")
                    "rejected" -> Triple(Color.Red, Icons.Rounded.Warning, "Payment Rejected")
                    else -> Triple(Color.Gray, Icons.Rounded.Warning, due.status)
                }
                
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Due: ${due.dueDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            if (due.status == "rejected" && due.adminNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Reason: ${due.adminNote}",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            
            if (due.status == "pending" || due.status == "rejected") {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onPayClicked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Pay Now")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    feeDue: FeeDue,
    upiId: String,
    collegeName: String,
    onDismiss: () -> Unit,
    onSubmitProof: (String, Uri) -> Unit,
    isLoading: Boolean
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var utrNumber by remember { mutableStateOf("") }
    var utrError by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // Generate QR Code Bitmap for UPI
    val qrBitmap = remember(upiId, feeDue.amount) {
        generateUpiQrCode(upiId, collegeName, feeDue.amount.toString())
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pay Zero-Commission Fee", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Scan QR from GPay, PhonePe, or Paytm", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            
            Spacer(Modifier.height(24.dp))
            
            // Display QR Code
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "UPI QR Code",
                    modifier = Modifier.size(200.dp)
                )
            } else {
                Box(Modifier.size(200.dp).background(Color.LightGray)) {
                    Text("Failed to load QR", modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Paying to: $upiId", fontWeight = FontWeight.Bold)
            Text("Amount: ₹${feeDue.amount}", color = Primary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            
            Divider(Modifier.padding(vertical = 24.dp))
            
            // Upload Proof Section
            Text("Submit Payment Proof", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = utrNumber,
                onValueChange = { 
                    if (it.length <= 12 && it.all { char -> char.isDigit() }) {
                        utrNumber = it 
                        utrError = null
                    }
                },
                label = { Text("12-Digit UTR / Ref Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = utrError != null,
                supportingText = if (utrError != null) { { Text(utrError!!, color = MaterialTheme.colorScheme.error) } } else null
            )
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(if (selectedImageUri != null) "Screenshot Selected ✓" else "Upload Screenshot")
            }
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (utrNumber.length != 12) {
                        utrError = "UTR must be exactly 12 digits"
                        return@Button
                    }
                    if (selectedImageUri == null) {
                        utrError = "Screenshot is required"
                        return@Button
                    }
                    onSubmitProof(utrNumber, selectedImageUri!!)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && selectedImageUri != null && utrNumber.length == 12,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (isLoading) "Submitting..." else "Submit Proof", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Smart QR Generator Logic using ZXing
fun generateUpiQrCode(upiId: String, name: String, amount: String): Bitmap? {
    return try {
        val cleanUpiId = upiId.trim().lowercase()
        if (cleanUpiId.isBlank() || !cleanUpiId.contains("@")) return null

        // Format amount properly — UPI needs exactly 2 decimal places (e.g. "500.00")
        val cleanAmount = amount.trim().toDoubleOrNull()?.let { "%.2f".format(it) } ?: return null

        // Sanitize payee name — remove special chars, URL-encode spaces
        val cleanName = name.trim()
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .take(50)
            .replace(" ", "%20")
            .ifBlank { "College" }

        // Standard UPI deep link — pn (payee name) is REQUIRED by GPay, PhonePe, Paytm
        val upiUri = "upi://pay?pa=$cleanUpiId&pn=$cleanName&am=$cleanAmount&cu=INR&tn=CollegeFeePayment"

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(upiUri, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
