package com.campussync.app.feature.timetable

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.model.TimetableEntry
import com.campussync.app.core.components.*
import com.campussync.app.core.theme.*
import com.campussync.app.feature.timetable.TimetableViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    collegeId: String,
    classId: String,
    role: String,
    viewModel: TimetableViewModel,
    onOpenSettings: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // Resolve today's name (e.g. "Monday") to set it as default selection
    val todayName = remember {
        LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    }
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    var selectedDay by remember {
        mutableStateOf(if (days.contains(todayName)) todayName else "Monday")
    }

    // View filter: 0 = Day View, 1 = Week View
    var selectedViewMode by remember { mutableStateOf(0) }
    val viewModes = listOf("Day View", "Week View")

    LaunchedEffect(classId) {
        viewModel.loadTimetable(collegeId, classId)
    }

    val timetable = viewModel.timetable.value
    val isLoading = viewModel.isLoading.value

    // Filter entries based on Day View or Week View
    val filteredEntries = remember(timetable, selectedDay, selectedViewMode) {
        if (selectedViewMode == 0) {
            // Day View
            timetable.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
                .sortedBy { it.time }
        } else {
            // Week View: show all sorted by Day index and then Time
            val dayOrder = days.associateWith { days.indexOf(it) }
            timetable.sortedWith(compareBy({ dayOrder[it.dayOfWeek] ?: 99 }, { it.time }))
        }
    }

    val isTeacher = role.equals("teacher", ignoreCase = true)

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
                            text = "Class Schedule",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (selectedViewMode == 0) "$selectedDay's Lectures" else "Full Week Schedule",
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
                    modifier = Modifier.padding(bottom = 80.dp) // Offset above floating bottom bar
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Schedule")
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
            // View Mode Toggle (Day View / Week View)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant))
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    viewModes.forEachIndexed { idx, label ->
                        val isSelected = selectedViewMode == idx
                        val bgCol by animateColorAsState(if (isSelected) Primary else Color.Transparent)
                        val textCol by animateColorAsState(if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgCol)
                                .clickable { selectedViewMode = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textCol)
                        }
                    }
                }
            }

            // Day Selector tab row (only visible in Day View)
            AnimatedVisibility(
                visible = selectedViewMode == 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(days) { day ->
                        val isSelected = day == selectedDay
                        val tabBgColor = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant
                        val tabTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        val tabBorderColor = if (isSelected) Color.Transparent else (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant)

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedDay = day },
                            color = tabBgColor,
                            border = BorderStroke(1.dp, tabBorderColor)
                        ) {
                            Text(
                                text = day.take(3), // "Mon", "Tue" etc.
                                color = tabTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Cards Feed List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    LoadingState(modifier = Modifier.align(Alignment.Center))
                } else if (filteredEntries.isEmpty()) {
                    EmptyState(
                        text = if (selectedViewMode == 0) "No lectures scheduled for $selectedDay.\nEnjoy your day!"
                        else "No schedule entries loaded.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredEntries) { entry ->
                            LectureItemCard(
                                entry = entry,
                                isTeacher = isTeacher,
                                showDayHeader = selectedViewMode == 1,
                                isToday = entry.dayOfWeek.equals(todayName, ignoreCase = true),
                                onDelete = { viewModel.deleteTimetableEntry(collegeId, classId, entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimetableDialog(
            classId = classId,
            defaultDay = selectedDay,
            onDismiss = { showAddDialog = false },
            onSave = { entry ->
                viewModel.addTimetableEntry(collegeId, entry) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun LectureItemCard(
    entry: TimetableEntry,
    isTeacher: Boolean,
    showDayHeader: Boolean,
    isToday: Boolean,
    onDelete: () -> Unit
) {
    val hash = entry.subject.hashCode()
    val colorsList = listOf(Primary, Secondary, Accent, Color(0xFF10B981), Color(0xFFF59E0B))
    val subjectThemeColor = colorsList[Math.abs(hash) % colorsList.size]

    val outlineColor = if (isToday) subjectThemeColor.copy(alpha = 0.5f) else Color.Transparent

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDayHeader) {
            Text(
                text = entry.dayOfWeek,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isToday) outlineColor else (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visual bar matching subject color
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(subjectThemeColor)
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.subject,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                        if (isToday) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = subjectThemeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    color = subjectThemeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = entry.time,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Room 203",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Instructor: ${entry.teacherName}",
                        fontSize = 12.sp,
                        color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isTeacher) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete entry",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableDialog(
    classId: String,
    defaultDay: String,
    onDismiss: () -> Unit,
    onSave: (TimetableEntry) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(defaultDay) }

    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    var expandedDay by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Class Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Day Dropdown selection
                ExposedDropdownMenuBox(
                    expanded = expandedDay,
                    onExpandedChange = { expandedDay = !expandedDay }
                ) {
                    OutlinedTextField(
                        value = dayOfWeek,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false }
                    ) {
                        days.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    dayOfWeek = day
                                    expandedDay = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                // Time Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showStartTimePicker = true }) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Start Time") },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showEndTimePicker = true }) {
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("End Time") },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = teacherName,
                    onValueChange = { teacherName = it },
                    label = { Text("Teacher Name") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && teacherName.isNotBlank()) {
                        val timeString = "$startTime - $endTime"
                        onSave(
                            TimetableEntry(
                                classId = classId,
                                dayOfWeek = dayOfWeek,
                                subject = subject.trim(),
                                time = timeString,
                                teacherName = teacherName.trim()
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

    if (showStartTimePicker) {
        val state = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format(Locale.getDefault(), "%02d:%02d", state.hour, state.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = state)
        }
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = 10, initialMinute = 0)
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format(Locale.getDefault(), "%02d:%02d", state.hour, state.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = state)
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { content() },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(10.dp)) { Text("OK", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

