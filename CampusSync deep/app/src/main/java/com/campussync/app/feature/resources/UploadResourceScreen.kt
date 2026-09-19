package com.campussync.app.feature.resources

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campussync.app.core.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadResourceScreen(
    user: User,
    viewModel: ResourcesViewModel,
    initialSubject: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(initialSubject) }
    var driveLink by remember { mutableStateOf("") }
    
    var fileType by remember { mutableStateOf("PDF") }
    var expanded by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()

    LaunchedEffect(uploadStatus) {
        uploadStatus?.let { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Resource Shared Successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetUploadStatus()
                onBack()
            } else {
                Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                viewModel.resetUploadStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Resource Link") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (e.g., DBMS Unit 1 Notes)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = driveLink,
                onValueChange = { driveLink = it },
                label = { Text("Google Drive / Web Link") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = fileType,
                    onValueChange = { },
                    label = { Text("Resource Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text("PDF") }, onClick = { fileType = "PDF"; expanded = false })
                    DropdownMenuItem(text = { Text("PPT") }, onClick = { fileType = "PPT"; expanded = false })
                    DropdownMenuItem(text = { Text("DOC") }, onClick = { fileType = "DOC"; expanded = false })
                    DropdownMenuItem(text = { Text("VIDEO") }, onClick = { fileType = "VIDEO"; expanded = false })
                    DropdownMenuItem(text = { Text("LINK") }, onClick = { fileType = "LINK"; expanded = false })
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    if (title.isNotBlank() && subject.isNotBlank() && driveLink.isNotBlank()) {
                        viewModel.uploadResource(
                            collegeId = user.collegeId,
                            classId = user.classId, // Now passing classId to strictly isolate uploads
                            driveLink = driveLink.trim(),
                            uploaderId = user.userId,
                            uploaderName = user.name.ifBlank { "Student" },
                            title = title,
                            description = description,
                            branch = "CSE", // hardcoded for demo
                            semester = "3", // hardcoded for demo
                            subject = subject.trim(),
                            tags = listOf("Notes"),
                            fileType = fileType
                        )
                    } else {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && title.isNotBlank() && subject.isNotBlank() && driveLink.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Share Resource")
                }
            }
        }
    }
}
