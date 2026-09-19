package com.campussync.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campussync.app.ui.components.bounceClick
import com.campussync.app.ui.components.shimmerEffect
import com.campussync.app.ui.theme.*
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StudentDashboardScreen() {
    val viewModel: StudentDashboardViewModel = viewModel()
    val dashboardState by viewModel.dashboardState.collectAsState()

    // RULE 3: UI-Level Clean-Up (DisposableEffect)
    // Ensures heavy UI operations, broadcast receivers, or UI-bound listeners
    // are immediately cleaned up when this screen leaves the composition.
    DisposableEffect(Unit) {
        // Setup code (if any) runs here when screen enters composition
        // e.g., registering a local broadcast receiver, starting a UI animation timer

        onDispose {
            // Clean up code runs here when screen leaves composition (e.g., navigating to Admin panel)
            // e.g., unregistering receivers, cancelling timers
            println("StudentDashboardScreen destroyed: Releasing UI resources immediately to prevent Zombie UI!")
        }
    }

    val isLoading = dashboardState.isLoading

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp), // Let it breathe!
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Header Section
            item { DashboardHeader(isLoading = isLoading) }

            // Quick Stats Row
            item { QuickStatsRow(isLoading = isLoading) }

            // Pending Tasks / Fees Section
            item { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pending Actions",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    if (!isLoading && dashboardState.pendingActionCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("${dashboardState.pendingActionCount}", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                items(3) { ActionCardSkeleton() }
            } else {
                val pendingItems = listOf(
                    ActionItem("Library Fine", "Due in 2 days", "$15.00", StatusError, StatusErrorBackground, Icons.Rounded.Warning),
                    ActionItem("Registration Fee", "Fall 2026", "$1,200.00", StatusInfo, StatusInfoBackground, Icons.Rounded.Info)
                )

                if (pendingItems.isEmpty()) {
                    item { EmptyStateCard() }
                } else {
                    items(pendingItems) { item ->
                        ActionCard(item = item)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun DashboardHeader(isLoading: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good morning,",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(32.dp)
                        .width(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
            } else {
                Text(
                    text = "Alex Carter",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Profile Picture Placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(BrandPrimaryLight)
                .bounceClick {
                    android.widget.Toast.makeText(context, "Profile coming soon", android.widget.Toast.LENGTH_SHORT).show()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Profile",
                tint = BrandPrimary
            )
        }
    }
}

@Composable
fun QuickStatsRow(isLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Attendance",
            value = "94%",
            icon = Icons.Rounded.CheckCircle,
            color = StatusSuccess,
            bgColor = StatusSuccessBackground,
            isLoading = isLoading
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "CGPA",
            value = "3.8",
            icon = Icons.Rounded.Star,
            color = BrandPrimary,
            bgColor = BrandPrimaryLight,
            isLoading = isLoading
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    isLoading: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = modifier.bounceClick {
            android.widget.Toast.makeText(context, "$title details coming soon", android.widget.Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Subtle shadow
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(28.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

data class ActionItem(
    val title: String, 
    val subtitle: String, 
    val amount: String, 
    val iconColor: androidx.compose.ui.graphics.Color, 
    val iconBgColor: androidx.compose.ui.graphics.Color, 
    val icon: ImageVector
)

@Composable
fun ActionCard(item: ActionItem) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick {
                android.widget.Toast.makeText(context, "${item.title} payment coming soon", android.widget.Toast.LENGTH_SHORT).show()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(item.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = item.icon, contentDescription = null, tint = item.iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActionCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.height(20.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.height(16.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StatusSuccessBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = StatusSuccess,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = "No pending actions at the moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
