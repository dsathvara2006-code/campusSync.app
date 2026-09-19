package com.campussync.app.feature.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.theme.Primary

data class FaqItem(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    val faqs = remember {
        listOf(
            FaqItem(
                question = "How do I pay my fees via UPI?",
                answer = "Go to the Student Fees section, select your pending fee due, tap 'Pay via UPI', and choose your preferred UPI app (Google Pay, PhonePe, Paytm, etc.). After completing the payment, enter the 12-digit UTR/Reference transaction number to submit for verification."
            ),
            FaqItem(
                question = "Why is my fee payment pending?",
                answer = "Once you submit your UTR number, your college administrative officer verifies the transaction in the bank account. Verification typically completes within 24 to 48 working hours. Your fee receipt will be ready once approved."
            ),
            FaqItem(
                question = "How do I check my attendance?",
                answer = "Open your Student Dashboard to view the live attendance ring and summary percentages. Tap on any subject to view lecture-by-lecture logs, teacher names, and present/absent status."
            ),
            FaqItem(
                question = "I didn't receive an invite email.",
                answer = "Accounts in CampusSync are invite-only, managed directly by your college administration. Please check your spam/junk folder or contact your college administrator to confirm your registration and correct email address."
            ),
            FaqItem(
                question = "Where can I find study materials?",
                answer = "Navigate to the Resources section from the dashboard. You can filter by subject or semester, download lecture notes, view shared syllabus PDFs, and access previous examination papers."
            ),
            FaqItem(
                question = "How can faculty mark attendance?",
                answer = "Faculty can select their assigned class and lecture slot from the Timetable, tap 'Take Attendance', toggle student statuses, and tap 'Submit Attendance' to log real-time attendance records."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Center", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Frequently Asked Questions",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap any question below to view detailed answers.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            faqs.forEach { faq ->
                ExpandableHelpTopicItem(faq.question, faq.answer)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ExpandableHelpTopicItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "faqChevronRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = question,
                fontSize = 15.sp,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium,
                color = if (expanded) Primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = if (expanded) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotationAngle)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
