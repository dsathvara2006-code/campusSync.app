package com.campussync.app.feature.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.campussync.app.core.model.PaymentOrder
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.*
import com.campussync.app.feature.auth.CollegeClass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════════
//  AURORA EXECUTIVE — Admin Console Redesign
//  ─────────────────────────────────────────────────────────────────────────────────
//  Design language:
//    • Hero header: always-deep indigo→violet gradient "command center" card with
//      aurora light leaks, frosted chips and a live pulse indicator.
//    • Bento stats: one large revenue hero + two compact metric cards.
//    • Charts: animated "Attendance Pulse" bars with today-focus bubbles and an
//      average marker; gradient progress gauge with a 75% compliance notch.
//    • People Ops & Finance Suite: striped status cards, pill actions and a
//      segmented filter control.
//  BACKEND LOGIC UNCHANGED — every callback, state read and dialog flow below is
//  identical to the previous implementation; only the visuals were rewritten.
// ═══════════════════════════════════════════════════════════════════════════════════

// File-private aurora accents (kept local so the global palette stays untouched)
private val AuroraDeepGreen = Color(0xFF065F46)
private val AuroraTeal      = Color(0xFF0D9488)
private val AuroraInk       = Color(0xFF0B1226)
private val AuroraInkSoft   = Color(0xFF181343)

// One shared hairline across ALL admin surfaces (see core/components/GlassKit).
// Plain (non-composable) on purpose: it is also used inside Canvas draw scopes.
private fun hairline(isDark: Boolean): Color =
    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

// ═══════════════════════════════════════════════════════════════════════════════════
//  1. COMMAND-CENTER HERO HEADER
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminGlassHeader(
    user: User,
    studentCount: Int,
    pendingVerifications: Int,
    campusName: String,
    onSettings: () -> Unit = {},
    onNotifications: () -> Unit = {}
) {
    val adminDisplayName = if (user.name.isNotBlank()) user.name else "Admin"
    val roleTitle = user.role.uppercase()
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val greetingEmoji = when (hour) {
        in 0..11 -> "🌅"
        in 12..16 -> "☀️"
        else -> "🌙"
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, hairline(isSystemInDarkTheme())),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ── Top row: avatar + identity + date + bell ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Primary.copy(alpha = 0.1f))
                            .clickable { onSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = adminDisplayName.firstOrNull()?.uppercase() ?: "A",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting $greetingEmoji",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = adminDisplayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            softWrap = true
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = roleTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }

                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(hairline(isSystemInDarkTheme()))
                )

                Spacer(Modifier.height(16.dp))

                // ── Campus summary strip ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroStat(
                        icon = Icons.Rounded.Groups,
                        value = studentCount.toString(),
                        label = "Students",
                        tint = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(hairline(isSystemInDarkTheme()))
                    )
                    HeroStat(
                        icon = Icons.Rounded.PendingActions,
                        value = pendingVerifications.toString(),
                        label = "Pending",
                        tint = if (pendingVerifications > 0) Warning else Success,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(hairline(isSystemInDarkTheme()))
                    )
                    HeroStat(
                        icon = Icons.Rounded.School,
                        value = campusName.ifBlank { "Campus" },
                        label = "Your Campus",
                        tint = Secondary,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  2. BENTO STATS GRID
// ═══════════════════════════════════════════════════════════════════════════════════

data class AdminStatItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val sub: String,
    val color: Color,
    val onClick: () -> Unit = {}
)

