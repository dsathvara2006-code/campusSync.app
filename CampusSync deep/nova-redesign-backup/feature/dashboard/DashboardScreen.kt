package com.campussync.app.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.*
import com.campussync.app.feature.assignments.AssignmentScreen
import com.campussync.app.feature.attendance.AttendanceMarkScreen
import com.campussync.app.feature.attendance.AttendanceViewScreen
import com.campussync.app.feature.timetable.TimetableScreen
import com.campussync.app.feature.assignments.AssignmentViewModel
import com.campussync.app.feature.attendance.AttendanceViewModel
import com.campussync.app.feature.timetable.TimetableViewModel

/**
 * Container router — unchanged branching logic (Principal/Admin get the
 * full-bleed dashboard, Teacher/Student get the 4-tab shell). Only the
 * bottom navigation visual changed: a floating pill with a solid active
 * indicator instead of the previous glass bar.
 */
@Composable
fun DashboardScreen(
    user: User,
    classId: String,
    attendanceViewModel: AttendanceViewModel,
    timetableViewModel: TimetableViewModel,
    assignmentViewModel: AssignmentViewModel,
    onOpenSettings: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToFeeTypes: () -> Unit = {},
    onNavigateToAssignFee: () -> Unit = {},
    onNavigateToStudentFees: () -> Unit = {},
    onNavigateToApprovePayments: () -> Unit = {},
    onNavigateToManageUsers: () -> Unit = {},
    onNavigateToVerifyInvites: () -> Unit = {}
) {
    val isPrincipal = user.role.equals("principal", ignoreCase = true)
    val isAdmin = user.role.equals("admin", ignoreCase = true)

    if (isPrincipal || isAdmin) {
        HomeScreen(
            user = user, classId = classId,
            attendanceViewModel = attendanceViewModel, timetableViewModel = timetableViewModel, assignmentViewModel = assignmentViewModel,
            onNavigateToTab = { /* Principal/Admin doesn't have tabs */ },
            onOpenSettings = onOpenSettings, onNavigateToResources = onNavigateToResources,
            onNavigateToFeeTypes = onNavigateToFeeTypes, onNavigateToAssignFee = onNavigateToAssignFee,
            onNavigateToStudentFees = onNavigateToStudentFees, onNavigateToApprovePayments = onNavigateToApprovePayments,
            onNavigateToManageUsers = onNavigateToManageUsers, onNavigateToVerifyInvites = onNavigateToVerifyInvites
        )
    } else {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Home", "Attendance", "Timetable", "Tasks")
        val icons = listOf(Icons.Rounded.Home, Icons.Rounded.CheckCircle, Icons.Rounded.DateRange, Icons.Rounded.Edit)

        Scaffold(
            bottomBar = {
                NovaBottomNav(tabs = tabs, icons = icons, selected = selectedTab, onSelect = { selectedTab = it })
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        user = user, classId = classId,
                        attendanceViewModel = attendanceViewModel, timetableViewModel = timetableViewModel, assignmentViewModel = assignmentViewModel,
                        onNavigateToTab = { index -> selectedTab = index }, onOpenSettings = onOpenSettings,
                        onNavigateToResources = onNavigateToResources, onNavigateToFeeTypes = onNavigateToFeeTypes,
                        onNavigateToAssignFee = onNavigateToAssignFee, onNavigateToStudentFees = onNavigateToStudentFees,
                        onNavigateToApprovePayments = onNavigateToApprovePayments
                    )
                    1 -> if (user.role.equals("teacher", ignoreCase = true)) {
                        AttendanceMarkScreen(collegeId = user.collegeId, classId = classId, viewModel = attendanceViewModel, onOpenSettings = onOpenSettings)
                    } else {
                        AttendanceViewScreen(collegeId = user.collegeId, classId = classId, studentId = user.userId, viewModel = attendanceViewModel, onOpenSettings = onOpenSettings)
                    }
                    2 -> TimetableScreen(collegeId = user.collegeId, classId = classId, role = user.role, viewModel = timetableViewModel, onOpenSettings = onOpenSettings)
                    3 -> AssignmentScreen(collegeId = user.collegeId, classId = classId, role = user.role, userId = user.userId, viewModel = assignmentViewModel, onOpenSettings = onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun NovaBottomNav(tabs: List<String>, icons: List<androidx.compose.ui.graphics.vector.ImageVector>, selected: Int, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(68.dp).shadow(14.dp, RoundedCornerShape(26.dp), clip = false),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selected == index
                    val scale by animateFloatAsState(if (isSelected) 1f else 0.94f, spring(Spring.StiffnessLow), label = "navScale")

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .then(
                                if (isSelected) Modifier.background(Brush.linearGradient(listOf(Indigo500, Violet500)))
                                else Modifier
                            )
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(index) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icons[index], contentDescription = title,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            AnimatedVisibility(visible = isSelected, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                                Text(
                                    title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
