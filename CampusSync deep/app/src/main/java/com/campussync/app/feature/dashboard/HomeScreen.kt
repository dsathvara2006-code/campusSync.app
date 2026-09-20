package com.campussync.app.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.components.*
import com.campussync.app.core.model.Notice
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.*
import com.campussync.app.feature.assignments.AssignmentViewModel
import com.campussync.app.feature.attendance.AttendanceViewModel
import com.campussync.app.feature.timetable.TimetableViewModel
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.foundation.lazy.LazyRow
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.format.DateTimeFormatter

/**
 * ============================================================================
 *  HomeScreen — NOVA redesign
 *  Design intent:
 *   • One consistent "elevated soft card" language everywhere (20dp radius,
 *     hairline border, tiny shadow) instead of mixed flat/neon styles.
 *   • Student/teacher header = warm greeting card w/ gradient + avatar ring.
 *   • Admin/Principal header = compact utility bar, dashboard does the talking.
 *   • Quick actions get a gradient icon tile instead of flat tinted box.
 *   • Attendance ring redesigned as a hero stat, not just a side gauge.
 *   • Admin/Principal dashboards get a proper stat-strip + chart cards.
 *  BACKEND LOGIC UNCHANGED — every ViewModel call, state collection and
 *  callback below is identical to the original; only composition/visuals
 *  were rewritten.
 * ============================================================================
 */
