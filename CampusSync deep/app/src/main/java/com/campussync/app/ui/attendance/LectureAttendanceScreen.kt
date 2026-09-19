package com.campussync.app.feature.attendance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
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
import com.campussync.app.core.components.*
import com.campussync.app.core.theme.*
import com.campussync.app.feature.attendance.AttendanceViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun LectureAttendanceScreen(
    collegeId: String,
    classId: String,
    teacherId: String,
    subjectName: String,
    viewModel: AttendanceViewModel,
    onOpenSettings: (() -> Unit)? = null,
    onBack: () -> Unit = {}
) {
    var showCalendar by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateString = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val currentYearMonth = YearMonth.from(selectedDate).format(DateTimeFormatter.ofPattern("yyyy-MM"))

    var submitMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Holiday ViewModel
    val holidayViewModel: com.campussync.app.core.viewmodel.HolidayViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val holidayDates by holidayViewModel.holidayDates.collectAsState()

    LaunchedEffect(collegeId, currentYearMonth) {
        holidayViewModel.listenToHolidays(collegeId)
    }

    // Load data
    LaunchedEffect(classId, currentYearMonth) {
        viewModel.loadMarkedDatesForMonth(collegeId, classId, currentYearMonth)
    }

    LaunchedEffect(selectedDate, classId, subjectName) {
        submitMessage = null
        viewModel.loadAttendanceForDate(collegeId, classId, dateString, subjectName)
    }

    val students by viewModel.students.collectAsState()
    val markedDates by viewModel.markedDates.collectAsState()
    val isLoadingDate by viewModel.isLoadingDate.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isDateMarked = markedDates[dateString] == true

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.rollNo.contains(searchQuery, ignoreCase = true)
        }
    }

    val presentCount = students.count { it.status == "present" }
    val absentCount = students.count { it.status == "absent" }
    val unmarkedCount = students.count { it.status.isBlank() }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(PrimaryDark, Primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.background),
        startY = 0f, endY = 500f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Space for bottom save bar
                .verticalScroll(rememberScrollState())
        ) {
            // Modern Header Bar
            HeaderBar(
                title = "$subjectName Lecture",
                selectedDate = selectedDate,
                showCalendar = showCalendar,
                isDateMarked = isDateMarked,
                onToggleCalendar = { showCalendar = !showCalendar },
                onOpenSettings = onOpenSettings,
                onBack = onBack,
                onExportExcel = {
                    try {
                        val exportsDir = java.io.File(context.cacheDir, "exports")
                        if (!exportsDir.exists()) exportsDir.mkdirs()

                        // Use Excel Export
                        val xlsxFile = java.io.File(exportsDir, "Attendance_${classId}_${dateString}.xlsx")
                        val rows = mutableListOf<List<String>>()
                        rows.add(listOf("Roll No", "Name", "Status"))
                        students.forEach {
                            rows.add(listOf(it.rollNo, it.name, if (it.status.isBlank()) "Absent" else it.status))
                        }
                        java.io.FileOutputStream(xlsxFile).use { fos ->
                            com.campussync.app.core.utils.XlsxWriter.write(fos, rows)
                        }
                        var file = xlsxFile
                        var mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        var shareTitle = "Share Attendance Excel"

                        file?.let { exportFile ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                exportFile
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = mimeType
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, shareTitle))
                        }
                    } catch (e: Throwable) {
                        // If even CSV or FileProvider fails, show feedback to the user
                        android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                        submitMessage = "Export failed"
                    }
                }
            )

            // Animated Calendar dropdown
            AnimatedVisibility(
                visible = showCalendar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AttendanceCalendar(
                    selectedDate = selectedDate,
                    markedDates = markedDates,
                    holidayDates = holidayDates,
                    onMonthChange = { newYearMonth ->
                        viewModel.loadMarkedDatesForMonth(collegeId, classId, newYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    },
                    onDateSelected = { date ->
                        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val isSunday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY
                        val isHoliday = holidayDates.contains(dateStr)
                        if (!isSunday && !isHoliday) {
                            selectedDate = date
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Search and quick stats card
            CustomCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student or roll number...", fontSize = 13.sp) },
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
                            .height(48.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.markAllPresent() },
                        colors = ButtonDefaults.buttonColors(containerColor = Success.copy(alpha = 0.15f), contentColor = Success),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        elevation = null
                    ) {
                        Text("Mark All Present", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.markAllAbsent() },
                        colors = ButtonDefaults.buttonColors(containerColor = Error.copy(alpha = 0.15f), contentColor = Error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                        elevation = null
                    ) {
                        Text("Mark All Absent", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatSummaryChip(label = "Total", value = students.size, color = Primary)
                    StatSummaryChip(label = "Present", value = presentCount, color = Success)
                    StatSummaryChip(label = "Absent", value = absentCount, color = Error)
                    StatSummaryChip(label = "Unmarked", value = unmarkedCount, color = Warning)
                }
            }

            Spacer(Modifier.height(6.dp))

            // Student Roster Card
            CustomCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                elevation = 2.dp
            ) {
                Text(
                    text = "Class Roster",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoadingDate) {
                    LoadingState()
                } else if (filteredStudents.isEmpty()) {
                    EmptyState(text = if (searchQuery.isNotBlank()) "No students match '$searchQuery'" else "No students enrolled in this class.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filteredStudents.forEach { student ->
                            StudentAttendanceRow(
                                name = student.name,
                                rollNo = student.rollNo,
                                status = student.status,
                                onPresent = { viewModel.markStatus(student.studentId, "present") },
                                onAbsent = { viewModel.markStatus(student.studentId, "absent") }
                            )
                        }
                    }
                }
            }
        }

        // Fixed Save Bar at the bottom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .shadow(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                submitMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = if (msg.startsWith("Saved")) Success.copy(alpha = 0.12f) else Error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = msg,
                            color = if (msg.startsWith("Saved")) Success else Error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                PrimaryButton(
                    text = if (isDateMarked) "Update $subjectName Attendance" else "Submit $subjectName Attendance",
                    onClick = {
                        viewModel.submitAttendance(collegeId, classId, dateString, subjectName, teacherId) { success, error ->
                            submitMessage = if (success) "Saved attendance successfully!"
                            else "Error saving: $error"
                        }
                    },
                    isLoading = isSubmitting,
                    enabled = students.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeaderBar(
    title: String,
    selectedDate: LocalDate,
    showCalendar: Boolean,
    isDateMarked: Boolean,
    onToggleCalendar: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onBack: () -> Unit = {},
    onExportExcel: () -> Unit
) {
    val displayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(end = 8.dp).size(32.dp)
        ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                ModernBadge(
                    text = if (isDateMarked) "Saved" else "Pending",
                    containerColor = if (isDateMarked) NeonGreen else NeonOrange
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = selectedDate.format(displayFmt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        IconButton(
            onClick = onExportExcel,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceVariant),
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Rounded.Share, contentDescription = "Export Excel", modifier = Modifier.size(20.dp))
        }
        
        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = onToggleCalendar,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (showCalendar) Primary else (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceVariant),
                contentColor = if (showCalendar) Color.White else MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Rounded.DateRange, contentDescription = "Toggle Calendar", modifier = Modifier.size(20.dp))
        }

        if (onOpenSettings != null) {
            Spacer(Modifier.width(8.dp))
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

@Composable
private fun AttendanceCalendar(
    selectedDate: LocalDate,
    markedDates: Map<String, Boolean>,
    holidayDates: Set<String> = emptySet(),
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }
    
    Surface(
        modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, (if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Calendar Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                    onMonthChange(currentMonth)
                }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Month", tint = Primary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.year,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                currentMonth = YearMonth.now()
                                onMonthChange(currentMonth)
                                onDateSelected(today)
                            },
                        color = Primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "Today",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                    onMonthChange(currentMonth)
                }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Month", tint = Primary)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Weekday labels
            val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Grid Days Calculation
            val firstDay = currentMonth.atDay(1)
            val startOffset = firstDay.dayOfWeek.value % 7
            val daysInMonth = currentMonth.lengthOfMonth()
            val cells = startOffset + daysInMonth
            val rows = (cells + 6) / 7

            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.1f))
                        } else {
                            val date = currentMonth.atDay(dayNum)
                            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            val isFuture = date.isAfter(today)
                            val hasRecord = markedDates[dateStr] == true

                            val isSunday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY
                            val isHoliday = holidayDates.contains(dateStr)
                            val isExcluded = isSunday || isHoliday

                            val cellBgColor = when {
                                isSelected && !isExcluded -> Primary
                                isExcluded -> Color(0xFFDC2626).copy(alpha = 0.15f)
                                isToday -> Primary.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                            val cellTextColor = when {
                                isSelected && !isExcluded -> Color.White
                                isExcluded -> Color(0xFFDC2626)
                                isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cellBgColor)
                                    .then(
                                        if (isToday && !isSelected && !isExcluded) Modifier.border(1.5.dp, Primary, RoundedCornerShape(12.dp))
                                        else if (isExcluded) Modifier.border(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                                    .clickable(enabled = !isFuture && !isExcluded) { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 13.sp,
                                        fontWeight = if ((isSelected && !isExcluded) || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = cellTextColor
                                    )
                                    if (isExcluded && !isFuture) {
                                        // Holiday/Sunday indicator
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(NeonRed)
                                        )
                                    } else if (hasRecord) {
                                        Spacer(Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else NeonGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentAttendanceRow(
    name: String,
    rollNo: String,
    status: String,
    onPresent: () -> Unit,
    onAbsent: () -> Unit
) {
    val isPresent = status == "present"
    val isAbsent = status == "absent"

    val bgColor by animateColorAsState(
        targetValue = when (status) {
            "present" -> Success.copy(alpha = 0.08f)
            "absent" -> Error.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        }, label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when (status) {
            "present" -> Success.copy(alpha = 0.5f)
            "absent" -> Error.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }, label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (status.isBlank()) 1.dp else 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    color = Primary,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = rollNo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Present Button
            FilledIconButton(
                onClick = onPresent,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPresent) Success else Success.copy(alpha = 0.1f),
                    contentColor = if (isPresent) Color.White else Success
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Mark Present",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Absent Button
            FilledIconButton(
                onClick = onAbsent,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isAbsent) Error else Error.copy(alpha = 0.1f),
                    contentColor = if (isAbsent) Color.White else Error
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Mark Absent",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StatSummaryChip(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
            fontWeight = FontWeight.Bold
        )
    }
}

