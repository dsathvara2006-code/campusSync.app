import codecs

file_path = 'app/src/main/java/com/campussync/app/feature/dashboard/HomeScreen.kt'

with codecs.open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_ui_lines = '''    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(scrollState).padding(bottom = 100.dp)
    ) {
        if (isAdmin) {
            AdminHeader(user = user, onOpenSettings = onOpenSettings)
            AdminDashboardContent(user, principalViewModel, onNavigateToFeeTypes, onNavigateToAssignFee, onNavigateToApprovePayments, onNavigateToManageUsers, onNavigateToVerifyInvites)
        } else if (isPrincipal) {
            AdminHeader(user = user, onOpenSettings = onOpenSettings)
            PrincipalDashboardContent(user, principalViewModel)
        } else {
            // Student & Teacher UI
            PremiumStudentHeader(user, displayName, todayDate, greeting, greetingEmoji, onOpenSettings)
            
            Spacer(Modifier.height(32.dp))
            
            PremiumQuickActionsRow(isTeacher, onNavigateToTab, onNavigateToResources)
            
            Spacer(Modifier.height(32.dp))

            if (!isTeacher) {
                PremiumAttendanceHero(overallPercentage, presentDays, totalDays) { onNavigateToTab(1) }
                Spacer(Modifier.height(32.dp))
            }

            PremiumSectionTitle("Today's Schedule", "Full Week") { onNavigateToTab(2) }
            Spacer(Modifier.height(16.dp))
            if (todayClasses.isEmpty()) {
                PremiumEmptyState("Free Day!", "No classes scheduled for today.", Icons.Rounded.DateRange)
            } else {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    todayClasses.forEachIndexed { idx, lecture ->
                        TimelineScheduleRow(lecture.subject, lecture.time, lecture.teacherName, isLast = idx == todayClasses.lastIndex, index = idx)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            PremiumSectionTitle("Upcoming Deadlines", "See All") { onNavigateToTab(3) }
            Spacer(Modifier.height(16.dp))
            if (activeAssignments.isEmpty()) {
                PremiumEmptyState("All Caught Up", "You have no pending assignments.", Icons.Rounded.CheckCircle)
            } else {
                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeAssignments.forEach { assignment ->
                        FlatDeadlineRow(assignment.title, assignment.dueDate)
                    }
                }
            }

            if (!isTeacher) {
                Spacer(Modifier.height(32.dp))
                PremiumSectionTitle("Payments", "") {}
                Spacer(Modifier.height(16.dp))
                PremiumFeeActionCard(onNavigateToStudentFees)
            }
        }
    }
'''

premium_components = '''

// ═══════════════════════════════════════════════════════════════════════════════════
// 🌟 PREMIUM UI COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumStudentHeader(user: User, classId: String, date: String, greeting: String, emoji: String, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "$emoji $greeting", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(text = user.name.ifBlank { "Student" }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            if (classId.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = "$classId • $date", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)).clickable { onSettings() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = user.name.take(1).uppercase(), color = Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PremiumQuickActionsRow(isTeacher: Boolean, onTab: (Int) -> Unit, onRes: () -> Unit) {
    val items = if (isTeacher) listOf(
        Triple("Attendance", Icons.Rounded.CheckCircle, 1), Triple("New Task", Icons.Rounded.Edit, 3),
        Triple("Timetable", Icons.Rounded.DateRange, 2), Triple("Resources", Icons.Rounded.Star, -1)
    ) else listOf(
        Triple("My Stats", Icons.Rounded.CheckCircle, 1), Triple("Tasks", Icons.Rounded.Edit, 3),
        Triple("Schedule", Icons.Rounded.DateRange, 2), Triple("Library", Icons.Rounded.Star, -1)
    )

    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items) { item ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label="")

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable(interactionSource = interactionSource, indication = null) { if (item.third >= 0) onTab(item.third) else onRes() }
            ) {
                Box(
                    modifier = Modifier.size(64.dp).shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.second, null, tint = Primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(item.first, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PremiumAttendanceHero(percentage: Int, present: Int, total: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = Primary,
        shadowElevation = 16.dp
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Attendance", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("$percentage%", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                Text("$present / $total Days Present", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            // Circular Ring using Canvas
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                    drawArc(Color.White, -90f, (percentage / 100f) * 360f, false, style = Stroke(width = 16f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

@Composable
private fun TimelineScheduleRow(subject: String, time: String, teacher: String, isLast: Boolean, index: Int) {
    val colors = listOf(Color(0xFF4F46E5), Color(0xFF059669), Color(0xFFD97706), Color(0xFFDB2777))
    val accent = colors[index % colors.size]

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).border(3.dp, accent, CircleShape).background(MaterialTheme.colorScheme.background))
            if (!isLast) Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(accent.copy(alpha = 0.2f)))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(subject, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text("$time • $teacher", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FlatDeadlineRow(title: String, date: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFEF4444))) // Red dot
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Due: $date", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumFeeActionCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(20.dp)).background(Primary.copy(alpha = 0.08f)).clickable { onClick() }.padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ShoppingCart, null, tint = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("View Dues & Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Zero-commission UPI", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Primary)
    }
}

@Composable
private fun PremiumEmptyState(title: String, subtitle: String, icon: ImageVector) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumSectionTitle(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        if (actionLabel.isNotBlank()) {
            Text(text = actionLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.clickable { onAction() })
        }
    }
}
'''

new_lines = lines[:181] + [new_ui_lines + '\n'] + lines[383:] + [premium_components + '\n']

with codecs.open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("Rewrite successful")