@Composable
fun HomeScreen(
    user: User,
    classId: String,
    attendanceViewModel: AttendanceViewModel,
    timetableViewModel: TimetableViewModel,
    assignmentViewModel: AssignmentViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToFeeTypes: () -> Unit = {},
    onNavigateToAssignFee: () -> Unit = {},
    onNavigateToStudentFees: () -> Unit = {},
    onNavigateToApprovePayments: () -> Unit = {},
    onNavigateToManageUsers: () -> Unit = {},
    onNavigateToVerifyInvites: () -> Unit = {},
    onNavigateToPrincipalOverview: () -> Unit = {},
    onNavigateToLecture: (String, String) -> Unit = { _, _ -> },
    onNavigateToDailySummary: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isTeacher = user.role.equals("teacher", ignoreCase = true)
    val isPrincipal = user.role.equals("principal", ignoreCase = true)
    val isAdmin = user.role.equals("admin", ignoreCase = true) ||
                  user.role.equals("tenant_admin", ignoreCase = true)

    val greeting = remember {
        val h = LocalTime.now().hour
        when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
    }
    val greetingEmoji = remember {
        val h = LocalTime.now().hour
        when { h < 12 -> "☀️"; h < 17 -> "⚡"; else -> "🌙" }
    }
    val todayName = remember { LocalDate.now().dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US) }
    val todayDate = remember {
        val d = LocalDate.now()
        "${d.dayOfMonth} ${d.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.US)}, ${d.year}"
    }

    val noticeViewModel: com.campussync.app.feature.notice.NoticeViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val principalViewModel: com.campussync.app.feature.dashboard.PrincipalViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val holidayViewModel: com.campussync.app.core.viewmodel.HolidayViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val holidayDates by holidayViewModel.holidayDates.collectAsState()

    LaunchedEffect(classId, user.userId, holidayDates) {
        if (classId.isNotBlank()) {
            timetableViewModel.loadTimetable(user.collegeId, classId)
            assignmentViewModel.loadAssignments(user.collegeId, classId)
            if (!isTeacher) attendanceViewModel.loadOverallPercentage(user.collegeId, classId, user.userId, holidayDates)
        }
        noticeViewModel.listenToNotices(user.collegeId)
        holidayViewModel.listenToHolidays(user.collegeId)
        if (isPrincipal || isAdmin) {
            holidayViewModel.initializeDefaultHolidays(user.collegeId)
        }
        principalViewModel.loadClasses(user.collegeId)
        if (isPrincipal || isAdmin) {
            principalViewModel.loadDashboardStats(user.collegeId)
        }
    }

    val timetable            = timetableViewModel.timetable.value
    val isTimetableLoading   = timetableViewModel.isLoading.value
    val assignments          by assignmentViewModel.assignments.collectAsState()
    val isAssignmentsLoading by assignmentViewModel.isLoading.collectAsState()
    val overallPctState      by attendanceViewModel.overallPercentage.collectAsState()
    val overallPercentage    = overallPctState ?: 0
    val presentDays          by attendanceViewModel.presentDays.collectAsState()
    val totalDays             by attendanceViewModel.totalDays.collectAsState()
    val classes               by principalViewModel.classes.collectAsState()

    val displayName = remember(classId, classes) {
        classes.find { it.classId == classId }?.className?.takeIf { it.isNotBlank() } ?: classId
    }

    val todayClasses = remember(timetable) {
        timetable.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }.sortedBy { it.time }
    }
    val activeAssignments = remember(assignments) {
        val today = LocalDate.now()
        assignments.filter {
            try { !LocalDate.parse(it.dueDate).isBefore(today) } catch (e: Exception) { true }
        }.take(3)
    }

    var showAnnouncementsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        if (!isAdmin && !isPrincipal) {
            StudentHeader(
                user = user,
                classId = displayName,
                todayDate = todayDate,
                greeting = greeting,
                greetingEmoji = greetingEmoji,
                onOpenSettings = onOpenSettings,
                onBell = { showAnnouncementsDialog = true }
            )
        }

        // ONE unified admin console: Principal and Admin share the exact same
        // dashboard — admin can do everything a principal could (3-role system:
        // Admin / Teacher / Student).
        if (isPrincipal || isAdmin) {
            AdminDashboardContent(
                user = user,
                principalViewModel = principalViewModel,
                onNavigateToFeeTypes = onNavigateToFeeTypes,
                onNavigateToAssignFee = onNavigateToAssignFee,
                onNavigateToApprovePayments = onNavigateToApprovePayments,
                onNavigateToManageUsers = onNavigateToManageUsers,
                onNavigateToVerifyInvites = onNavigateToVerifyInvites,
                onNavigateToOverview = onNavigateToPrincipalOverview,
                onOpenSettings = onOpenSettings,
                onNotifications = { showAnnouncementsDialog = true },
                onNavigateToDailySummary = onNavigateToDailySummary
            )
        } else {
            Spacer(Modifier.height(8.dp))

            // ── Quick Actions ────────────────────────────────────────────
            SectionTitle(title = "Quick Actions")
            Spacer(Modifier.height(12.dp))


            val actionItems = if (isTeacher) listOf(
                ActionItemData("Attendance", "Register today's roll", Icons.Rounded.CheckCircle, Success, 1),
                ActionItemData("Assignments", "Post tasks & deadlines", Icons.Rounded.Edit, Secondary, 3),
                ActionItemData("Timetable", "Weekly class schedule", Icons.Rounded.DateRange, Primary, 2),
                ActionItemData("Resources", "Notes, PDFs & PPTs", Icons.Rounded.Star, Warning, -1)
            ) else listOf(
                ActionItemData("Attendance", "Check your standing", Icons.Rounded.CheckCircle, Success, 1),
                ActionItemData("Assignments", "View pending tasks", Icons.Rounded.Edit, Secondary, 3),
                ActionItemData("Timetable", "Weekly schedule", Icons.Rounded.DateRange, Primary, 2),
                ActionItemData("Resources", "Notes, PDFs & PPTs", Icons.Rounded.Star, Warning, -1)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                actionItems.take(2).forEach { item ->
                    ActionCard(item = item, modifier = Modifier.weight(1f)) {
                        if (item.tabIndex >= 0) onNavigateToTab(item.tabIndex) else onNavigateToResources()
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                actionItems.drop(2).forEach { item ->
                    ActionCard(item = item, modifier = Modifier.weight(1f)) {
                        if (item.tabIndex >= 0) onNavigateToTab(item.tabIndex) else onNavigateToResources()
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Attendance hero (students only) ─────────────────────────
            if (!isTeacher && !isAdmin) {
                SectionTitle(title = "Attendance", actionLabel = "Details", onAction = { onNavigateToTab(1) })
                Spacer(Modifier.height(12.dp))
                AttendanceCard(overallPercentage, presentDays, totalDays)
                Spacer(Modifier.height(20.dp))

                // Fees teaser — folded into a slim row instead of a big banner
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onNavigateToStudentFees() },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Amber500, Color(0xFFF97316)))),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fees & Dues", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text("View pending fees & pay via UPI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── Today's Schedule ─────────────────────────────────────────
            SectionTitle(title = "Today's Schedule", actionLabel = "All", onAction = { onNavigateToTab(2) })
            Spacer(Modifier.height(12.dp))

            if (isTimetableLoading) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { ShimmerCard() } }
            } else if (todayClasses.isEmpty()) {
                EmptyStateCard(icon = Icons.Rounded.DateRange, title = "No classes today", subtitle = "Enjoy your free day 🎉", iconColor = Primary)
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todayClasses.forEachIndexed { idx, lecture -> ScheduleCard(subject = lecture.subject, time = lecture.time, index = idx) }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Upcoming Deadlines ────────────────────────────────────────
            SectionTitle(title = "Upcoming Deadlines", actionLabel = "See all", onAction = { onNavigateToTab(3) })
            Spacer(Modifier.height(12.dp))

            if (isAssignmentsLoading) {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { repeat(2) { ShimmerCard() } }
            } else if (activeAssignments.isEmpty()) {
                EmptyStateCard(icon = Icons.Rounded.CheckCircle, title = "All caught up!", subtitle = "No pending assignments 🚀", iconColor = Success)
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeAssignments.forEachIndexed { idx, assignment -> DeadlineCard(title = assignment.title, dueDate = assignment.dueDate, index = idx) }
                }
            }
        }

        Spacer(Modifier.height(90.dp))
    }

    if (showAnnouncementsDialog) {
        AnnouncementsModal(
            isTeacher = isTeacher,
            notices = noticeViewModel.notices.value,
            onDismiss = { showAnnouncementsDialog = false },
            onAddNotice = { noticeViewModel.addNotice(user.collegeId, it) {} }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
//  DATA
// ════════════════════════════════════════════════════════════════════════
private data class ActionItemData(
    val title: String, val subtitle: String, val icon: ImageVector, val color: Color, val tabIndex: Int
)

// ════════════════════════════════════════════════════════════════════════
//  HEADERS
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun StudentHeader(
    user: User, classId: String, todayDate: String, greeting: String, greetingEmoji: String,
    onOpenSettings: () -> Unit, onBell: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Indigo700, Indigo500, Violet500)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 28.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$greetingEmoji  $greeting", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(user.name.ifBlank { "Student" }, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
                }


                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                Text(
                    "Class $classId  •  $todayDate",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AdminHeader(user: User, onOpenSettings: () -> Unit, onBell: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(Indigo500, Violet500))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }

        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val roleName = user.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val displayName = if (user.name.equals("User", true) || user.name.isBlank()) "Admin" else user.name
            Text("Welcome, $displayName", color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$roleName Dashboard", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }


        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonPurple, NeonBlue)))
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) { 
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp)) 
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  SECTION / CARD PRIMITIVES
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun SectionTitle(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, letterSpacing = (-0.2).sp)
        if (actionLabel != null && onAction != null) {
            Row(modifier = Modifier.clickable { onAction() }, verticalAlignment = Alignment.CenterVertically) {
                Text(actionLabel, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ActionCard(item: ActionItemData, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "cardScale")

    Surface(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(item.color.copy(alpha = 0.9f), item.color.copy(alpha = 0.6f)))),
                contentAlignment = Alignment.Center
            ) { Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(item.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AttendanceCard(overallPercentage: Int, presentDays: Int, totalDays: Int) {
    val statusColor = when {
        totalDays == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        overallPercentage >= 75 -> Success
        overallPercentage >= 60 -> Warning
        else -> Error
    }
    val statusText = when {
        totalDays == 0 -> "No data yet"
        overallPercentage >= 75 -> "Good standing"
        overallPercentage >= 60 -> "Needs improvement"
        else -> "Below 75% requirement"
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 22f, cap = StrokeCap.Round))
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Indigo400, Violet500, Cyan500)),
                        startAngle = -90f, sweepAngle = (overallPercentage / 100f) * 360f, useCenter = false,
                        style = Stroke(width = 22f, cap = StrokeCap.Round)
                    )
                }
                Text("$overallPercentage%", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Overall Attendance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("$presentDays / $totalDays days", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                }
            }
        }
    }
}

