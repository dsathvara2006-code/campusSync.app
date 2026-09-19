package com.campussync.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.theme.Primary

@Composable
fun GenericStateScreen(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(32.dp))
            PrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
fun OfflineState(onRetry: () -> Unit) {
    GenericStateScreen(
        icon = Icons.Rounded.WifiOff,
        iconTint = Color(0xFF9E9E9E),
        title = "No Connection",
        message = "It looks like you're offline. Please check your internet connection and try again. Don't worry, your offline changes are saved locally.",
        actionText = "Retry",
        onAction = onRetry
    )
}

@Composable
fun PermissionDeniedState(onGoBack: () -> Unit) {
    GenericStateScreen(
        icon = Icons.Rounded.Lock,
        iconTint = Color(0xFFEF4444),
        title = "Access Denied",
        message = "You don't have the required permissions to view this content.",
        actionText = "Go Back",
        onAction = onGoBack
    )
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    GenericStateScreen(
        icon = Icons.Rounded.ErrorOutline,
        iconTint = Color(0xFFEF4444),
        title = "Oops! Something went wrong",
        message = message,
        actionText = "Try Again",
        onAction = onRetry
    )
}

@Composable
fun SuccessState(message: String, onContinue: () -> Unit) {
    GenericStateScreen(
        icon = Icons.Rounded.CheckCircleOutline,
        iconTint = Color(0xFF4ADE80),
        title = "Success!",
        message = message,
        actionText = "Continue",
        onAction = onContinue
    )
}

@Composable
fun MaintenanceState() {
    GenericStateScreen(
        icon = Icons.Rounded.Build,
        iconTint = Primary,
        title = "Under Maintenance",
        message = "CampusSync is currently undergoing scheduled maintenance to improve your experience. We'll be back shortly."
    )
}

@Composable
fun NoResultsState(query: String = "", onClear: (() -> Unit)? = null) {
    GenericStateScreen(
        icon = Icons.Rounded.SearchOff,
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        title = "No Results Found",
        message = if (query.isNotEmpty()) "We couldn't find anything matching '$query'." else "There's nothing here yet.",
        actionText = if (onClear != null) "Clear Search" else null,
        onAction = onClear
    )
}
