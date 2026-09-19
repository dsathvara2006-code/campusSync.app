package com.campussync.app.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

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
    onNavigateToVerifyInvites: () -> Unit = {}
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
    val todayName = remember { LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US) }
    val todayDate = remember {
        val d = LocalDate.now()
        "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.US)}, ${d.year}"
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
        if (isAdmin) {
            AdminHeader(user = user, onOpenSettings = onOpenSettings, onBell = { showAnnouncementsDialog = true })
        } else {
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

        if (isPrincipal) {
            PrincipalDashboardContent(user = user, principalViewModel = principalViewModel, scrollState = scrollState)
        } else if (isAdmin) {
            AdminDashboardContent(
                user = user,
                principalViewModel = principalViewModel,
                onNavigateToFeeTypes = onNavigateToFeeTypes,
                onNavigateToAssignFee = onNavigateToAssignFee,
                onNavigateToApprovePayments = onNavigateToApprovePayments,
                onNavigateToManageUsers = onNavigateToManageUsers,
                onNavigateToVerifyInvites = onNavigateToVerifyInvites
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
                        ) { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
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

                IconButton(onClick = onBell, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                    Icon(Icons.Rounded.Notifications, contentDescription = "Announcements", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.name.take(1).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        ) { Icon(Icons.Rounded.Dashboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }

        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val roleName = user.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val displayName = if (user.name.equals("User", true) || user.name.isBlank()) "Admin" else user.name
            Text("Welcome, $displayName", color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$roleName Dashboard", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        IconButton(onClick = onBell, modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Icon(Icons.Rounded.Notifications, contentDescription = "Announcements", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonPurple, NeonBlue)))
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) { Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
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

// ════════════════════════════════════════════════════════════════════════
//  ADMIN / PRINCIPAL DASHBOARD
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun WelcomeBanner(user: User) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Indigo700, Indigo500, Violet500)))
                .padding(24.dp)
        ) {
            Column {
                val displayName = if (user.name.equals("User", true) || user.name.isBlank()) "Admin" else user.name
                Text("Welcome, $displayName 👋", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("Campus Management System", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(14.dp))
                Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text("Monitor • Manage • Grow", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
fun PrincipalDashboardContent(user: User, principalViewModel: PrincipalViewModel, scrollState: ScrollState) {
    val studentCount by principalViewModel.studentCount.collectAsState()
    val teacherCount by principalViewModel.teacherCount.collectAsState()
    val classCount by principalViewModel.classCount.collectAsState()
    val averageAttendance by principalViewModel.averageAttendance.collectAsState()
    val classWiseAttendance by principalViewModel.classWiseAttendance.collectAsState()
    val isLoading by principalViewModel.isLoading.collectAsState()
    val recentActivities by principalViewModel.recentActivities.collectAsState()
    val upcomingEvents by principalViewModel.upcomingEvents.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        WelcomeBanner(user = user)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardAdmin("Students", "$studentCount", Icons.Rounded.Groups, Indigo500, Modifier.weight(1f))
            StatCardAdmin("Faculty", "$teacherCount", Icons.Rounded.Star, Sky500, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardAdmin("Classes", "$classCount", Icons.Rounded.DateRange, Emerald500, Modifier.weight(1f))
            StatCardAdmin("Avg Attendance", "${averageAttendance.toInt()}%", Icons.Rounded.CheckCircle, Amber500, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AttendanceDonutChart(percentage = averageAttendance.toInt(), modifier = Modifier.fillMaxWidth())
            if (classWiseAttendance.isNotEmpty()) ClassAttendanceBarChart(classWiseAttendance, modifier = Modifier.fillMaxWidth())
            else StudentTrendChartMock(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RecentActivityList(activities = recentActivities, modifier = Modifier.fillMaxWidth())
            UpcomingEventsSection(events = upcomingEvents, onAddClick = { showAddEventDialog = true }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            isLoading = isLoading, onDismiss = { showAddEventDialog = false },
            onSubmit = { title, desc, date ->
                principalViewModel.createEvent(user.collegeId, title, desc, date, user.name) { result ->
                    if (result.isSuccess) showAddEventDialog = false
                }
            }
        )
    }
}

@Composable
fun AdminDashboardContent(
    user: User, principalViewModel: PrincipalViewModel,
    onNavigateToFeeTypes: () -> Unit = {}, onNavigateToAssignFee: () -> Unit = {},
    onNavigateToApprovePayments: () -> Unit = {}, onNavigateToManageUsers: () -> Unit = {},
    onNavigateToVerifyInvites: () -> Unit = {}
) {
    val isLoading by principalViewModel.isLoading.collectAsState()
    val classes by principalViewModel.classes.collectAsState()
    val studentCount by principalViewModel.studentCount.collectAsState()
    val teacherCount by principalViewModel.teacherCount.collectAsState()
    val classCount by principalViewModel.classCount.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showCreateClassDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        WelcomeBanner(user = user)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardAdmin("Total Students", "$studentCount", Icons.Rounded.Groups, Indigo500, Modifier.weight(1f))
            StatCardAdmin("Total Staff", "$teacherCount", Icons.Rounded.Star, Sky500, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardAdmin("Total Classes", "$classCount", Icons.Rounded.DateRange, Emerald500, Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(26.dp))
        SectionTitle(title = "👥 User Management")
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionNeonCard("Add New User", "Register students & staff", Icons.Rounded.PersonAdd, Indigo500, Modifier.weight(1f)) { showAddUserDialog = true }
            QuickActionNeonCard("Manage Users", "Deactivate & access control", Icons.Rounded.ManageAccounts, Rose500, Modifier.weight(1f)) { onNavigateToManageUsers() }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionNeonCard("Verify Invites", "Review pending invites", Icons.Rounded.CheckCircle, Sky500, Modifier.weight(1f)) { onNavigateToVerifyInvites() }
            QuickActionNeonCard("Create Class", "Add new academic batches", Icons.Rounded.Add, Emerald500, Modifier.weight(1f)) { showCreateClassDialog = true }
        }

        Spacer(Modifier.height(26.dp))
        SectionTitle(title = "💰 Financial Management")
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionNeonCard("Fee Templates", "Setup new fee structures", Icons.Rounded.Receipt, Amber500, Modifier.weight(1f)) { onNavigateToFeeTypes() }
            QuickActionNeonCard("Assign Fees", "Apply fees to classes", Icons.Rounded.AssignmentTurnedIn, Sky500, Modifier.weight(1f)) { onNavigateToAssignFee() }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionNeonCard("Approve Payments", "Verify student UTRs", Icons.Rounded.CheckCircle, Emerald500, Modifier.weight(1f)) { onNavigateToApprovePayments() }
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showAddUserDialog) {
        AddUserDialog(
            isLoading = isLoading, classes = classes, onDismiss = { showAddUserDialog = false },
            onSubmit = { role, email, name, rollNo, classId ->
                when (role) {
                    "admin" -> principalViewModel.createAdminInvite(email, name, user.collegeId) { result ->
                        result.onSuccess {
                            showAddUserDialog = false
                            android.widget.Toast.makeText(context, "Admin invite sent to $email", android.widget.Toast.LENGTH_LONG).show()
                        }.onFailure { e -> android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show() }
                    }
                    "student" -> principalViewModel.createStudentInvite(email, name, rollNo, classId, user.collegeId) { result ->
                        result.onSuccess {
                            showAddUserDialog = false
                            android.widget.Toast.makeText(context, "Invite sent to $email", android.widget.Toast.LENGTH_LONG).show()
                        }.onFailure { e -> android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show() }
                    }
                    else -> principalViewModel.createTeacherInvite(email, name, classId, user.collegeId) { result ->
                        result.onSuccess {
                            showAddUserDialog = false
                            android.widget.Toast.makeText(context, "Invite sent to $email", android.widget.Toast.LENGTH_LONG).show()
                        }.onFailure { e -> android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show() }
                    }
                }
            }
        )
    }

    if (showCreateClassDialog) {
        CreateClassDialog(
            isLoading = isLoading, onDismiss = { showCreateClassDialog = false },
            onSubmit = { course, semester, className ->
                principalViewModel.createClass(user.collegeId, course, semester, className) { result ->
                    if (result.isSuccess) showCreateClassDialog = false
                }
            }
        )
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

@Composable
private fun CreateClassDialog(isLoading: Boolean, onDismiss: () -> Unit, onSubmit: (course: String, semester: String, className: String) -> Unit) {
    var course by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        title = { Text("Create New Class", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Course / Department") }, placeholder = { Text("e.g. BCA, BBA, MBA") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") }, placeholder = { Text("e.g. 1, 2, 3") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Division / Batch") }, placeholder = { Text("e.g. Div A, Morning Batch") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(course.trim(), semester.trim(), className.trim()) },
                enabled = !isLoading && course.isNotBlank() && semester.isNotBlank() && className.isNotBlank(),
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
    onSubmit: (role: String, email: String, name: String, rollNo: String, classId: String) -> Unit
) {
    var role by remember { mutableStateOf("student") }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val selectedClassObj = classes.find { it.classId == selectedClassId }
    val selectedClassName = selectedClassObj?.let {
        if (it.course.isNotBlank()) "${it.course} - Sem ${it.semester} - ${it.className}" else it.className
    } ?: ""

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        title = { Text("Add New User", fontWeight = FontWeight.ExtraBold) },
        text = {
            if (classes.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Warning, null, tint = Warning, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No Classes Found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Create a class first.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
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
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = selectedClassName, onValueChange = {}, readOnly = true, label = { Text("Assign to Class") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                classes.forEach { c ->
                                    val dName = if (c.course.isNotBlank()) "${c.course} - Sem ${c.semester} - ${c.className}" else c.className
                                    DropdownMenuItem(text = { Text(dName) }, onClick = { selectedClassId = c.classId; expanded = false })
                                }
                            }
                        }
                    } else {
                        Text("Admins have college-wide access (no class assignment needed)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            if (classes.isNotEmpty()) {
                Button(
                    onClick = { onSubmit(role, email.trim(), name.trim(), rollNo.trim(), selectedClassId) },
                    enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && (role == "admin" || selectedClassId.isNotBlank()) && (role == "teacher" || role == "admin" || rollNo.isNotBlank()),
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Create")
                }
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
                            "fee" -> Icons.Rounded.AccountBalanceWallet to NeonOrange
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
private fun AddEventDialog(isLoading: Boolean, onDismiss: () -> Unit, onSubmit: (title: String, desc: String, date: Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var daysFromNow by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(22.dp),
        title = { Text("Schedule Event", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, placeholder = { Text("e.g. Annual Tech Fest") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 2)
                OutlinedTextField(value = daysFromNow, onValueChange = { if (it.all { c -> c.isDigit() }) daysFromNow = it }, label = { Text("Days from today") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysFromNow.toIntOrNull() ?: 1
                    onSubmit(title.trim(), desc.trim(), System.currentTimeMillis() + (days * 86400000L))
                },
                enabled = !isLoading && title.isNotBlank() && desc.isNotBlank() && daysFromNow.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text("Add")
            }
        },
        dismissButton = { if (!isLoading) TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