private val scheduleColors = listOf(Indigo500, Violet500, Sky500, Emerald500, Amber500)

@Composable
private fun ScheduleCard(subject: String, time: String, index: Int) {
    val accent = scheduleColors[index % scheduleColors.size]
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subject, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text(time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text("Room 104", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DeadlineCard(title: String, dueDate: String, index: Int) {
    val daysLeft = try { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate)).toInt() } catch (e: Exception) { null }
    val urgencyLabel = when {
        daysLeft == null -> dueDate
        daysLeft < 0 -> "Overdue"
        daysLeft == 0 -> "Today"
        daysLeft == 1 -> "Tomorrow"
        else -> "${daysLeft}d left"
    }
    val urgencyColor = when {
        daysLeft == null -> MaterialTheme.colorScheme.onSurfaceVariant
        daysLeft <= 0 -> Error
        daysLeft <= 2 -> Warning
        else -> Success
    }

    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(urgencyColor))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("Due: $dueDate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = urgencyColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text(urgencyLabel, color = urgencyColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun EmptyStateCard(icon: ImageVector, title: String, subtitle: String, iconColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(iconColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ShimmerCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "shimmerAlpha")
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.5f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(DarkSurfaceHover))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(DarkSurfaceHover))
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(DarkSurfaceHover))
            }
        }
    }
}

