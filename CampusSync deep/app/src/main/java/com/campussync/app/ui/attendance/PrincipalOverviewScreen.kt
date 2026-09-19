package com.campussync.app.feature.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.theme.Primary
import com.campussync.app.core.theme.Success
import com.campussync.app.core.theme.Error
import com.campussync.app.core.theme.TextPrimary
import com.campussync.app.core.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalOverviewScreen(
    collegeId: String,
    onBack: () -> Unit,
    viewModel: PrincipalOverviewViewModel = viewModel()
) {
    val classes by viewModel.classes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedClassAttendance by viewModel.selectedClassAttendance.collectAsState()
    val selectedClassStudents by viewModel.selectedClassStudents.collectAsState()

    var selectedClass by remember { mutableStateOf<ClassOverview?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(collegeId) {
        viewModel.loadClasses(collegeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("College Overview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "All Classes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (classes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No classes found in the college.", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(classes) { classItem ->
                        ClassOverviewCard(classItem = classItem, onClick = {
                            selectedClass = classItem
                            viewModel.loadClassDetails(collegeId, classItem.classId)
                        })
                    }
                }
            }
        }
    }

    if (selectedClass != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedClass = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "${selectedClass!!.name} - Recent Attendance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedClassAttendance.isEmpty()) {
                    Text("No recent attendance records found.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        items(selectedClassAttendance) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(record.subjectName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Text(record.date, fontSize = 12.sp, color = TextSecondary)
                                    }
                                    val percent = if (record.total > 0) (record.present * 100) / record.total else 0
                                    val color = if (percent >= 75) Success else if (percent >= 60) Color(0xFFF57C00) else Error
                                    Text(
                                        text = "$percent%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Enrolled Students",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedClassStudents.isEmpty()) {
                    Text("No students enrolled yet.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp)) {
                        items(selectedClassStudents) { student ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(student.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Text(student.rollNo, fontSize = 13.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ClassOverviewCard(classItem: ClassOverview, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = classItem.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                if (classItem.branch.isNotBlank() && classItem.semester.isNotBlank()) {
                    Text(text = "${classItem.branch} • Sem ${classItem.semester}", fontSize = 13.sp, color = TextSecondary)
                } else if (classItem.semester.isNotBlank()) {
                    Text(text = "Sem ${classItem.semester}", fontSize = 13.sp, color = TextSecondary)
                } else if (classItem.branch.isNotBlank()) {
                    Text(text = classItem.branch, fontSize = 13.sp, color = TextSecondary)
                }
            }
            Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "View", tint = TextSecondary)
        }
    }
}
