package com.campussync.app.feature.resources

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.components.ShimmerResourceCard
import com.campussync.app.core.model.Resource
import com.campussync.app.core.model.User
import com.campussync.app.core.theme.Error
import com.campussync.app.core.theme.Primary

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val subjectColors = listOf(
    Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFF059669),
    Color(0xFFD97706), Color(0xFFDC2626), Color(0xFF0891B2), Color(0xFFDB2777)
)

private fun getSubjectColor(subjectName: String): Color {
    val index = kotlin.math.abs(subjectName.hashCode()) % subjectColors.size
    return subjectColors[index]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    user: User,
    viewModel: ResourcesViewModel,
    onBack: () -> Unit,
    onNavigateToUpload: () -> Unit
) {
    val context = LocalContext.current
    val isTeacher = user.role.equals("teacher", ignoreCase = true)
    val branch = "CSE"
    val semester = "3"

    // Crucial Fix: Use user.classId as the key. If it changes (or is properly loaded), it re-runs.
    LaunchedEffect(user.collegeId, user.classId) {
        if (user.classId.isNotBlank()) {
            viewModel.fetchResources(user.collegeId, user.classId, branch, semester)
        } else {
            Toast.makeText(context, "No class assigned. Cannot fetch resources.", Toast.LENGTH_LONG).show()
        }
    }

    // collectAsState() works fine, but we ensure the ViewModel emits a .toList() copy so this ALWAYS triggers
    val resources by viewModel.resources.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var activeSubjectFolder by remember { mutableStateOf<String?>(null) }
    var folderSearchQuery by remember { mutableStateOf("") }
    var fileSearchQuery by remember { mutableStateOf("") }
    var selectedFileType by remember { mutableStateOf("All") }

    var showAddDialogForSubject by remember { mutableStateOf<String?>(null) }
    var resourceToDelete by remember { mutableStateOf<Resource?>(null) }
    var resourceToEdit by remember { mutableStateOf<Resource?>(null) }

    BackHandler(enabled = activeSubjectFolder != null) {
        activeSubjectFolder = null
    }

    val availableSubjects = remember(resources) {
        resources.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredFolders = remember(availableSubjects, folderSearchQuery) {
        if (folderSearchQuery.isBlank()) availableSubjects
        else availableSubjects.filter { it.contains(folderSearchQuery, ignoreCase = true) }
    }

    val activeSubjectMaterials = remember(resources, activeSubjectFolder, fileSearchQuery, selectedFileType) {
        if (activeSubjectFolder == null) emptyList()
        else resources.filter { res ->
            res.subject.equals(activeSubjectFolder, ignoreCase = true) &&
            (fileSearchQuery.isBlank() || res.title.contains(fileSearchQuery, ignoreCase = true) || res.description.contains(fileSearchQuery, ignoreCase = true)) &&
            (selectedFileType == "All" || res.fileType.equals(selectedFileType, ignoreCase = true))
        }
    }

    val fileTypes = listOf("All", "PDF", "PPT", "DOC", "VIDEO", "LINK")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (activeSubjectFolder != null) "$activeSubjectFolder Material" else "Study Resources", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (activeSubjectFolder != null) activeSubjectFolder = null else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (user.classId.isNotBlank()) {
                            viewModel.fetchResources(user.collegeId, user.classId, branch, semester) 
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (user.role.equals("teacher", ignoreCase = true) || user.role.equals("admin", ignoreCase = true) || user.role.equals("principal", ignoreCase = true) || user.role.equals("tenant_admin", ignoreCase = true)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (user.classId.isBlank()) {
                            Toast.makeText(context, "Error: You are not assigned to a class. Cannot upload.", Toast.LENGTH_LONG).show()
                            return@ExtendedFloatingActionButton
                        }
                        if (activeSubjectFolder != null) showAddDialogForSubject = activeSubjectFolder
                        else onNavigateToUpload()
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text(text = if (activeSubjectFolder != null) "Add Resource" else "Share Resource", fontWeight = FontWeight.Bold) },
                    containerColor = Primary,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (activeSubjectFolder == null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = folderSearchQuery,
                        onValueChange = { folderSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = { Text("Search subject folders...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    if (isLoading && resources.isEmpty()) {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                            items(6) { ShimmerResourceCard() }
                        }
                    } else if (filteredFolders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = if (user.classId.isBlank()) "No class assigned." else "No subject folders found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                            items(filteredFolders, key = { it }) { subjectName ->
                                val count = resources.count { it.subject.equals(subjectName, ignoreCase = true) }
                                FolderGridCard(subjectName, count) { fileSearchQuery = ""; selectedFileType = "All"; activeSubjectFolder = subjectName }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = fileSearchQuery,
                        onValueChange = { fileSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = { Text("Search in $activeSubjectFolder...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        items(fileTypes) { type ->
                            FilterChip(selected = type == selectedFileType, onClick = { selectedFileType = type }, label = { Text(type, fontWeight = if (type == selectedFileType) FontWeight.Bold else FontWeight.Normal) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (activeSubjectMaterials.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Info, null, tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No study materials found for this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(activeSubjectMaterials, key = { it.resourceId }) { resource ->
                                MaterialCard(resource, isTeacher, { viewModel.likeResource(user.collegeId, user.classId, resource.resourceId, branch, semester) }, { resourceToEdit = resource }, { resourceToDelete = resource })
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
    if (showAddDialogForSubject != null) AddResourceDialog(showAddDialogForSubject!!, user, viewModel) { showAddDialogForSubject = null }
    if (resourceToEdit != null) EditResourceDialog(user, resourceToEdit!!, viewModel) { resourceToEdit = null }
    if (resourceToDelete != null) {
        AlertDialog(
            onDismissRequest = { resourceToDelete = null },
            title = { Text("Delete Resource", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${resourceToDelete?.title}'?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteResource(user.collegeId, user.classId, resourceToDelete?.resourceId ?: ""); resourceToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Error)) { Text("Delete", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { resourceToDelete = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun FolderGridCard(subjectName: String, itemCount: Int, onClick: () -> Unit) {
    val accentColor = remember(subjectName) { getSubjectColor(subjectName) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "folderScale")
    Surface(modifier = Modifier.fillMaxWidth().height(130.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(20.dp)).clickable(interactionSource = interactionSource, indication = null) { onClick() }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)), shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Star, null, tint = accentColor, modifier = Modifier.size(24.dp)) }
                Surface(color = accentColor.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) { Text("$itemCount ${if (itemCount == 1) "File" else "Files"}", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
            }
            Column {
                Text(subjectName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tap to open", fontSize = 11.sp, color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), fontWeight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(resource: Resource, isTeacher: Boolean, onLike: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(resource.uploadTimestamp))
    var showMenu by remember { mutableStateOf(false) }
    val (icon, iconColor) = when (resource.fileType.uppercase()) {
        "PDF" -> Icons.Rounded.Info to Color(0xFFEF4444)
        "PPT" -> Icons.Rounded.Info to Color(0xFFF59E0B)
        "DOC" -> Icons.Rounded.Edit to Color(0xFF2563EB)
        "VIDEO" -> Icons.Rounded.PlayArrow to Color(0xFF7C3AED)
        else -> Icons.Rounded.Share to Primary
    }
    Surface(modifier = Modifier.fillMaxWidth().clickable { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.fileUrl))) } catch (e: Exception) { Toast.makeText(context, "Invalid link", Toast.LENGTH_SHORT).show() } }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), shadowElevation = 1.5.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${resource.fileType}  •  By ${resource.uploaderName}", style = MaterialTheme.typography.bodySmall, color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Favorite, null, tint = Primary, modifier = Modifier.size(20.dp)) }
                    Text("${resource.likesCount}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Primary)
                }
                if (isTeacher) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, null, tint = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Rounded.Edit, null) })
                            DropdownMenuItem(text = { Text("Delete", color = Error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Error) })
                        }
                    }
                }
            }
            if (resource.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(resource.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text("Shared on $dateStr", style = MaterialTheme.typography.bodySmall, color = (MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)), fontSize = 11.sp)
        }
    }
}

@Composable
private fun AddResourceDialog(subjectName: String, user: User, viewModel: ResourcesViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }; var driveLink by remember { mutableStateOf("") }; var fileType by remember { mutableStateOf("PDF") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Resource") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = driveLink, onValueChange = { driveLink = it }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(onClick = { viewModel.uploadResource(user.collegeId, user.classId, driveLink, user.userId, user.name, title, "", "CSE", "3", subjectName, listOf("Study"), fileType); onDismiss() }) { Text("Save") } })
}

@Composable
private fun EditResourceDialog(user: User, resource: Resource, viewModel: ResourcesViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(resource.title) }; var driveLink by remember { mutableStateOf(resource.fileUrl) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Resource") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = driveLink, onValueChange = { driveLink = it }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(onClick = { viewModel.updateResource(user.collegeId, user.classId, resource.resourceId, title, resource.description, driveLink, resource.fileType) {}; onDismiss() }) { Text("Update") } })
}