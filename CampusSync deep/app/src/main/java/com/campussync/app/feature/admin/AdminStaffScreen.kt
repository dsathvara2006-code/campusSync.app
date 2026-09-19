package com.campussync.app.feature.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campussync.app.core.components.GlassCard
import com.campussync.app.core.components.GlassChip
import com.campussync.app.core.components.GlassEmptyState
import com.campussync.app.core.components.GlassMonogram
import com.campussync.app.core.components.GlassPill
import com.campussync.app.core.components.GlassScreen
import com.campussync.app.core.components.glassHairline
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.Error
import com.campussync.app.core.theme.Info
import com.campussync.app.core.theme.Primary
import com.campussync.app.core.theme.Secondary
import com.campussync.app.core.theme.Success
import com.campussync.app.core.theme.Warning

// Hairline helper
private fun hairline(isDark: Boolean): Color =
    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

@Composable
fun CleanUserListItem(
    user: User,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit = {},
    onAssignSubjectClick: (() -> Unit)? = null,
    onPromoteAdminClick: (() -> Unit)? = null
) {
    val roleAccent = when (user.role.lowercase()) {
        "teacher" -> Info
        "admin", "tenant_admin" -> Warning
        "principal" -> Secondary
        else -> Primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(androidx.compose.foundation.isSystemInDarkTheme())),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(roleAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = roleAccent,
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(roleAccent.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = user.role.replaceFirstChar { it.uppercase() },
                                color = roleAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (user.isActive) "• Active" else "• Deactivated",
                        color = if (user.isActive) Success else Error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Activate / Deactivate tile
                IconButton(
                    onClick = { if (user.isActive) onDeactivateClick() else onActivateClick() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (user.isActive) Error.copy(alpha = 0.1f) else Success.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = if (user.isActive) Icons.Rounded.Close else Icons.Rounded.Check,
                        contentDescription = if (user.isActive) "Deactivate" else "Activate",
                        tint = if (user.isActive) Error else Success,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Action Buttons
            val hasAssignSubject = user.isActive && user.role.equals("teacher", ignoreCase = true) && onAssignSubjectClick != null
            val hasMakeAdmin = user.isActive && onPromoteAdminClick != null

            if (hasAssignSubject || hasMakeAdmin) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = hairline(androidx.compose.foundation.isSystemInDarkTheme()))
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasAssignSubject) {
                        Button(
                            onClick = { onAssignSubjectClick?.invoke() },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary.copy(alpha = 0.1f),
                                contentColor = Primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = null
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Assign Subjects", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (hasMakeAdmin) {
                        Button(
                            onClick = { onPromoteAdminClick?.invoke() },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Warning.copy(alpha = 0.1f),
                                contentColor = Warning
                            ),
                            shape = RoundedCornerShape(10.dp),
                            elevation = null
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Make Admin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffScreen(
    currentUser: User,
    onBack: () -> Unit,
    viewModel: AdminStaffViewModel = viewModel()
) {
    val activeStaff by viewModel.activeStaff.collectAsState()
    val deactivatedStaff by viewModel.deactivatedStaff.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedTeacherForSubjects by remember { mutableStateOf<User?>(null) }
    var userToPromote by remember { mutableStateOf<User?>(null) }
    var showInviteAdminDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(currentUser.collegeId) {
        viewModel.loadStaff(currentUser.collegeId)
        viewModel.loadClasses(currentUser.collegeId)
    }

    val canInvite = currentUser.role.equals("principal", true) || currentUser.role.equals("admin", true)

    GlassScreen(
        title = "Manage Staff",
        subtitle = "${activeStaff.size} active  •  ${deactivatedStaff.size} deactivated",
        onBack = onBack,
        topBarActions = {
            if (canInvite) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Primary, Secondary)))
                        .clickable { showInviteAdminDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add, contentDescription = "Invite Admin",
                        tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented tab control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(5.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "Active Staff" to Primary,
                        "Deactivated" to Warning
                    ).forEachIndexed { index, (label, color) ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface
                                    else Color.Transparent
                                )
                                .clickable { selectedTabIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedTabIndex == 0,
                        enter = fadeIn(animationSpec = spring()),
                        exit = fadeOut(animationSpec = spring())
                    ) {
                        val activeTeachers = activeStaff.filter { !it.role.equals("student", true) }
                        val activeStudents = activeStaff.filter { it.role.equals("student", true) }

                        if (activeStaff.isEmpty()) {
                            GlassEmptyState(
                                icon = Icons.Rounded.People,
                                title = "No active users",
                                subtitle = "Invite teachers and students to get started.",
                                accent = Primary
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (activeTeachers.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Teachers & Admin",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                        )
                                    }
                                    items(activeTeachers) { user ->
                                        CleanUserListItem(
                                            user = user,
                                            onDeactivateClick = {
                                                viewModel.deactivateUser(user.userId, user.role, currentUser.collegeId) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onAssignSubjectClick = if (user.role.equals("teacher", true)) {
                                                {
                                                    selectedTeacherForSubjects = user
                                                    viewModel.loadTeacherSubjects(currentUser.collegeId, user.userId)
                                                }
                                            } else null,
                                            onPromoteAdminClick = if ((currentUser.role.equals("principal", true) || currentUser.role.equals("admin", true)) && user.role.equals("teacher", true)) {
                                                { userToPromote = user }
                                            } else null
                                        )
                                    }
                                }

                                if (activeStudents.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Students",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp, start = 2.dp)
                                        )
                                    }
                                    items(activeStudents) { user ->
                                        CleanUserListItem(
                                            user = user,
                                            onDeactivateClick = {
                                                viewModel.deactivateUser(user.userId, user.role, currentUser.collegeId) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onAssignSubjectClick = null
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedTabIndex == 1,
                        enter = fadeIn(animationSpec = spring()),
                        exit = fadeOut(animationSpec = spring())
                    ) {
                        val deactiveTeachers = deactivatedStaff.filter { !it.role.equals("student", true) }
                        val deactiveStudents = deactivatedStaff.filter { it.role.equals("student", true) }

                        if (deactivatedStaff.isEmpty()) {
                            GlassEmptyState(
                                icon = Icons.Rounded.PersonOff,
                                title = "No deactivated users",
                                subtitle = "Deactivated accounts will appear here for re-activation.",
                                accent = Warning
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (deactiveTeachers.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Teachers & Admin",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                        )
                                    }
                                    items(deactiveTeachers) { user ->
                                        CleanUserListItem(
                                            user = user,
                                            onDeactivateClick = {},
                                            onActivateClick = {
                                                viewModel.activateUser(user.userId, user.role, currentUser.collegeId) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }

                                if (deactiveStudents.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Students",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp, start = 2.dp)
                                        )
                                    }
                                    items(deactiveStudents) { user ->
                                        CleanUserListItem(
                                            user = user,
                                            onDeactivateClick = {},
                                            onActivateClick = {
                                                viewModel.activateUser(user.userId, user.role, currentUser.collegeId) { success, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (userToPromote != null) {
        AlertDialog(
            onDismissRequest = { userToPromote = null },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Promote to Admin", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to promote ${userToPromote?.name} to Admin? They will have full access to manage students, teachers, and settings.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val user = userToPromote!!
                        userToPromote = null
                        viewModel.promoteToAdmin(user, currentUser.collegeId) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Confirm", color = Warning, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { userToPromote = null }) { Text("Cancel") }
            }
        )
    }

    if (showInviteAdminDialog) {
        var inviteEmail by remember { mutableStateOf("") }
        var inviteName by remember { mutableStateOf("") }
        var isInviting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isInviting) showInviteAdminDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("Invite Admin", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the name and email of the person you want to invite as an Admin. When they log in with this email, they will automatically be granted Admin access.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = inviteName,
                        onValueChange = { inviteName = it },
                        label = { Text("Admin Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteEmail.isNotBlank() && inviteName.isNotBlank()) {
                            isInviting = true
                            viewModel.inviteAdmin(inviteEmail, inviteName, currentUser.collegeId) { success, message ->
                                isInviting = false
                                showInviteAdminDialog = false
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isInviting,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isInviting) CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White, strokeWidth = 2.dp
                    )
                    else Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteAdminDialog = false }, enabled = !isInviting) { Text("Cancel") }
            }
        )
    }

    // Modal Bottom Sheet for Subject Assignment
    if (selectedTeacherForSubjects != null) {
        val teacher = selectedTeacherForSubjects!!
        val teacherSubjects by viewModel.teacherSubjects.collectAsState()
        val classes by viewModel.classes.collectAsState()

        var selectedClass by remember { mutableStateOf<com.campussync.app.feature.attendance.ClassOverview?>(null) }
        var isClassDropdownExpanded by remember { mutableStateOf(false) }

        var subjectNameInput by remember { mutableStateOf("") }
        var semesterInput by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf("") }
        var isAssigning by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { selectedTeacherForSubjects = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassMonogram(name = teacher.name, accent = Primary, size = 42, fontSize = 16)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Assign Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "to ${teacher.name}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))

                ExposedDropdownMenuBox(
                    expanded = isClassDropdownExpanded,
                    onExpandedChange = { isClassDropdownExpanded = !isClassDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClass?.name ?: "Select Class",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isClassDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isClassDropdownExpanded,
                        onDismissRequest = { isClassDropdownExpanded = false }
                    ) {
                        if (classes.isEmpty()) {
                            DropdownMenuItem(text = { Text("No classes available") }, onClick = { isClassDropdownExpanded = false })
                        } else {
                            classes.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedClass = c
                                        semesterInput = c.semester
                                        isClassDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = subjectNameInput,
                        onValueChange = { subjectNameInput = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Semester", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.campussync.app.core.components.SemesterChipRow(
                    selected = semesterInput,
                    onSelect = { semesterInput = it }
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedClass != null && subjectNameInput.isNotBlank()) {
                            isAssigning = true
                            viewModel.addSubjectAssignment(
                                currentUser.collegeId, selectedClass!!.classId, teacher.userId, teacher.name, subjectNameInput, semesterInput
                            ) { success, resultMsg ->
                                isAssigning = false
                                msg = resultMsg
                                if (success) {
                                    selectedClass = null
                                    subjectNameInput = ""
                                    semesterInput = ""
                                }
                            }
                        } else {
                            msg = "Class and Subject Name are required."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isAssigning,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isAssigning) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Assign Subject", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (msg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = if (msg.contains("success", true)) Success else Error,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    "Currently Assigned Subjects",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (teacherSubjects.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No subjects assigned yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(teacherSubjects) { subj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, glassHairline(), RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Primary)
                                )
                                Spacer(Modifier.width(11.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        subj.subjectName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Class: ${subj.classId}  •  Sem: ${subj.semester}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSubjectAssignment(currentUser.collegeId, teacher.userId, subj.id) { _, _ -> } },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Error.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete, contentDescription = "Remove",
                                        tint = Error, modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
