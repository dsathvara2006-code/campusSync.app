package com.campussync.app.feature.assignments

import com.campussync.app.core.model.StudentRow
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.model.Assignment
import com.campussync.app.core.components.*
import com.campussync.app.core.theme.*
import com.campussync.app.feature.assignments.AssignmentViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    collegeId: String,
    classId: String,
    role: String,
    userId: String,
    viewModel: AssignmentViewModel,
    onOpenSettings: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var assignmentIdForSubmissions by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filters: 0 = All, 1 = Active, 2 = Overdue
    var activeFilter by remember { mutableStateOf(0) }
    val filters = listOf("All", "Active", "Overdue")

    val isTeacher = role.equals("teacher", ignoreCase = true)

    LaunchedEffect(classId) {
        viewModel.loadAssignments(collegeId, classId)
        if (isTeacher) {
            viewModel.loadClassRoster(collegeId)
        }
    }

    val assignments by viewModel.assignments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val classRoster by viewModel.classRoster.collectAsState()

    // Filtering logic
    val filteredAssignments = remember(assignments, searchQuery, activeFilter) {
        val today = LocalDate.now()
        assignments.filter { assignment ->
            val matchesSearch = assignment.title.contains(searchQuery, ignoreCase = true) ||
                    assignment.description.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (activeFilter) {
                1 -> { // Active
                    try {
                        val due = LocalDate.parse(assignment.dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        !due.isBefore(today)
                    } catch (e: Exception) { true }
                }
                2 -> { // Overdue
                    try {
                        val due = LocalDate.parse(assignment.dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        due.isBefore(today)
                    } catch (e: Exception) { false }
                }
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(PrimaryDark, Primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background),
        startY = 0f, endY = 400f
    )

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(brush = gradientBg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assignments",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isTeacher) "Manage tasks for $classId" else "Your upcoming classroom tasks",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceVariant),
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTeacher) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.padding(bottom = 80.dp) // Offset above floating navigation
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "New Assignment")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title or description...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, null, tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp)
            )

            // Dynamic filter chips list
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters.size) { index ->
                    ModernChip(
                        text = filters[index],
                        selected = activeFilter == index,
                        onClick = { activeFilter = index }
                    )
                }
            }

            // Assignments cards feed list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    LoadingState(modifier = Modifier.align(Alignment.Center))
                } else if (filteredAssignments.isEmpty()) {
                    EmptyState(
                        text = if (searchQuery.isNotBlank() || activeFilter > 0) "No assignments match your search or filter."
                        else "No assignments posted yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items = filteredAssignments, key = { it.id }) { assignment ->
                            AssignmentItemCard(
                                assignment = assignment,
                                isTeacher = isTeacher,
                                userId = userId,
                                onViewSubmissions = { assignmentIdForSubmissions = assignment.id },
                                onDelete = { viewModel.deleteAssignment(collegeId, classId, assignment.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssignmentDialog(
            classId = classId,
            onDismiss = { showAddDialog = false },
            onSave = { assignment ->
                viewModel.addAssignment(collegeId, assignment) {
                    showAddDialog = false
                }
            }
        )
    }

    if (assignmentIdForSubmissions != null) {
        val currentAssignment = assignments.find { it.id == assignmentIdForSubmissions }
        if (currentAssignment != null) {
            SubmissionsDialog(
                assignment = currentAssignment,
                roster = classRoster,
                onDismiss = { assignmentIdForSubmissions = null },
                onToggleStatus = { studentId, isDone ->
                    viewModel.toggleAssignmentStatus(collegeId, classId, currentAssignment.id, studentId, isDone)
                }
            )
        } else {
            assignmentIdForSubmissions = null
        }
    }
}

@Composable
fun AssignmentItemCard(
    assignment: Assignment,
    isTeacher: Boolean,
    userId: String,
    onViewSubmissions: () -> Unit,
    onDelete: () -> Unit
) {
    val hash = assignment.title.hashCode()
    val subjectColors = listOf(Primary, Secondary, Accent, Color(0xFF10B981))
    val subjectColor = subjectColors[Math.abs(hash) % subjectColors.size]

    CustomCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Modern color badge for subject categorization
                    Surface(
                        color = subjectColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (assignment.title.contains(" ")) assignment.title.split(" ").first() else "Task",
                            color = subjectColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (!isTeacher && assignment.completedBy.contains(userId)) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Success.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "✅ Done",
                                color = Success,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isTeacher) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete assignment",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = assignment.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Renders URLs using native TextView to allow links clicks
            val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
            val linkColor = MaterialTheme.colorScheme.primary.toArgb()
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.widget.TextView(context).apply {
                        text = assignment.description
                        textSize = 14f
                        setTextColor(textColor)
                        setLinkTextColor(linkColor)
                        autoLinkMask = android.text.util.Linkify.WEB_URLS
                        movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    }
                },
                update = { view ->
                    view.text = assignment.description
                    view.setTextColor(textColor)
                    view.setLinkTextColor(linkColor)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daysLeftText = try {
                    val due = LocalDate.parse(assignment.dueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val today = LocalDate.now()
                    val diffInDays = ChronoUnit.DAYS.between(today, due).toInt()
                    val isCompleted = !isTeacher && assignment.completedBy.contains(userId)
                    
                    when {
                        isCompleted -> "Completed"
                        diffInDays < 0 -> "OVERDUE - MISSING"
                        diffInDays == 0 -> "Due Today!"
                        else -> "$diffInDays Days Left"
                    }
                } catch (e: Exception) {
                    assignment.dueDate
                }

                val errColor = MaterialTheme.colorScheme.error
                val daysLeftColor = when (daysLeftText) {
                    "OVERDUE - MISSING" -> errColor
                    "Completed" -> Success
                    "Due Today!" -> Color(0xFFF59E0B)
                    "" -> Color.Transparent
                    else -> Success
                }

                Column {
                    Text(
                        text = "Due: ${assignment.dueDate}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (daysLeftText.isNotEmpty()) {
                        Surface(
                            color = daysLeftColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = daysLeftText,
                                color = daysLeftColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val format = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                    val createdStr = format.format(Date(assignment.createdAt))
                    Text(
                        text = "Posted: $createdStr",
                        color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = if (isTeacher) 8.dp else 0.dp)
                    )
                    
                    if (isTeacher) {
                        Button(
                            onClick = onViewSubmissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Submissions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubmissionsDialog(
    assignment: Assignment,
    roster: List<StudentRow>,
    onDismiss: () -> Unit,
    onToggleStatus: (String, Boolean) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Submissions",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = assignment.title,
                    fontSize = 14.sp,
                    color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(roster) { student ->
                        val isDone = assignment.completedBy.contains(student.studentId)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = student.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = student.rollNo,
                                    fontSize = 12.sp,
                                    color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                )
                            }
                            androidx.compose.material3.Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleStatus(student.studentId, it) },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = Success
                                )
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssignmentDialog(
    classId: String,
    onDismiss: () -> Unit,
    onSave: (Assignment) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        dueDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Assignment Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Instructions") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Due Date") },
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.DateRange, contentDescription = "Select Date", tint = Primary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank() && dueDate.isNotBlank()) {
                        onSave(
                            Assignment(
                                classId = classId,
                                title = title.trim(),
                                description = description.trim(),
                                dueDate = dueDate
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

