package com.campussync.app.feature.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreenTemplate(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun LegalPlaceholderWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
    ) {
        Text(
            text = "Pending final legal review and business details (e.g. operator name, jurisdiction, contact addresses). Do not publish this version.",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Privacy Policy", onBack = onBack) {
        LegalPlaceholderWarning()
        Text("1. Data Collection", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("We collect email, name, role, college ID, and attendance/fee information when you use CampusSync. The legal entity operating this service is currently pending confirmation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("2. Purpose of Processing", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Data is processed strictly for the administration of college activities, attendance tracking, and fee management.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("3. Storage & Security", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("We use Google Firebase (Firestore/Auth/Storage) with strict security rules separating data by college tenant.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("4. Contact Us", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Privacy contact email address: [PENDING].", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Terms of Service", onBack = onBack) {
        LegalPlaceholderWarning()
        Text("1. Acceptance of Terms", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("By accessing CampusSync, you agree to these Terms. Legal jurisdiction is [PENDING].", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("2. User Accounts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Accounts are created via invites managed by your college administrator. You must use a valid Google account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RefundPolicyScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Refund Policy", onBack = onBack) {
        LegalPlaceholderWarning()
        Text("Fee Payment Refunds", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("CampusSync acts as a zero-commission facilitator for UPI payments directly to your institution. Refunds for duplicate or erroneous transactions are handled directly by the college administration.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please contact your college admin at [PENDING] for refund inquiries.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AcceptableUsePolicyScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Acceptable Use Policy", onBack = onBack) {
        LegalPlaceholderWarning()
        Text("Content Guidelines", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Users must not upload illegal, harmful, or inappropriate materials to the Resources or Notice sections. Violations may result in immediate account suspension by the college administrator.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CommunityGuidelinesScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Community Guidelines", onBack = onBack) {
        LegalPlaceholderWarning()
        Text("Respect and Professionalism", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("CampusSync is an educational environment. Communication via notices and assignments should remain professional and respectful to all students and faculty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
