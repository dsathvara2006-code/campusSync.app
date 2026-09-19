package com.campussync.app.feature.attendance

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
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
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AttendanceViewScreen(
    collegeId: String,
    classId: String,
    studentId: String,
    viewModel: AttendanceViewModel,
    onOpenSettings: (() -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateString = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val currentYearMonth = YearMonth.from(selectedDate).format(DateTimeFormatter.ofPattern("yyyy-MM"))
    var showCalendar by remember { mutableStateOf(false) }

    val holidayViewModel: com.campussync.app.core.viewmodel.HolidayViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val holidayDates by holidayViewModel.holidayDates.collectAsState()

    LaunchedEffect(collegeId) {
        holidayViewModel.listenToHolidays(collegeId)
    }

    LaunchedEffect(collegeId, classId, studentId, dateString, holidayDates) {
        viewModel.listenToClassAttendance(collegeId, classId, dateString)
        viewModel.loadOverallPercentage(collegeId, classId, studentId, holidayDates)
        viewModel.loadMarkedDatesForMonth(collegeId, classId, currentYearMonth)
        viewModel.loadSubjectWisePercentage(collegeId, classId, studentId, holidayDates)
    }

    val attendance by viewModel.liveAttendance.collectAsState()
    val overallPercentageState by viewModel.overallPercentage.collectAsState()
    val overallPercentage = overallPercentageState ?: 0
    val myStatus = attendance?.records?.get(studentId) ?: "not_marked"
    val markedDates by viewModel.markedDates.collectAsState()
    val subjectWisePercentage by viewModel.subjectWisePercentage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            HeaderBar(
                selectedDate = selectedDate,
                showCalendar = showCalendar,
                onToggleCalendar = { showCalendar = !showCalendar },
                onOpenSettings = onOpenSettings
            )

            AnimatedVisibility(
                visible = showCalendar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                StudentCalendar(
                    selectedDate = selectedDate,
                    markedDates = markedDates,
                    holidayDates = holidayDates,
                    onMonthChange = { newYearMonth ->
                        viewModel.loadMarkedDatesForMonth(collegeId, classId, newYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    },
                    onDateSelected = { date -> selectedDate = date }
                )
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, (if (isSystemInDarkTheme()) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Attendance Overview",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(24.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AttendanceRing(
                            percentage = overallPercentage,
                            size = 150.dp,
                            strokeWidth = 14.dp
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                    
                    Text(
                        text = "Date Status: ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    StatusBadgeCard(myStatus = myStatus)

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClassStatItem(label = "Present", value = "${attendance?.present ?: 0}", color = Success, modifier = Modifier.weight(1f))
                        ClassStatItem(label = "Absent", value = "${attendance?.absent ?: 0}", color = Error, modifier = Modifier.weight(1f))
                        ClassStatItem(label = "Total", value = "${attendance?.total ?: 0}", color = Primary, modifier = Modifier.weight(1f))
                    }
                }
            }

            // [NEW] Subject-wise Breakdown Section
            if (subjectWisePercentage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Subject-wise Breakdown",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subjectWisePercentage.entries.toList()) { entry ->
                        SubjectPercentageCard(subject = entry.key, percentage = entry.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectPercentageCard(subject: String, percentage: Int) {
    val color = when {
        percentage >= 75 -> Success
        percentage >= 60 -> Warning
        else -> Error
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.widthIn(min = 130.dp, max = 160.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).heightIn(min = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.size(56.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "$percentage%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = subject,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeaderBar(
    selectedDate: LocalDate,
    showCalendar: Boolean,
    onToggleCalendar: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val displayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "My Attendance",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = selectedDate.format(displayFmt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        IconButton(
            onClick = onToggleCalendar,
            modifier = Modifier.size(44.dp)
                .background(
                    if (showCalendar) Brush.linearGradient(listOf(Indigo500, Violet500)) 
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Rounded.DateRange, 
                contentDescription = "Toggle Calendar", 
                tint = if (showCalendar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        if (onOpenSettings != null) {
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun StatusBadgeCard(myStatus: String) {
    val (statusText, color, icon) = when (myStatus) {
        "present" -> Triple("PRESENT", Success, Icons.Rounded.CheckCircle)
        "absent" -> Triple("ABSENT", Error, Icons.Rounded.Close)
        else -> Triple("NOT MARKED", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.Info)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = statusText,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ClassStatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, (if (isSystemInDarkTheme()) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Spacer(Modifier.height(4.dp))
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StudentCalendar(
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
        border = BorderStroke(1.dp, (if (isSystemInDarkTheme()) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.outlineVariant)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                    onMonthChange(currentMonth)
                }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Prev", tint = Primary)
                }

                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.year,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                    onMonthChange(currentMonth)
                }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next", tint = Primary)
                }
            }

            Spacer(Modifier.height(12.dp))

            val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

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
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(cellBgColor)
                                    .border(if (isSelected && isExcluded) 1.dp else 0.dp, if (isSelected && isExcluded) MaterialTheme.colorScheme.error else Color.Transparent, CircleShape)
                                    .clickable { if (!isExcluded) onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasRecord && !isSelected) {
                                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).clip(CircleShape).background(Primary))
                                }
                                Text(
                                    text = dayNum.toString(),
                                    color = cellTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || hasRecord || isToday) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