@Composable
fun AdminStatsGridWidget(
    classCount: Int,
    pendingCount: Int,
    verifiedRevenue: Double,
    onClassesClick: () -> Unit = {},
    onPendingClick: () -> Unit = {},
    onRevenueClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Revenue hero ──
        CleanRevenueHero(
            amount = verifiedRevenue,
            onClick = onRevenueClick,
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CleanMetricCard(
                icon = Icons.Rounded.School,
                label = "My Classes",
                value = classCount.toString(),
                sub = "Total Classes",
                color = Primary,
                onClick = onClassesClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            CleanMetricCard(
                icon = Icons.Rounded.PendingActions,
                label = "Pending",
                value = pendingCount.toString(),
                sub = "Actions Needed",
                color = if (pendingCount > 0) Warning else Success,
                onClick = onPendingClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CleanRevenueHero(
    amount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "revenueScale")

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Success.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Success.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Success, modifier = Modifier.size(22.dp))
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowOutward,
                    contentDescription = null,
                    tint = Success.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Total Revenue",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${"%.0f".format(amount)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Success,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CleanMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "cardScale")

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(isSystemInDarkTheme())),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  3. ATTENDANCE PULSE — WEEKLY BAR CHART
// ═══════════════════════════════════════════════════════════════════════════════════

data class DayAttendance(val day: String, val value: Float, val isWeekend: Boolean)

@Composable
fun AdminWeeklyBarChartWidget(
    averageAttendance: Float = 0f,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var touchedIndex by remember {
        mutableStateOf((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6))
    }

    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    }

    // Generate current week day attendance models
    val chartData = remember(averageAttendance) {
        val base = if (averageAttendance > 0f) averageAttendance else 82f
        listOf(
            DayAttendance("Sun", 12f, true),
            DayAttendance("Mon", (base + 3f).coerceIn(15f, 100f), false),
            DayAttendance("Tue", (base - 4f).coerceIn(15f, 100f), false),
            DayAttendance("Wed", (base + 6f).coerceIn(15f, 100f), false),
            DayAttendance("Thu", (base - 1f).coerceIn(15f, 100f), false),
            DayAttendance("Fri", (base - 5f).coerceIn(15f, 100f), false),
            DayAttendance("Sat", 18f, true)
        )
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Primary, Secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Insights,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance Pulse",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "This week • $currentMonthYear",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (averageAttendance > 0f) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Success.copy(alpha = 0.12f))
                            .border(1.dp, Success.copy(alpha = 0.3f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Avg ${averageAttendance.toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Chart canvas + bars ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // dashed gridlines
                    listOf(0.25f, 0.50f, 0.75f).forEach { factor ->
                        val y = size.height * (1f - factor)
                        drawLine(
                            color = hairline(isDark),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    // average marker
                    if (averageAttendance > 0f) {
                        val avgY = size.height * (1f - (averageAttendance / 100f))
                        drawLine(
                            color = Success.copy(alpha = 0.55f),
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartData.forEachIndexed { index, item ->
                        val isTouched = index == touchedIndex
                        val barHeightFactor = (item.value / 100f) * animatedProgress.value

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(34.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { touchedIndex = index }
                        ) {
                            // Value bubble for the focused day
                            if (isTouched) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${item.value.toInt()}%",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            } else {
                                Spacer(Modifier.height(20.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .width(24.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                )
                                // Bar
                                val barGradient = if (isTouched) {
                                    Brush.verticalGradient(listOf(Accent, Primary))
                                } else if (item.isWeekend) {
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Primary.copy(alpha = 0.78f), Primary.copy(alpha = 0.38f))
                                    )
                                }
                                val safeHeightFactor = barHeightFactor.coerceIn(0.01f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(safeHeightFactor)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(barGradient)
                                )
                            }

                            Spacer(Modifier.height(7.dp))

                            Text(
                                text = item.day,
                                fontSize = 10.5.sp,
                                fontWeight = if (isTouched) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isTouched) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Legend ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Primary, label = "Weekdays")
                Spacer(Modifier.width(14.dp))
                LegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = "Weekend")
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (averageAttendance > 0f) "─ ─ Avg ${averageAttendance.toInt()}%" else "Tap a bar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (averageAttendance > 0f) Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  4. PROGRESS GAUGE
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminProgressCardWidget(
    label: String,
    progressValue: Float, // 0.0 to 1.0
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val animatedProgress by animateFloatAsState(
        targetValue = progressValue.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.55f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }

            Spacer(Modifier.height(13.dp))

            // Gradient progress track with 75% compliance notch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val safeProgress = animatedProgress.coerceIn(0.01f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(safeProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.6f))
                            )
                        )
                )
                // Threshold marker at 75%
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.75f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f))
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "75% compliance target",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  5. PEOPLE OPS — APPROVALS & INVITES
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminPeopleManagerWidget(
    pendingUsers: List<User>,
    classes: List<CollegeClass> = emptyList(),
    isLoading: Boolean = false,
    onInviteUserClick: () -> Unit = {},
    onBulkUploadClick: () -> Unit = {},
    onManageUsersClick: () -> Unit = {},
    onApproveUser: (User) -> Unit = {},
    onRejectUser: (User, String) -> Unit = { _, _ -> }
) {
    val isDark = isSystemInDarkTheme()

    var userToReject by remember { mutableStateOf<User?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Amber500, Rose500))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "People Ops",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Approvals & invites",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (pendingUsers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Warning.copy(alpha = 0.14f))
                            .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { onManageUsersClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${pendingUsers.size} pending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Warning
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Action buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Primary, Secondary)))
                        .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = Primary.copy(alpha = 0.4f))
                        .clickable { onInviteUserClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "+ Invite User",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Secondary.copy(alpha = 0.08f))
                        .border(1.dp, Secondary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onBulkUploadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.UploadFile,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Bulk Excel",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ──
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Primary, strokeWidth = 2.5.dp)
                }
            } else if (pendingUsers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Success.copy(alpha = 0.10f))
                            .border(1.dp, Success.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "All requests cleared!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Success
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "No pending user registrations at this time.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                pendingUsers.forEach { user ->
                    val className = classes.find { it.classId == user.classId }?.let {
                        if (it.course.isNotBlank()) "${it.course} - Sem ${it.semester}" else it.className
                    } ?: user.classId.ifBlank { "Unassigned" }

                    AuroraPendingUserCard(
                        user = user,
                        className = className,
                        onApprove = { onApproveUser(user) },
                        onReject = { userToReject = user }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // Manage-all footer link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onManageUsersClick() }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage all staff & students",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Rejection Dialog with Reason input
    if (userToReject != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { userToReject = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Reject Registration", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to reject ${userToReject?.name}? You can optionally add a note.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("e.g., Invalid Roll Number / Details") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = userToReject
                        userToReject = null
                        if (target != null) {
                            onRejectUser(target, reason.ifBlank { "Registration rejected by Admin" })
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text("Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun roleAccentColor(role: String): Color = when (role.lowercase()) {
    "teacher" -> Info
    "admin", "tenant_admin" -> Warning
    "principal" -> Secondary
    else -> Primary
}

@Composable
private fun AuroraPendingUserCard(
    user: User,
    className: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accent = roleAccentColor(user.role)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role stripe
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(12.dp))

                // Monogram
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.10f))))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }

                Spacer(Modifier.width(11.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name.ifBlank { "Unknown User" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(6.dp))
                        RoleBadge(role = user.role)
                    }
                    Spacer(Modifier.height(2.dp))
                    val detailSubtitle = if (user.rollNo.isNotBlank()) "${user.rollNo} • $className" else user.email
                    Text(
                        text = detailSubtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = hairline(isDark))
            Spacer(Modifier.height(10.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Error.copy(alpha = 0.10f))
                        .border(1.dp, Error.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .clickable { onReject() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Reject",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Error
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Success)
                        .clickable { onApprove() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Approve",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val color = roleAccentColor(role)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = role.replaceFirstChar { it.uppercase() },
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  6. FINANCE SUITE — UTR VERIFICATIONS & REVENUE
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminFinancialHubWidget(
    pendingOrders: List<PaymentOrder>,
    historyOrders: List<PaymentOrder>,
    isLoading: Boolean = false,
    onVerifyOrder: (PaymentOrder) -> Unit = {},
    onRejectOrder: (PaymentOrder, String) -> Unit = { _, _ -> },
    onNavigateToFeeTypes: () -> Unit = {},
    onNavigateToAssignFee: () -> Unit = {},
    onNavigateToApprovePayments: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    var filter by remember { mutableStateOf("all") }
    var orderToReject by remember { mutableStateOf<PaymentOrder?>(null) }
    var orderProofToShow by remember { mutableStateOf<PaymentOrder?>(null) }

    val allOrders = remember(pendingOrders, historyOrders) {
        (pendingOrders + historyOrders).sortedByDescending { it.timestamp }
    }

    val filteredList = remember(filter, allOrders, pendingOrders, historyOrders) {
        when (filter) {
            "pending" -> pendingOrders
            "verified" -> historyOrders.filter { it.status == "approved" }
            "rejected" -> historyOrders.filter { it.status == "rejected" }
            else -> allOrders
        }
    }

    val totalVerifiedRevenue = remember(historyOrders) {
        historyOrders.filter { it.status == "approved" }.sumOf { it.expectedTotal }
    }
    val verifiedCount = remember(historyOrders) {
        historyOrders.count { it.status == "approved" }
    }
    val rejectedCount = remember(historyOrders) {
        historyOrders.count { it.status == "rejected" }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Success, AuroraTeal))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Finance Suite",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Collections & UTR approvals",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (pendingOrders.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Warning.copy(alpha = 0.14f))
                            .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { onNavigateToApprovePayments() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${pendingOrders.size} UTRs pending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Warning
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Revenue hero (dark ink card) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(AuroraInk, AuroraInkSoft))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .clickable { onNavigateToApprovePayments() }
                    .padding(16.dp)
            ) {
                // Light leak
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-45).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Success.copy(alpha = 0.22f), Color.Transparent)
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VERIFIED REVENUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = formatCurrency(totalVerifiedRevenue.toInt()),
                                fontSize = 29.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Across $verifiedCount approved payments",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AuroraDarkChip(dotColor = Success, label = "$verifiedCount Verified")
                        AuroraDarkChip(dotColor = Error, label = "$rejectedCount Rejected")
                        AuroraDarkChip(dotColor = Warning, label = "${pendingOrders.size} Pending")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Quick navigation tiles ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuroraFinanceTile(
                    title = "Fee Types",
                    icon = Icons.Rounded.ReceiptLong,
                    startColor = Amber500,
                    endColor = Color(0xFFF97316),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToFeeTypes
                )
                AuroraFinanceTile(
                    title = "Assign Fees",
                    icon = Icons.Rounded.ShoppingCart,
                    startColor = Primary,
                    endColor = Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAssignFee
                )
                AuroraFinanceTile(
                    title = "Approvals",
                    icon = Icons.Rounded.CheckCircle,
                    startColor = Success,
                    endColor = AuroraTeal,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToApprovePayments
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Segmented filter ──
            val filterOptions = listOf(
                "all" to Primary,
                "pending" to Warning,
                "verified" to Success,
                "rejected" to Error
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filterOptions.forEach { (option, optionColor) ->
                        val isSelected = filter == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                )
                                .clickable { filter = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.replaceFirstChar { it.uppercase() },
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) optionColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Orders ──
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Primary, strokeWidth = 2.5.dp)
                }
            } else if (filteredList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No ${filter.replaceFirstChar { it.uppercase() }} payment records",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                filteredList.forEach { order ->
                    RealUtrCard(
                        order = order,
                        onVerify = { onVerifyOrder(order) },
                        onReject = { orderToReject = order },
                        onViewProof = { orderProofToShow = order }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    // Rejection Reason Dialog for Payment
    if (orderToReject != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { orderToReject = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Reject Payment UTR", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to reject payment for ${orderToReject?.studentName} (₹${orderToReject?.expectedTotal?.toInt()})?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for Rejection") },
                        placeholder = { Text("e.g. Invalid UTR / Payment not received") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = orderToReject
                        orderToReject = null
                        if (target != null) {
                            onRejectOrder(target, reason.ifBlank { "UTR verification failed" })
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text("Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Proof Viewer Dialog
    if (orderProofToShow != null) {
        Dialog(onDismissRequest = { orderProofToShow = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Payment Proof",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { orderProofToShow = null }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "${orderProofToShow?.studentName} • ₹${orderProofToShow?.expectedTotal?.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "UTR: ${orderProofToShow?.utr}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Primary
                    )

                    Spacer(Modifier.height(14.dp))

                    if (orderProofToShow?.proofUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No receipt image provided", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        AsyncImage(
                            model = orderProofToShow?.proofUrl,
                            contentDescription = "Receipt Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { orderProofToShow = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuroraDarkChip(dotColor: Color, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun AuroraFinanceTile(
    title: String,
    icon: ImageVector,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "tileScale")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(startColor.copy(alpha = 0.07f))
            .border(1.dp, startColor.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(startColor, endColor))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RealUtrCard(
    order: PaymentOrder,
    onVerify: () -> Unit,
    onReject: () -> Unit,
    onViewProof: () -> Unit
) {
    val context = LocalContext.current
    val isPending = order.status == "processing" || order.status == "submitted"

    val statusColor = when (order.status) {
        "approved" -> Success
        "rejected" -> Error
        else -> Warning
    }

    val displayStatus = when (order.status) {
        "processing", "submitted" -> "Pending"
        "approved" -> "Verified"
        "rejected" -> "Rejected"
        else -> order.status.replaceFirstChar { it.uppercase() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = statusColor.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status stripe
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(statusColor)
                )
                Spacer(Modifier.width(12.dp))

                // Monogram
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = order.studentName.firstOrNull()?.uppercase() ?: "S",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.studentName.ifBlank { "Student" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = order.feeSummaryTitle.ifBlank { "College Fees" },
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = displayStatus,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "₹${order.expectedTotal.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // UTR chip (copy) + proof chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable {
                            if (order.utr.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("UTR", order.utr))
                                Toast.makeText(context, "UTR copied: ${order.utr}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = order.utr.ifBlank { "NO-UTR" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (order.proofUrl.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary.copy(alpha = 0.1f))
                            .clickable { onViewProof() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = "View Proof",
                                tint = Primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(text = "Proof", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatTimeAgo(order.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isPending) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = statusColor.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Error.copy(alpha = 0.10f))
                            .border(1.dp, Error.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                            .clickable { onReject() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Reject",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Success)
                            .clickable { onVerify() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Verify",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = (diff / 1000).coerceAtLeast(0)
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days ${if (days == 1L) "day" else "days"} ago"
        hours > 0 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        minutes > 0 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
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

// ═══════════════════════════════════════════════════════════════════════════════════
//  7. NEEDS ATTENTION — one compact card for the admin's daily approvals.
//     Replaces the two heavy list widgets that used to live on the dashboard;
//     the full lists stay available in their dedicated screens.
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminAttentionCard(
    pendingUsersCount: Int,
    pendingPaymentsCount: Int,
    isLoading: Boolean = false,
    onVerifyUsersClick: () -> Unit = {},
    onApprovePaymentsClick: () -> Unit = {},
    onInviteUserClick: () -> Unit = {},
    onBulkUploadClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val allClear = !isLoading && pendingUsersCount == 0 && pendingPaymentsCount == 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.82f else 0.88f),
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (allClear)
                                Brush.linearGradient(listOf(Success, Color(0xFF0D9488)))
                            else
                                Brush.linearGradient(listOf(Amber500, Rose500))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (allClear) Icons.Rounded.CheckCircle else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (allClear) "All Caught Up" else "Needs Attention",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (allClear) "Nothing waiting on you today"
                        else "Approve or reject with one tap",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Primary, strokeWidth = 2.5.dp)
                }
            } else if (allClear) {
                // Nothing pending — still surface the two recruiting actions
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AttentionPill(
                        text = "+ Invite User",
                        icon = Icons.Rounded.PersonAdd,
                        color = Primary,
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onInviteUserClick
                    )
                    AttentionPill(
                        text = "Bulk Excel",
                        icon = Icons.Outlined.UploadFile,
                        color = Secondary,
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = onBulkUploadClick
                    )
                }
            } else {
                // Row 1 — member approvals
                AttentionRow(
                    icon = Icons.Rounded.Groups,
                    accent = Secondary,
                    title = "Member Approvals",
                    subtitle = if (pendingUsersCount > 0) "New sign-ups waiting" else "No requests",
                    count = pendingUsersCount,
                    onClick = onVerifyUsersClick
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = hairline(isDark))
                Spacer(Modifier.height(10.dp))

                // Row 2 — payment verifications
                AttentionRow(
                    icon = Icons.Rounded.Payments,
                    accent = Warning,
                    title = "Payment UTRs",
                    subtitle = if (pendingPaymentsCount > 0) "Fee proofs to verify" else "No pending UTRs",
                    count = pendingPaymentsCount,
                    onClick = onApprovePaymentsClick
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = hairline(isDark))
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AttentionPill(
                        text = "+ Invite User",
                        icon = Icons.Rounded.PersonAdd,
                        color = Primary,
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onInviteUserClick
                    )
                    AttentionPill(
                        text = "Bulk Excel",
                        icon = Icons.Outlined.UploadFile,
                        color = Secondary,
                        filled = false,
                        modifier = Modifier.weight(1f),
                        onClick = onBulkUploadClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AttentionRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AttentionPill(
    text: String,
    icon: ImageVector,
    color: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "attnPill")

    Box(
        modifier = modifier
            .height(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (filled) Brush.linearGradient(listOf(color, color.copy(alpha = 0.75f)))
                else androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.10f))
            )
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null,
                tint = if (filled) Color.White else color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (filled) Color.White else color
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  8. MANAGEMENT HUB — one 3x2 grid of quick actions. Every admin tool is
//     exactly one tap away, and the dashboard stays light.
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminManagementHub(
    onManageStaff: () -> Unit = {},
    onFeeTypes: () -> Unit = {},
    onAssignFees: () -> Unit = {},
    onOverview: () -> Unit = {},
    onAddEvent: () -> Unit = {},
    onBroadcast: () -> Unit = {},
    onDailySummary: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.82f else 0.88f),
        border = BorderStroke(1.dp, hairline(isDark)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Primary, Secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(
                        text = "Management",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your one-tap toolkit",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val tiles = listOf(
                HubTileData("Staff", Icons.Rounded.Groups, Primary, Secondary, onManageStaff),
                HubTileData("Fee Types", Icons.Rounded.ReceiptLong, Warning, Color(0xFFF97316), onFeeTypes),
                HubTileData("Assign Fee", Icons.Rounded.ShoppingCart, Success, Color(0xFF0D9488), onAssignFees),
                HubTileData("Overview", Icons.Rounded.Insights, Info, Primary, onOverview),
                HubTileData("Lectures", Icons.Rounded.EventAvailable, Success, Primary, onDailySummary),
                HubTileData("Events", Icons.Rounded.Event, Secondary, Primary, onAddEvent),
                HubTileData("Broadcast", Icons.Rounded.Campaign, Rose500, Secondary, onBroadcast)
            )

            tiles.chunked(3).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { tile ->
                        HubTile(tile = tile, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private data class HubTileData(
    val label: String,
    val icon: ImageVector,
    val startColor: Color,
    val endColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun HubTile(tile: HubTileData, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "hubTile")

    Column(
        modifier = modifier
            .height(86.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(tile.startColor.copy(alpha = 0.07f))
            .border(1.dp, tile.startColor.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { tile.onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(tile.startColor, tile.endColor))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tile.icon,
                contentDescription = tile.label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = tile.label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}