@Composable
private fun AnnouncementsModal(isTeacher: Boolean, notices: List<Notice>, onDismiss: () -> Unit, onAddNotice: (String) -> Unit) {
    var newNoticeText by remember { mutableStateOf("") }
    var isAddingNotice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Notifications, null, tint = Primary, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Announcements", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isTeacher) {
                    OutlinedTextField(
                        value = newNoticeText, onValueChange = { newNoticeText = it },
                        placeholder = { Text("Write an announcement...") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = {
                            if (newNoticeText.isNotBlank()) {
                                isAddingNotice = true; onAddNotice(newNoticeText); isAddingNotice = false; newNoticeText = ""
                            }
                        },
                        enabled = newNoticeText.isNotBlank() && !isAddingNotice,
                        modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Post") }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (notices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("No announcements yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notices.size) { i ->
                            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Box(modifier = Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(Primary))
                                    Spacer(Modifier.width(10.dp))
                                    Text(notices[i].message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Primary, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun PrincipalBentoStats(studentCount: Int, teacherCount: Int, classCount: Int, averageAttendance: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top Row: Big Attendance Block + Classes
        Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Main Attendance Box
            Surface(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(BrandPrimary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("$averageAttendance%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Avg Attendance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Classes Box
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Emerald100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.DateRange, null, tint = Emerald500, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(classCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Active Classes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Bottom Row: Students & Faculty
        Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Students Box
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Sky100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Face, null, tint = Sky500, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(studentCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Staff Box
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Amber100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Star, null, tint = Amber500, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(teacherCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Staff", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PrincipalDashboardContent(
    user: User, 
    principalViewModel: PrincipalViewModel, 
    scrollState: ScrollState = rememberScrollState(),
    onNavigateToOverview: () -> Unit = {},
    onNavigateToManageStaff: () -> Unit = {},
    onNavigateToFeeTypes: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val approvePaymentViewModel: com.campussync.app.feature.fees.ApprovePaymentViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    LaunchedEffect(user.collegeId) {
        if (user.collegeId.isNotBlank()) {
            try {
                principalViewModel.loadDashboardStats(user.collegeId)
                principalViewModel.loadClasses(user.collegeId)
                approvePaymentViewModel.loadHistoryOrders(user.collegeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val studentCount by principalViewModel.studentCount.collectAsState()
    val teacherCount by principalViewModel.teacherCount.collectAsState()
    val classCount by principalViewModel.classCount.collectAsState()
    val averageAttendance by principalViewModel.averageAttendance.collectAsState()
    val classWiseAttendance by principalViewModel.classWiseAttendance.collectAsState()
    val isLoading by principalViewModel.isLoading.collectAsState()
    val recentActivities by principalViewModel.recentActivities.collectAsState()
    val upcomingEvents by principalViewModel.upcomingEvents.collectAsState()

    val historyOrders by approvePaymentViewModel.historyOrders.collectAsState()
    val verifiedRevenue = historyOrders.filter { it.status == "approved" }.sumOf { it.expectedTotal }

    var showAddEventDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 1. Executive Glass Header (Greeting + Principal Info + 4 Chips) ────
        PrincipalGlassHeader(
            user = user,
            studentCount = studentCount,
            teacherCount = teacherCount,
            classCount = classCount,
            campusName = user.collegeId.ifBlank { "Campus" },
            onSettings = onOpenSettings,
            onNotifications = onNotifications
        )
        Spacer(Modifier.height(16.dp))

        // ── 2. Executive 4-Card Stats Grid ────
        PrincipalExecutiveStatsGrid(
            studentCount = studentCount,
            teacherCount = teacherCount,
            classCount = classCount,
            averageAttendance = averageAttendance,
            verifiedRevenue = verifiedRevenue,
            onManageStaffClick = onNavigateToManageStaff,
            onOverviewClick = onNavigateToOverview,
            onFinanceClick = onNavigateToFeeTypes
        )
        Spacer(Modifier.height(20.dp))

        // ── 3. 7-Day Campus Attendance Interactive Bar Chart ────
        AdminWeeklyBarChartWidget(
            averageAttendance = averageAttendance
        )
        Spacer(Modifier.height(20.dp))

        // ── 4. Campus Attendance Standing Hero Gauge ────
        val attendanceStanding = if (averageAttendance > 0f) (averageAttendance / 100f).coerceIn(0.01f, 1f) else 0.82f
        AdminProgressCardWidget(
            label = "Campus Attendance Standing",
            progressValue = attendanceStanding,
            icon = Icons.Rounded.School,
            accentColor = if (averageAttendance >= 75f) Success else Amber500
        )
        Spacer(Modifier.height(20.dp))

        // ── 5. Class-Wise Performance Breakdown & Defaulter Alert ────
        PrincipalClassAnalyticsWidget(
            classWiseAttendance = classWiseAttendance,
            onViewDetailedOverview = onNavigateToOverview
        )
        Spacer(Modifier.height(20.dp))

        // ── 6. Campus Executive Operations Hub ────
        PrincipalOperationsHub(
            onManageStaffClick = onNavigateToManageStaff,
            onClassesOverviewClick = onNavigateToOverview,
            onFinanceClick = onNavigateToFeeTypes,
            onBroadcastNoticeClick = onNotifications
        )
        Spacer(Modifier.height(20.dp))

        // ── 7. Institutional Activity Timeline ────
        PrincipalActivityFeedWidget(
            activities = recentActivities
        )
        Spacer(Modifier.height(20.dp))

        // ── 8. Academic Events & Calendar ────
        PrincipalEventsWidget(
            events = upcomingEvents,
            onAddEventClick = { showAddEventDialog = true },
            onDeleteEventClick = { event ->
                principalViewModel.deleteEvent(user.collegeId, event.eventId) { result ->
                    result.onSuccess {
                        android.widget.Toast.makeText(context, "Event removed from schedule", android.widget.Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        android.widget.Toast.makeText(context, "Failed to remove event: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        Spacer(Modifier.height(40.dp))
    }

    if (showAddEventDialog) {
        AddEventDialog(
            isLoading = isLoading,
            onDismiss = { showAddEventDialog = false },
            onSubmit = { title, desc, date ->
                principalViewModel.createEvent(user.collegeId, title, desc, date, user.name) { result ->
                    result.onSuccess {
                        android.widget.Toast.makeText(context, "Event scheduled successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        showAddEventDialog = false
                    }.onFailure { err ->
                        android.widget.Toast.makeText(context, "Failed to schedule event: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 👑 PREMIUM BENTO ADMIN DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminDashboardContent(
    user: User,
    principalViewModel: com.campussync.app.feature.dashboard.PrincipalViewModel,
    onNavigateToFeeTypes: () -> Unit = {},
    onNavigateToAssignFee: () -> Unit = {},
    onNavigateToApprovePayments: () -> Unit = {},
    onNavigateToManageUsers: () -> Unit = {},
    onNavigateToVerifyInvites: () -> Unit = {},
    onNavigateToOverview: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onNavigateToDailySummary: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val adminVerificationViewModel: com.campussync.app.feature.admin.AdminVerificationViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val approvePaymentViewModel: com.campussync.app.feature.fees.ApprovePaymentViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    LaunchedEffect(user.collegeId) {
        if (user.collegeId.isNotBlank()) {
            try {
                adminVerificationViewModel.loadPendingUsers(user.collegeId)
                approvePaymentViewModel.loadPendingOrders(user.collegeId)
                approvePaymentViewModel.loadHistoryOrders(user.collegeId)
                principalViewModel.loadClasses(user.collegeId)
                principalViewModel.loadDashboardStats(user.collegeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val pendingUsers by adminVerificationViewModel.pendingUsers.collectAsState()
    val isUsersLoading by adminVerificationViewModel.isLoading.collectAsState()

    val pendingOrders by approvePaymentViewModel.pendingOrders.collectAsState()
    val historyOrders by approvePaymentViewModel.historyOrders.collectAsState()
    val isOrdersLoading by approvePaymentViewModel.isLoading.collectAsState()

    val isLoading by principalViewModel.isLoading.collectAsState()
    val studentCount by principalViewModel.studentCount.collectAsState()
    val classCount by principalViewModel.classCount.collectAsState()
    val averageAttendance by principalViewModel.averageAttendance.collectAsState()
    val classes by principalViewModel.classes.collectAsState()
    val classWiseAttendance by principalViewModel.classWiseAttendance.collectAsState()
    val recentActivities by principalViewModel.recentActivities.collectAsState()
    val upcomingEvents by principalViewModel.upcomingEvents.collectAsState()

    val totalPendingCount = pendingUsers.size + pendingOrders.size
    val verifiedRevenue = historyOrders.filter { it.status == "approved" }.sumOf { it.expectedTotal }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    val excelPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            principalViewModel.bulkUploadUsersFromExcel(context, uri, user.collegeId) { result ->
                result.onSuccess { count ->
                    android.widget.Toast.makeText(context, "Successfully registered $count users!", android.widget.Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    android.widget.Toast.makeText(context, "Upload failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 1. Glassmorphic Header (Greeting + Dynamic Admin Info + Summary Strip) ────
        AdminGlassHeader(
            user = user,
            studentCount = studentCount,
            pendingVerifications = totalPendingCount,
            campusName = user.collegeId.ifBlank { "Campus" },
            onSettings = onOpenSettings,
            onNotifications = onNotifications
        )
        Spacer(Modifier.height(16.dp))

        // ── 2. Stats Grid (3-col: Live Classes, Actions Needed, Verified Revenue) ────
        AdminStatsGridWidget(
            classCount = classCount,
            pendingCount = totalPendingCount,
            verifiedRevenue = verifiedRevenue,
            onClassesClick = { showCreateClassDialog = true },
            onPendingClick = onNavigateToVerifyInvites,
            onRevenueClick = onNavigateToApprovePayments
        )
        Spacer(Modifier.height(20.dp))

        // ── 3. Needs Attention — compact approvals launcher (lists live in
        //      their dedicated screens, keeping this dashboard light) ────
        AdminAttentionCard(
            pendingUsersCount = pendingUsers.size,
            pendingPaymentsCount = pendingOrders.size,
            isLoading = isUsersLoading || isOrdersLoading,
            onVerifyUsersClick = onNavigateToVerifyInvites,
            onApprovePaymentsClick = onNavigateToApprovePayments,
            onInviteUserClick = { showAddUserDialog = true },
            onBulkUploadClick = {
                try {
                    excelPickerLauncher.launch("*/*")
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Cannot open file picker", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        Spacer(Modifier.height(20.dp))

        // ── 4. Attendance Pulse (weekly chart with live average) ────
        AdminWeeklyBarChartWidget(
            averageAttendance = averageAttendance
        )
        Spacer(Modifier.height(20.dp))

        // ── 5. Management Hub — every admin tool one tap away ────
        AdminManagementHub(
            onManageStaff = onNavigateToManageUsers,
            onFeeTypes = onNavigateToFeeTypes,
            onAssignFees = onNavigateToAssignFee,
            onOverview = onNavigateToOverview,
            onAddEvent = { showAddEventDialog = true },
            onBroadcast = onNotifications,
            onDailySummary = onNavigateToDailySummary
        )
        Spacer(Modifier.height(20.dp))

        // ── 6. Institutional Activity Timeline ────
        PrincipalActivityFeedWidget(
            activities = recentActivities
        )
        Spacer(Modifier.height(20.dp))

        // ── 7. Academic Events & Calendar ────
        PrincipalEventsWidget(
            events = upcomingEvents,
            onAddEventClick = { showAddEventDialog = true },
            onDeleteEventClick = { event ->
                principalViewModel.deleteEvent(user.collegeId, event.eventId) { result ->
                    result.onSuccess {
                        android.widget.Toast.makeText(context, "Event removed from schedule", android.widget.Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        android.widget.Toast.makeText(context, "Failed to remove event: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        Spacer(Modifier.height(40.dp))
    }

    // Event scheduling (merged from Principal dashboard)
    if (showAddEventDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AddEventDialog(
            isLoading = isLoading,
            onDismiss = { showAddEventDialog = false },
            onSubmit = { title, desc, date ->
                principalViewModel.createEvent(user.collegeId, title, desc, date, user.name) { result ->
                    result.onSuccess {
                        android.widget.Toast.makeText(context, "Event scheduled successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        showAddEventDialog = false
                    }.onFailure { err ->
                        android.widget.Toast.makeText(context, "Failed to schedule event: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showAddUserDialog) {
        val classes by principalViewModel.classes.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current
        AddUserDialog(
            isLoading = isLoading, classes = classes,
            onDismiss = { showAddUserDialog = false },
            onSubmit = { role, email, name, rollNo, classId ->
                if (role == "admin") {
                    principalViewModel.createAdminInvite(email, name, user.collegeId) { result ->
                        result.onSuccess {
                            android.widget.Toast.makeText(context, "Admin invite created in 'invites' successfully!", android.widget.Toast.LENGTH_LONG).show()
                            showAddUserDialog = false
                        }.onFailure { err ->
                            android.widget.Toast.makeText(context, "Failed to invite Admin: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } else if (role == "student") {
                    principalViewModel.createStudentInvite(email, name, rollNo, classId, user.collegeId) { result ->
                        result.onSuccess {
                            android.widget.Toast.makeText(context, "Student invite created successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            showAddUserDialog = false
                        }.onFailure { err ->
                            android.widget.Toast.makeText(context, "Failed to invite Student: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    principalViewModel.createTeacherInvite(email, name, classId, user.collegeId) { result ->
                        result.onSuccess {
                            android.widget.Toast.makeText(context, "Teacher invite created successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            showAddUserDialog = false
                        }.onFailure { err ->
                            android.widget.Toast.makeText(context, "Failed to invite Teacher: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onBulkUpload = { uri ->
                principalViewModel.bulkUploadUsersFromExcel(context, uri, user.collegeId) { result ->
                    result.onSuccess { count ->
                        android.widget.Toast.makeText(context, "Successfully uploaded $count users!", android.widget.Toast.LENGTH_LONG).show()
                        showAddUserDialog = false
                    }.onFailure { err ->
                        android.widget.Toast.makeText(context, "Upload failed: ${err.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showCreateClassDialog) {
        CreateClassDialog(
            isLoading = isLoading, onDismiss = { showCreateClassDialog = false },
            onSubmit = { course, semester ->
                principalViewModel.createClass(user.collegeId, course, semester) { 
                    if (it.isSuccess) showCreateClassDialog = false 
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 PREMIUM BENTO COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PremiumAdminHeader(user: User, onSettings: () -> Unit, onNotifications: () -> Unit = {}) {
    val greeting = remember { 
        val h = LocalTime.now().hour
        when { h < 12 -> "Good Morning"; h < 17 -> "Good Afternoon"; else -> "Good Evening" } 
    }
    val emoji = remember { 
        val h = LocalTime.now().hour
        when { h < 12 -> "☀️"; h < 17 -> "⚡"; else -> "🌙" } 
    }
    val currentDate = remember { 
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) 
    }
    
    val cyberPurple = Color(0xFF8B5CF6)
    val electricBlue = Color(0xFF3B82F6)
    val neonPink = Color(0xFFFF1493)

    // The whole header is wrapped in a Box to act as a container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp) // Outer padding
    ) {
        // Inner Premium Floating Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp, 
                    shape = RoundedCornerShape(24.dp), 
                    spotColor = cyberPurple.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E1E2E).copy(alpha = 0.95f),
                            Color(0xFF151520).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 20.dp), // Inner padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 1. The Greeting Section (Left)
        Column {
            Text(
                text = "$emoji $greeting • $currentDate", 
                color = Color.LightGray.copy(alpha = 0.7f), 
                fontSize = 11.sp, 
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            val displayName = if (user.name.isBlank()) "Admin" else user.name
            Text(
                text = displayName, 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            
            Text(
                text = "System Administrator", 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(cyberPurple, electricBlue)
                    )
                )
            )
        }
        
        // Right Side: Actions and Profile
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            // 3. The Profile Avatar (Far Right)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = cyberPurple, ambientColor = cyberPurple)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cyberPurple.copy(alpha = 0.15f))
                    .clickable { onSettings() },
                contentAlignment = Alignment.Center
            ) {
                val initial = if(user.name.isNotBlank()) user.name.take(1).uppercase() else "A"
                Text(
                    text = initial, 
                    color = cyberPurple, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
            }
        }
    }
}

@Composable
fun AdminBentoStats(studentCount: Int, teacherCount: Int, classCount: Int) {
    val totalUsers = studentCount + teacherCount
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Bento Box (Total Users)
        Surface(
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            color = BrandPrimary,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.White.copy(alpha=0.15f), Color.Transparent), radius = 400f, center = Offset(0f, 0f))))
                Column(modifier = Modifier.padding(20.dp).align(Alignment.BottomStart)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(totalUsers.toString(), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Total Strength", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                }
            }
        }

        // Secondary Stacked Boxes
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Students Box
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Sky100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Face, null, tint = Sky500, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(studentCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Staff Box
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Amber100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Star, null, tint = Amber500, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(teacherCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Staff", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminActionGrid(onAddUser: () -> Unit, onCreateClass: () -> Unit, onManageUsers: () -> Unit, onVerifyInvites: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminActionCard("Add User", "Register profiles", Icons.Rounded.Person, Indigo500, Indigo100, Modifier.weight(1f), onAddUser)
            AdminActionCard("New Class", "Add new", Icons.Rounded.Add, Emerald500, Emerald100, Modifier.weight(1f), onCreateClass)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminActionCard("Manage", "Access control", Icons.Rounded.Settings, Rose500, Rose100, Modifier.weight(1f), onManageUsers)
            AdminActionCard("Verify", "Review invites", Icons.Rounded.CheckCircle, Sky500, Sky100, Modifier.weight(1f), onVerifyInvites)
        }
    }
}

@Composable
private fun AdminActionCard(title: String, subtitle: String, icon: ImageVector, iconTint: Color, iconBg: Color, modifier: Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumListActionRow(title: String, subtitle: String, icon: ImageVector, iconBg: Color, iconTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuickActionNeonCard(title: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.6f)))),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatCardAdmin(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.55f)))),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateClassDialog(isLoading: Boolean, onDismiss: () -> Unit, onSubmit: (course: String, semester: String) -> Unit) {
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        title = { Text("Create New Class", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course / Department") },
                    placeholder = { Text("e.g. BCA, BBA, MBA") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                // Semester chip selector (Sem 1–6)
                Text("Semester", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.campussync.app.core.components.SemesterChipRow(
                    selected = semester,
                    onSelect = { semester = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(course.trim(), semester.trim()) },
                enabled = !isLoading && course.isNotBlank() && semester.isNotBlank(),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Create")
            }
        },
        dismissButton = { if (!isLoading) TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserDialog(
    isLoading: Boolean, classes: List<com.campussync.app.feature.auth.CollegeClass>, onDismiss: () -> Unit,
    onSubmit: (role: String, email: String, name: String, rollNo: String, classId: String) -> Unit,
    onBulkUpload: (android.net.Uri) -> Unit = {}
) {
    var role by remember { mutableStateOf("student") }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onBulkUpload(uri)
        }
    }

    val selectedClassObj = classes.find { it.classId == selectedClassId }
    val selectedClassName = selectedClassObj?.let {
        if (it.course.isNotBlank()) listOf(it.course, "Sem ${it.semester}", it.className).filter { it.isNotBlank() }.joinToString(" - ") else it.className
    } ?: ""

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        title = { Text("Add New User", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = role == "admin", onClick = { role = "admin" }, label = { Text("Admin") })
                    FilterChip(selected = role == "teacher", onClick = { role = "teacher" }, label = { Text("Teacher") })
                    FilterChip(selected = role == "student", onClick = { role = "student" }, label = { Text("Student") })
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                if (role == "student") {
                    OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                }
                if (role != "admin") {
                    if (classes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Warning.copy(alpha = 0.12f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ No classes created yet. Please create a class first for students/teachers.",
                                fontSize = 12.sp,
                                color = Warning
                            )
                        }
                    } else {
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = selectedClassName, onValueChange = {}, readOnly = true, label = { Text("Assign to Class") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                classes.forEach { c ->
                                    val dName = if (c.course.isNotBlank()) listOf(c.course, "Sem ${c.semester}", c.className).filter { it.isNotBlank() }.joinToString(" - ") else c.className
                                    DropdownMenuItem(text = { Text(dName) }, onClick = { selectedClassId = c.classId; expanded = false })
                                }
                            }
                        }
                    }
                } else {
                    Text("Admins have college-wide access (no class assignment needed)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(2.dp))
                
                OutlinedButton(
                    onClick = { csvLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.List, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bulk Upload (Excel)")
                }
            }
        },
        confirmButton = {
            val isFormValid = when (role) {
                "admin" -> name.isNotBlank() && email.isNotBlank()
                "teacher" -> name.isNotBlank() && email.isNotBlank() && selectedClassId.isNotBlank()
                else -> name.isNotBlank() && email.isNotBlank() && rollNo.isNotBlank() && selectedClassId.isNotBlank()
            }
            Button(
                onClick = { onSubmit(role, email.trim(), name.trim(), rollNo.trim(), selectedClassId) },
                enabled = !isLoading && isFormValid,
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Create Invite")
            }
        },
        dismissButton = { if (!isLoading) TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ────────────────────────────── Charts & lists ──────────────────────────
@Composable
private fun AttendanceDonutChart(percentage: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Attendance Overview", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.DateRange, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 22f, cap = StrokeCap.Round))
                        drawArc(brush = Brush.sweepGradient(listOf(ChartTeal, ChartBlue, NeonPurple)), startAngle = -90f, sweepAngle = (percentage / 100f) * 360f, useCenter = false, style = Stroke(width = 22f, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$percentage%", color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Present", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ChartLegendItem("Present", NeonGreen)
                    ChartLegendItem("Absent", NeonRed)
                    ChartLegendItem("Leave", NeonBlue)
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun ClassAttendanceBarChart(data: Map<String, Int>, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Class-wise Attendance", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Surface(color = NeonBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text("Last 7 Days", color = NeonBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            val items = data.entries.toList().take(5)
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                items.forEach { entry ->
                    val pct = entry.value.coerceIn(0, 100)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                        Text("$pct%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(0.6f).height((pct * 0.8).dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(Brush.verticalGradient(listOf(NeonBlue, ChartBlue)))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(entry.key.take(4), fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentTrendChartMock(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Student Trend", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Surface(color = NeonPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text("Students", color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            val surfaceColor = MaterialTheme.colorScheme.surface
            Box(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = listOf(
                        Offset(0f, size.height * 0.8f), Offset(size.width * 0.25f, size.height * 0.6f),
                        Offset(size.width * 0.5f, size.height * 0.4f), Offset(size.width * 0.75f, size.height * 0.7f), Offset(size.width, size.height * 0.2f)
                    )
                    val path = Path().apply { moveTo(points.first().x, points.first().y); points.forEach { lineTo(it.x, it.y) } }
                    val fillPath = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                    drawPath(fillPath, brush = Brush.verticalGradient(listOf(NeonPurple.copy(alpha = 0.3f), Color.Transparent)))
                    drawPath(path, color = NeonPurple, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    points.forEach { drawCircle(color = surfaceColor, radius = 8f, center = it); drawCircle(color = NeonPurple, radius = 6f, center = it) }
                }
            }
        }
    }
}

@Composable
private fun RecentActivityList(activities: List<com.campussync.app.core.model.ActivityLog>, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Recent Activity", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (activities.isEmpty()) {
                Text("No recent activities.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                activities.forEach { log ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val (icon, color) = when (log.type) {
                            "fee" -> Icons.Rounded.ShoppingCart to NeonOrange
                            "class" -> Icons.Rounded.DateRange to NeonGreen
                            "user" -> Icons.Rounded.Person to NeonPurple
                            "event" -> Icons.Rounded.Star to NeonBlue
                            else -> Icons.Rounded.Info to Primary
                        }
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(log.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        val timeString = java.text.SimpleDateFormat("MMM dd", Locale.getDefault()).format(java.util.Date(log.timestamp))
                        Text(timeString, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun UpcomingEventsSection(events: List<com.campussync.app.core.model.Event>, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Upcoming Schedule", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onAddClick) { Text("+ Add Event", fontSize = 12.sp, color = NeonBlue, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(6.dp))
            if (events.isEmpty()) {
                Text("No upcoming events scheduled.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                events.forEach { event ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(NeonBlue.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            val cal = java.util.Calendar.getInstance(); cal.timeInMillis = event.eventDate
                            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                            val month = java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(month.uppercase(), fontSize = 9.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                                Text("$day", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(event.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("By: ${event.createdBy}", color = Primary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, date: Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Academic & Exam") }
    var venue by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("All Campus") }

    var selectedDateMillis by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        mutableStateOf(cal.timeInMillis)
    }

    val formattedDateText = remember(selectedDateMillis) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    val datePickerDialog = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                selectedDateMillis = picked.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000L
        }
    }

    val categories = listOf(
        "Academic & Exam" to Icons.Rounded.School,
        "Cultural Fest" to Icons.Rounded.Celebration,
        "Sports Meet" to Icons.Rounded.EmojiEvents,
        "Holiday" to Icons.Rounded.WbSunny,
        "Workshop" to Icons.Rounded.CoPresent,
        "Notice" to Icons.Rounded.Campaign
    )

    val audiences = listOf("All Campus", "Students Only", "Faculty Only")

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Schedule Campus Event", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Select date from calendar and notify campus", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Chips
                Column {
                    Text("Event Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (cat, icon) ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedCategory = cat }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Event Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title *") },
                    placeholder = { Text("e.g. Mid-Term Semester Examinations") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // ── Date Selector ──
                Column {
                    Text("Select Event Date *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = "Pick Date",
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = formattedDateText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Tap to choose from calendar",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Select Date",
                                    color = Primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quick Shortcut Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickPresets = listOf(
                            "Today" to 0,
                            "Tomorrow" to 1,
                            "In 3 Days" to 3,
                            "In 1 Week" to 7,
                            "In 2 Weeks" to 14,
                            "In 1 Month" to 30
                        )
                        quickPresets.forEach { (label, days) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val cal = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, days)
                                            set(Calendar.HOUR_OF_DAY, 9)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                        }
                                        selectedDateMillis = cal.timeInMillis
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Venue & Location
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Location (Optional)") },
                    placeholder = { Text("e.g. Main Auditorium / Ground / Room 402") },
                    leadingIcon = { Icon(Icons.Rounded.Place, contentDescription = null, tint = Primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Audience Selector
                Column {
                    Text("Audience", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        audiences.forEach { aud ->
                            val isSelected = audience == aud
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.dp, Primary) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { audience = aud }
                            ) {
                                Text(
                                    text = aud,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Details *") },
                    placeholder = { Text("Details, guidelines, or instructions for participants...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullTitle = if (selectedCategory.isNotBlank() && selectedCategory != "Notice") {
                        val categoryPrefix = when (selectedCategory) {
                            "Academic & Exam" -> "Exam"
                            "Cultural Fest" -> "Fest"
                            "Sports Meet" -> "Sports"
                            "Holiday" -> "Holiday"
                            "Workshop" -> "Workshop"
                            else -> "Notice"
                        }
                        "[$categoryPrefix] ${title.trim()}"
                    } else title.trim()

                    val fullDesc = buildString {
                        if (venue.isNotBlank()) append("📍 Venue: ${venue.trim()}\n")
                        if (audience.isNotBlank() && audience != "All Campus") append("👥 Target: $audience\n")
                        if (venue.isNotBlank() || (audience.isNotBlank() && audience != "All Campus")) append("\n")
                        append(desc.trim())
                    }

                    onSubmit(fullTitle, fullDesc, selectedDateMillis)
                },
                enabled = !isLoading && title.isNotBlank() && desc.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Publish Event", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}


