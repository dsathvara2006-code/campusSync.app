package com.campussync.app.feature.attendance

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campussync.app.core.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    collegeId: String,
    onBack: () -> Unit,
    viewModel: OverviewViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val stats     by viewModel.stats.collectAsState()
    val classes   by viewModel.classes.collectAsState()
    val notices   by viewModel.notices.collectAsState()
    val events    by viewModel.events.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val error     by viewModel.error.collectAsState()

    LaunchedEffect(collegeId) { viewModel.load(collegeId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Campus Overview", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Real-time data", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(collegeId) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = Primary)
                    Text("Loading overview...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
                    Text("Failed to load data", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(error ?: "", color = TextSecondary, fontSize = 12.sp)
                    Button(onClick = { viewModel.load(collegeId) }) { Text("Retry") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── Section 1: Summary Stats Grid ──
                item {
                    OverviewSectionHeader(icon = Icons.Rounded.Dashboard, title = "Summary", subtitle = "College at a glance")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OverviewStatCard("Students", stats.studentCount.toString(), Icons.Rounded.Groups, Primary, Modifier.weight(1f))
                            OverviewStatCard("Teachers", stats.teacherCount.toString(), Icons.Rounded.School, Secondary, Modifier.weight(1f))
                            OverviewStatCard("Classes", stats.classCount.toString(), Icons.Rounded.Class, Info, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OverviewStatCard("Pending Users", stats.pendingUsersCount.toString(), Icons.Rounded.HourglassEmpty, Warning, Modifier.weight(1f))
                            OverviewStatCard("Avg Attendance", "${stats.averageAttendancePercent}%",
                                Icons.Rounded.CheckCircleOutline,
                                if (stats.averageAttendancePercent >= 75) Success else if (stats.averageAttendancePercent >= 60) Warning else Error,
                                Modifier.weight(1f))
                            OverviewStatCard("Notices", stats.noticeCount.toString(), Icons.Rounded.Campaign, Rose500, Modifier.weight(1f))
                        }
                    }
                }

                // ── Section 2: Fee Overview ──
                item {
                    OverviewSectionHeader(icon = Icons.Rounded.AccountBalance, title = "Fee Overview", subtitle = "${stats.totalFeeDues} total dues assigned")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OverviewStatCard("Pending Fees", stats.pendingFeeCount.toString(), Icons.Rounded.PendingActions, Warning, Modifier.weight(1f))
                        OverviewStatCard("Paid / Approved", stats.paidFeeCount.toString(), Icons.Rounded.CheckCircle, Success, Modifier.weight(1f))
                        OverviewStatCard("Events", stats.upcomingEventCount.toString(), Icons.Rounded.Event, Secondary, Modifier.weight(1f))
                    }
                }

                // ── Section 3: Classes Breakdown ──
                item {
                    OverviewSectionHeader(icon = Icons.Rounded.Class, title = "Classes", subtitle = "${classes.size} classes · last attendance %")
                }
                if (classes.isEmpty()) {
                    item {
                        OverviewEmptyCard("No classes created yet.")
                    }
                } else {
                    items(classes) { cls ->
                        OverviewClassCard(cls)
                    }
                }

                // ── Section 4: Recent Notices ──
                item {
                    OverviewSectionHeader(icon = Icons.Rounded.Campaign, title = "Recent Notices", subtitle = "Last 5 notices")
                }
                if (notices.isEmpty()) {
                    item { OverviewEmptyCard("No notices posted yet.") }
                } else {
                    items(notices) { notice ->
                        OverviewNoticeCard(notice)
                    }
                }

                // ── Section 5: Upcoming Events ──
                item {
                    OverviewSectionHeader(icon = Icons.Rounded.Event, title = "Upcoming Events", subtitle = "From today onward")
                }
                if (events.isEmpty()) {
                    item { OverviewEmptyCard("No upcoming events.") }
                } else {
                    items(events) { event ->
                        OverviewEventCard(event)
                    }
                }

                // ── Section 6: Recent Activity ──
                item {
                    OverviewSectionHeader(icon = Icons.Rounded.History, title = "Recent Activity", subtitle = "Last 6 actions")
                }
                if (activities.isEmpty()) {
                    item { OverviewEmptyCard("No activity logs yet.") }
                } else {
                    items(activities) { activity ->
                        OverviewActivityCard(activity)
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────

@Composable
private fun OverviewSectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Primary, Secondary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun OverviewStatCard(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = tint)
            Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OverviewClassCard(cls: OverviewClassRow) {
    val attColor = when {
        cls.hasNoData -> TextSecondary
        cls.attendancePercent >= 75 -> Success
        cls.attendancePercent >= 60 -> Warning
        else -> Error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Class, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                if (cls.semester.isNotBlank())
                    Text("Semester ${cls.semester}", fontSize = 12.sp, color = TextSecondary)
                Text("${cls.studentCount} students", fontSize = 11.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (cls.hasNoData) "No data" else "${cls.attendancePercent}%",
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = attColor
                )
                Text("attendance", fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun OverviewNoticeCard(notice: OverviewNotice) {
    val dateStr = remember(notice.timestamp) {
        if (notice.timestamp > 0)
            SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(notice.timestamp))
        else ""
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Rose500.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Rose500, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(notice.message, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                if (dateStr.isNotBlank())
                    Spacer(Modifier.height(4.dp))
                    Text(dateStr, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun OverviewEventCard(event: OverviewEvent) {
    val dateStr = remember(event.eventDate) {
        if (event.eventDate > 0)
            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(event.eventDate))
        else ""
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Secondary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Event, contentDescription = null, tint = Secondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                if (event.description.isNotBlank())
                    Text(event.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (dateStr.isNotBlank())
                    Text(dateStr, fontSize = 11.sp, color = Primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OverviewActivityCard(activity: OverviewActivity) {
    val timeStr = remember(activity.timestamp) {
        if (activity.timestamp > 0)
            SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(activity.timestamp))
        else ""
    }
    val tint = when (activity.type) {
        "fee"    -> Success
        "event"  -> Secondary
        "class"  -> Primary
        "invite" -> Info
        else     -> TextSecondary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(tint)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(activity.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                if (activity.description.isNotBlank())
                    Text(activity.description, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(timeStr, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun OverviewEmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(message, color = TextSecondary, fontSize = 13.sp)
        }
    }
}
