package com.campussync.app.feature.fees

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campussync.app.core.components.GlassCard
import com.campussync.app.core.components.GlassMonogram
import com.campussync.app.core.components.GlassSectionHeader
import com.campussync.app.core.components.GlassScreen
import com.campussync.app.core.components.glassHairline
import com.campussync.app.core.model.FeeType
import com.campussync.app.core.model.User
import com.campussync.app.feature.auth.CollegeClass
import com.campussync.app.core.theme.Primary
import com.campussync.app.core.theme.Secondary
import com.campussync.app.core.theme.Success
import kotlinx.coroutines.launch

@Composable
fun AssignFeeScreen(
    currentUser: User,
    onBack: () -> Unit
) {
    val viewModel: AssignFeeViewModel = viewModel()
    val feeTypes by viewModel.feeTypes.collectAsState()
    val students by viewModel.students.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var assignMode by remember { mutableStateOf("Student") } // "Student" or "Class"
    var studentSearchQuery by remember { mutableStateOf("") }

    var selectedFeeType by remember { mutableStateOf<FeeType?>(null) }
    var selectedStudent by remember { mutableStateOf<User?>(null) }
    var selectedClass by remember { mutableStateOf<CollegeClass?>(null) }
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L) }

    val filteredStudents = remember(students, studentSearchQuery) {
        if (studentSearchQuery.isBlank()) {
            students
        } else {
            students.filter {
                it.name.contains(studentSearchQuery, ignoreCase = true) ||
                it.rollNo.contains(studentSearchQuery, ignoreCase = true) ||
                it.classId.contains(studentSearchQuery, ignoreCase = true)
            }
        }
    }

    fun onShowSnackbar(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(currentUser.collegeId) {
        if (currentUser.collegeId.isNotBlank()) {
            viewModel.loadInitialData(currentUser.collegeId)
        }
    }

    val selectionReady = selectedFeeType != null &&
        ((assignMode == "Student" && selectedStudent != null) || (assignMode == "Class" && selectedClass != null))

    GlassScreen(
        title = "Assign Fee",
        subtitle = "Pick a template, then choose who pays it",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectionReady) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, glassHairline()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (assignMode == "Student" && selectedStudent != null && selectedFeeType != null) {
                                viewModel.assignFeeToStudent(
                                    collegeId = currentUser.collegeId,
                                    student = selectedStudent!!,
                                    feeType = selectedFeeType!!,
                                    dueDate = dueDateMillis
                                ) { success, msg ->
                                    if (success) {
                                        selectedStudent = null
                                    }
                                    onShowSnackbar(msg ?: (if (success) "Success" else "Failed"))
                                }
                            } else if (assignMode == "Class" && selectedClass != null && selectedFeeType != null) {
                                viewModel.assignFeeToClass(
                                    collegeId = currentUser.collegeId,
                                    classId = selectedClass!!.classId,
                                    className = "${selectedClass!!.course} - ${selectedClass!!.semester} - ${selectedClass!!.className}",
                                    feeType = selectedFeeType!!,
                                    dueDate = dueDateMillis
                                ) { success, msg ->
                                    if (success) {
                                        selectedClass = null
                                    }
                                    onShowSnackbar(msg ?: (if (success) "Success" else "Failed"))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(54.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        val assignText = if (assignMode == "Student") {
                            "Assign ₹${selectedFeeType?.amount ?: ""} to ${selectedStudent?.name ?: ""}"
                        } else {
                            "Assign ₹${selectedFeeType?.amount ?: ""} to ${selectedClass?.course ?: ""} ${selectedClass?.className ?: ""}"
                        }
                        Text(if (isLoading) "Assigning..." else assignText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { _ ->
        if (isLoading && feeTypes.isEmpty() && students.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Step 1 ──
                item {
                    GlassSectionHeader(title = "1. Select Fee Template")
                    Spacer(Modifier.height(8.dp))
                }

                if (feeTypes.isEmpty()) {
                    item {
                        GlassCard {
                            Text(
                                text = "No fee templates available. Please create them first.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(feeTypes) { feeType ->
                        val isSelected = selectedFeeType?.id == feeType.id
                        GlassCard(
                            onClick = {
                                selectedFeeType = if (selectedFeeType?.id == feeType.id) null else feeType
                            },
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Brush.linearGradient(listOf(Primary, Secondary))
                                            else Brush.linearGradient(listOf(Primary.copy(alpha = 0.12f), Primary.copy(alpha = 0.06f)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "₹",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else Primary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = feeType.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text("₹${feeType.amount}", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Step 2 ──
                item {
                    Spacer(Modifier.height(10.dp))
                    GlassSectionHeader(title = "2. Select Target")
                    Spacer(Modifier.height(10.dp))

                    // Mode toggle — segmented control
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
                            listOf("Student" to Icons.Rounded.Face, "Class" to Icons.Rounded.Class).forEach { (mode, icon) ->
                                val isSelected = assignMode == mode
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Brush.linearGradient(listOf(Primary, Secondary))
                                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                        )
                                        .clickable {
                                            assignMode = mode
                                            if (mode == "Student") selectedClass = null else selectedStudent = null
                                        },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (mode == "Student") "Individual Student" else "Entire Class",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (assignMode == "Student") {
                    item {
                        OutlinedTextField(
                            value = studentSearchQuery,
                            onValueChange = { studentSearchQuery = it },
                            placeholder = { Text("Search by name, roll no, or class...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (studentSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { studentSearchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    if (filteredStudents.isEmpty()) {
                        item {
                            GlassCard {
                                Text(
                                    text = if (students.isEmpty())
                                        "No students found. Students appear here after they sign up using their invites."
                                    else "No students match your search.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(filteredStudents) { student ->
                            val isSelected = selectedStudent?.userId == student.userId
                            SelectablePersonRow(
                                title = student.name,
                                subtitle = "${student.classId}  •  Roll: ${student.rollNo}",
                                monogram = student.name,
                                isSelected = isSelected,
                                onClick = {
                                    selectedStudent = if (selectedStudent?.userId == student.userId) null else student
                                }
                            )
                        }
                    }
                } else {
                    if (classes.isEmpty()) {
                        item {
                            GlassCard {
                                Text(
                                    text = "No classes found in the system.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(classes) { c ->
                            val isSelected = selectedClass?.classId == c.classId
                            SelectablePersonRow(
                                title = "${c.course} - ${c.className}",
                                subtitle = "Semester: ${c.semester}",
                                monogram = c.className.ifBlank { "C" },
                                isSelected = isSelected,
                                accent = Secondary,
                                onClick = {
                                    selectedClass = if (selectedClass?.classId == c.classId) null else c
                                }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/** Shared selectable row for students & classes (glass + check indicator). */
@Composable
private fun SelectablePersonRow(
    title: String,
    subtitle: String,
    monogram: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accent: Color = Primary
) {
    GlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassMonogram(name = monogram, accent = accent, size = 38, fontSize = 14)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
