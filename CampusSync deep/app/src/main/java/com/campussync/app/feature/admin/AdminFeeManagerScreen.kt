package com.campussync.app.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.campussync.app.ui.components.bounceClick
import com.campussync.app.ui.components.shimmerEffect
import com.campussync.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AdminFeeManagerScreen() {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Fee Manager",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Admin Summary Overview
            AdminOverviewSection(isLoading = isLoading)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Fee Types",
                    style = MaterialTheme.typography.titleLarge
                )
                
                TextButton(onClick = { /* Create New Fee Type */ }) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "New Type")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(4) { FeeTypeSkeleton() }
                }
            } else {
                val feeTypes = listOf(
                    FeeType("Tuition", "240 Active", Icons.Rounded.List, BrandPrimary, BrandPrimaryLight),
                    FeeType("Hostel", "120 Active", Icons.Rounded.Home, StatusInfo, StatusInfoBackground),
                    FeeType("Library", "30 Pending", Icons.Rounded.Face, StatusWarning, StatusWarningBackground),
                    FeeType("Events", "5 Active", Icons.Rounded.Star, StatusSuccess, StatusSuccessBackground)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(feeTypes) { type ->
                        FeeTypeCard(type)
                    }
                    
                    // Add New Item Box
                    item {
                        CreateNewFeeTypeCard()
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOverviewSection(isLoading: Boolean) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Total Collected (This Month)",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandPrimaryLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.height(40.dp).width(150.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            } else {
                Text(
                    text = "$45,200.00",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SurfaceWhite
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BrandPrimaryLight.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Pending Approvals", style = MaterialTheme.typography.labelSmall, color = BrandPrimaryLight)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLoading) {
                         Box(modifier = Modifier.height(20.dp).width(40.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    } else {
                        Text(text = "12", style = MaterialTheme.typography.titleMedium, color = SurfaceWhite)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Pending Amount", style = MaterialTheme.typography.labelSmall, color = BrandPrimaryLight)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLoading) {
                         Box(modifier = Modifier.height(20.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    } else {
                        Text(text = "$8,400.00", style = MaterialTheme.typography.titleMedium, color = StatusWarning)
                    }
                }
            }
        }
    }
}

data class FeeType(val title: String, val subtitle: String, val icon: ImageVector, val iconColor: Color, val bgColor: Color)

@Composable
fun FeeTypeCard(type: FeeType) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .bounceClick { /* Navigate to detail */ },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(type.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = type.icon, contentDescription = null, tint = type.iconColor)
            }
            
            Column {
                Text(text = type.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = type.subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun CreateNewFeeTypeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, BorderLight, RoundedCornerShape(20.dp))
            .bounceClick { /* Create New */ },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add New",
                tint = TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add New",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun FeeTypeSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).shimmerEffect())
            Column {
                Box(modifier = Modifier.height(20.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.height(14.dp).width(60.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
        }
    }
}
