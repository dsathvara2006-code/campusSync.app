package com.campussync.app.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.model.ActivityLog
import com.campussync.app.core.model.Event
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 1. PRINCIPAL EXECUTIVE GLASS HEADER
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun PrincipalGlassHeader(
    user: User,
    studentCount: Int,
    teacherCount: Int,
    classCount: Int,
    campusName: String,
    onSettings: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) {
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val currentDate = remember {
        val sdf = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
        sdf.format(Date())
    }

    val headerBg = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Primary.copy(alpha = 0.24f),
                Secondary.copy(alpha = 0.14f),
                Night800.copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Primary.copy(alpha = 0.16f),
                Secondary.copy(alpha = 0.09f),
                Color.White.copy(alpha = 0.96f)
            )
        )
    }

    val glassBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Primary.copy(alpha = 0.15f)
    val principalName = user.name.ifBlank { "Campus Principal" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, glassBorderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(18.dp)
            ) {
                Column {
                    // Top Row: Avatar + Principal Name + Date Pill + Notification Bell
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monogram Initial Box with Glowing Gradient
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(8.dp, shape = RoundedCornerShape(18.dp), spotColor = Primary.copy(alpha = 0.4f))
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, Primary.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(listOf(Primary.copy(alpha = 0.9f), Secondary.copy(alpha = 0.75f)))
                                )
                                .clickable { onSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = principalName.firstOrNull()?.uppercase() ?: "P"
                            Text(
                                text = initial,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Greeting + Principal Name + Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greeting, Principal 👋",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = principalName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Institutional Executive",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Date Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = currentDate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    }

                    Spacer(Modifier.height(16.dp))

                    // Institutional Summary Strip: 4 chips (Students, Teachers, Classes, Campus)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip(
                            icon = Icons.Rounded.School,
                            label = "$studentCount Students",
                            color = Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryChip(
                            icon = Icons.Rounded.Person,
                            label = "$teacherCount Faculty",
                            color = Primary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryChip(
                            icon = Icons.Rounded.Class,
                            label = "$classCount Classes",
                            color = Amber500,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryChip(
                            icon = Icons.Rounded.AccountBalance,
                            label = campusName.ifBlank { "Campus" },
                            color = Emerald500,
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 2. PRINCIPAL EXECUTIVE STATS GRID
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun PrincipalExecutiveStatsGrid(
    studentCount: Int,
    teacherCount: Int,
    classCount: Int,
    averageAttendance: Float,
    verifiedRevenue: Double,
    onManageStaffClick: () -> Unit = {},
    onOverviewClick: () -> Unit = {},
    onFinanceClick: () -> Unit = {}
) {
    val stats = remember(studentCount, teacherCount, classCount, averageAttendance, verifiedRevenue) {
        listOf(
            ExecutiveStatItem(
                icon = Icons.Outlined.People,
                label = "Total Students",
                value = studentCount.toString(),
                sub = "Enrolled",
                color = Secondary,
                onClick = onOverviewClick
            ),
            ExecutiveStatItem(
                icon = Icons.Outlined.PersonPin,
                label = "Faculty Staff",
                value = teacherCount.toString(),
                sub = "Active Teachers",
                color = Primary,
                onClick = onManageStaffClick
            ),
            ExecutiveStatItem(
                icon = Icons.Outlined.CheckCircle,
                label = "Attendance",
                value = "${averageAttendance.toInt()}%",
                sub = "Campus Average",
                color = if (averageAttendance >= 75f) Success else Warning,
                onClick = onOverviewClick
            ),
            ExecutiveStatItem(
                icon = Icons.Outlined.Payments,
                label = "Collections",
                value = "₹${formatCurrency(verifiedRevenue.toInt())}",
                sub = "Verified Fees",
                color = Emerald500,
                onClick = onFinanceClick
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExecutiveStatCard(item = stats[0], modifier = Modifier.weight(1f))
            ExecutiveStatCard(item = stats[1], modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExecutiveStatCard(item = stats[2], modifier = Modifier.weight(1f))
            ExecutiveStatCard(item = stats[3], modifier = Modifier.weight(1f))
        }
    }
}

private data class ExecutiveStatItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val sub: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun ExecutiveStatCard(item: ExecutiveStatItem, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { item.onClick() },
        shape = RoundedCornerShape(20.dp),
        color = item.color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, item.color.copy(alpha = 0.22f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(item.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = item.sub,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = item.color
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = item.color.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 3. CLASS PERFORMANCE & ATTENDANCE BREAKDOWN
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun PrincipalClassAnalyticsWidget(
    classWiseAttendance: Map<String, Int>,
    onViewDetailedOverview: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val glassBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Primary.copy(alpha = 0.15f)

    val classesList = remember(classWiseAttendance) {
        classWiseAttendance.toList().sortedByDescending { it.second }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, glassBorderColor),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Class Performance Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time class presence standing",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onViewDetailedOverview) {
                    Text("Overview", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (classesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No class attendance recorded yet",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                classesList.take(5).forEach { (className, percentage) ->
                    val validPercent = if (percentage >= 0) percentage else 0
                    val statusColor = when {
                        validPercent >= 75 -> Success
                        validPercent >= 60 -> Amber500
                        else -> Error
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = className,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )

                            if (percentage < 0) {
                                Text(
                                    text = "Not Marked",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                if (validPercent < 75) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (validPercent < 60) "Critical Alert" else "Review",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = "$validPercent%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Progress bar
                        val progressFraction = (validPercent / 100f).coerceIn(0.01f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 4. PRINCIPAL OPERATIONS HUB
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun PrincipalOperationsHub(
    onManageStaffClick: () -> Unit = {},
    onClassesOverviewClick: () -> Unit = {},
    onFinanceClick: () -> Unit = {},
    onBroadcastNoticeClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val glassBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Primary.copy(alpha = 0.15f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, glassBorderColor),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Campus Executive Operations",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OperationChip(
                    title = "Manage Staff",
                    icon = Icons.Rounded.SupervisedUserCircle,
                    color = Indigo500,
                    modifier = Modifier.weight(1f),
                    onClick = onManageStaffClick
                )
                OperationChip(
                    title = "Class Details",
                    icon = Icons.Rounded.School,
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onClassesOverviewClick
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OperationChip(
                    title = "Fee Realization",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    color = Emerald500,
                    modifier = Modifier.weight(1f),
                    onClick = onFinanceClick
                )
                OperationChip(
                    title = "Announcements",
                    icon = Icons.Rounded.Campaign,
                    color = Amber500,
                    modifier = Modifier.weight(1f),
                    onClick = onBroadcastNoticeClick
                )
            }
        }
    }
}

@Composable
private fun OperationChip(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 5. RECENT INSTITUTIONAL ACTIVITY FEED
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun PrincipalActivityFeedWidget(
    activities: List<ActivityLog>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val glassBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Primary.copy(alpha = 0.15f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, glassBorderColor),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Campus Activity Timeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time institutional events & logs",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent campus activities logged.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                activities.take(6).forEach { log ->
                    val typeColor = when (log.type.lowercase()) {
                        "fee" -> Emerald500
                        "user" -> Secondary
                        "class" -> Primary
                        else -> Amber500
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(typeColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (log.description.isNotBlank()) {
                                Text(
                                    text = log.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatTimeAgo(log.timestamp),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 6. UPCOMING EVENTS & CALENDAR WIDGET
// ═══════════════════════════════════════════════════════════════════════════════════

data class EventCategory(val label: String, val color: Color, val icon: ImageVector)

private fun resolveEventCategory(title: String, desc: String): EventCategory {
    val combined = (title + " " + desc).lowercase()
    return when {
        combined.contains("exam") || combined.contains("test") || combined.contains("midterm") || combined.contains("viva") || combined.contains("quiz") ->
            EventCategory("Exam", Color(0xFFEF4444), Icons.Rounded.School)
        combined.contains("fest") || combined.contains("cultural") || combined.contains("celebration") || combined.contains("annual") || combined.contains("dance") || combined.contains("music") ->
            EventCategory("Fest", Color(0xFF8B5CF6), Icons.Rounded.Celebration)
        combined.contains("sport") || combined.contains("cricket") || combined.contains("football") || combined.contains("match") || combined.contains("tournament") ->
            EventCategory("Sports", Color(0xFF10B981), Icons.Rounded.EmojiEvents)
        combined.contains("holiday") || combined.contains("vacation") || combined.contains("closed") || combined.contains("diwali") || combined.contains("eid") ->
            EventCategory("Holiday", Color(0xFFF59E0B), Icons.Rounded.WbSunny)
        combined.contains("workshop") || combined.contains("seminar") || combined.contains("webinar") || combined.contains("conference") || combined.contains("guest") ->
            EventCategory("Workshop", Color(0xFF0EA5E9), Icons.Rounded.CoPresent)
        else ->
            EventCategory("Campus", Color(0xFF6366F1), Icons.Rounded.Event)
    }
}

private fun formatEventCountdown(eventDate: Long): Pair<String, Color> {
    val now = System.currentTimeMillis()
    val diff = eventDate - now
    val days = (diff / 86400000L).toInt()
    return when {
        diff < -86400000L -> "Past Event" to Color(0xFF94A3B8)
        days == 0 -> "Today" to Color(0xFFEF4444)
        days == 1 -> "Tomorrow" to Color(0xFFF97316)
        days in 2..6 -> "In $days days" to Color(0xFF0EA5E9)
        days in 7..14 -> "Next week" to Color(0xFF6366F1)
        else -> "In ${days / 7} weeks" to Color(0xFF64748B)
    }
}

@Composable
fun PrincipalEventsWidget(
    events: List<Event>,
    onAddEventClick: () -> Unit = {},
    onDeleteEventClick: ((Event) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val glassBorderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Primary.copy(alpha = 0.15f)
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedEventForDetail by remember { mutableStateOf<Event?>(null) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }

    val filteredEvents = remember(events, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") events
        else events.filter { resolveEventCategory(it.title, it.description).label == selectedCategoryFilter }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, glassBorderColor),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Top Header with live counter and Add button ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Event,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Campus Events & Schedule",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (events.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${events.size} Scheduled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Exams, fests, sports meets & official notices",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddEventClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Category Filter Chips ──
            if (events.size > 2) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterCategories = listOf("All", "Exam", "Fest", "Sports", "Holiday", "Workshop", "Campus")
                    filterCategories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCategoryFilter = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filteredEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.EventAvailable,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (selectedCategoryFilter == "All") "No upcoming campus events" else "No $selectedCategoryFilter events found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Schedule' above to notify students & faculty of exams, holidays or activities.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredEvents.forEach { event ->
                        val eventDateVal = if (event.eventDate > 0) event.eventDate else event.createdAt
                        val cal = remember(eventDateVal) {
                            Calendar.getInstance().apply { timeInMillis = eventDateVal }
                        }
                        val monthStr = remember(eventDateVal) { SimpleDateFormat("MMM", Locale.US).format(cal.time) }
                        val dayStr = remember(eventDateVal) { cal.get(Calendar.DAY_OF_MONTH).toString() }
                        val dayOfWeekStr = remember(eventDateVal) { SimpleDateFormat("EEE", Locale.US).format(cal.time) }
                        val (countdownText, countdownColor) = remember(eventDateVal) { formatEventCountdown(eventDateVal) }
                        val category = remember(event.title, event.description) { resolveEventCategory(event.title, event.description) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedEventForDetail = event },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ── Elegant Vertical Date Block ──
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = category.color.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, category.color.copy(alpha = 0.25f)),
                                    modifier = Modifier.width(52.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = monthStr.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = category.color
                                        )
                                        Text(
                                            text = dayStr,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = dayOfWeekStr.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                // ── Middle Event Info ──
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Category Tag
                                        Surface(
                                            color = category.color.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = category.icon,
                                                    contentDescription = null,
                                                    tint = category.color,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = category.label,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = category.color
                                                )
                                            }
                                        }

                                        // Urgency / Countdown Tag
                                        Surface(
                                            color = countdownColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = countdownText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = countdownColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = event.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (event.description.isNotBlank()) {
                                        Text(
                                            text = event.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (event.createdBy.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "By: ${event.createdBy}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // ── Action: Delete Event Button ──
                                if (onDeleteEventClick != null) {
                                    IconButton(
                                        onClick = { eventToDelete = event },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Cancel Event",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
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

    // ── Event Detail Modal ──
    if (selectedEventForDetail != null) {
        val event = selectedEventForDetail!!
        val eventDateVal = if (event.eventDate > 0) event.eventDate else event.createdAt
        val formattedFullDate = remember(eventDateVal) {
            SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date(eventDateVal))
        }
        val category = remember(event.title, event.description) { resolveEventCategory(event.title, event.description) }
        val (countdownText, countdownColor) = remember(eventDateVal) { formatEventCountdown(eventDateVal) }

        AlertDialog(
            onDismissRequest = { selectedEventForDetail = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(category.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Surface(color = category.color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "${category.label} • $countdownText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = category.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(formattedFullDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (event.createdBy.isNotBlank()) {
                        Text(
                            text = "Organized / Scheduled by: ${event.createdBy}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEventForDetail = null }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Primary)
                }
            },
            dismissButton = if (onDeleteEventClick != null) {
                {
                    TextButton(
                        onClick = {
                            val target = selectedEventForDetail
                            selectedEventForDetail = null
                            if (target != null) eventToDelete = target
                        }
                    ) {
                        Text("Delete Event", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            } else null
        )
    }

    // ── Delete Confirmation Dialog ──
    if (eventToDelete != null) {
        val event = eventToDelete!!
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Cancel Event?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove '${event.title}' from the campus event schedule? This cannot be undone.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = eventToDelete!!
                        eventToDelete = null
                        onDeleteEventClick?.invoke(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove Event", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Keep Event")
                }
            }
        )
    }
}


// Helper time formatting
private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = (diff / 1000).coerceAtLeast(0)
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days ${if (days == 1L) "d" else "d"} ago"
        hours > 0 -> "$hours ${if (hours == 1L) "h" else "h"} ago"
        minutes > 0 -> "$minutes ${if (minutes == 1L) "m" else "m"} ago"
        else -> "Just now"
    }
}

private fun formatCurrency(amount: Int): String {
    return when {
        amount >= 100000 -> "${String.format(Locale.US, "%.1f", amount / 100000f)}L"
        amount >= 1000 -> "${String.format(Locale.US, "%.1f", amount / 1000f)}K"
        else -> amount.toString()
    }
}
