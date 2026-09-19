package com.campussync.app.feature.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campussync.app.core.theme.*
import com.campussync.app.core.utils.NetworkUtils
import kotlinx.coroutines.delay

/**
 * Splash — calm brand moment. A single glowing "mark" on a deep indigo
 * field, rather than the old flat navy block. Everything else (offline
 * detection, staged reveal, fade-out finish) is preserved 1:1.
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var showOfflineWarning by remember { mutableStateOf(false) }

    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var dotsVisible by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }

    val screenAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(500, easing = FastOutLinearInEasing),
        label = "screenFade",
        finishedListener = { if (fadeOut) onFinish() }
    )

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600), label = "logoAlpha"
    )

    val infinite = rememberInfiniteTransition(label = "pulse")
    val ringScale by infinite.animateFloat(
        0.94f, 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ringScale"
    )
    val glowAlpha by infinite.animateFloat(
        0.25f, 0.55f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val dot1 by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(keyframes { durationMillis = 1200; 0.25f at 0; 1f at 200; 0.25f at 600 }), label = "d1")
    val dot2 by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(keyframes { durationMillis = 1200; 0.25f at 200; 1f at 400; 0.25f at 800 }), label = "d2")
    val dot3 by infinite.animateFloat(0.25f, 1f, infiniteRepeatable(keyframes { durationMillis = 1200; 0.25f at 400; 1f at 600; 0.25f at 1000 }), label = "d3")

    LaunchedEffect(Unit) {
        delay(100); logoVisible = true
        delay(400); textVisible = true
        delay(200); subtitleVisible = true
        delay(300); dotsVisible = true

        val isOnline = NetworkUtils.isInternetAvailable(context)
        if (!isOnline) {
            delay(800); showOfflineWarning = true; delay(2000)
        } else {
            delay(1400)
        }
        fadeOut = true
    }

    val bgBrush = Brush.verticalGradient(listOf(Indigo900, Night950, Indigo900))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .graphicsLayer { alpha = screenAlpha },
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient blobs
        Box(Modifier.size(280.dp).offset(x = (-90).dp, y = (-160).dp).clip(CircleShape).background(Violet500.copy(alpha = 0.18f)).blur(70.dp))
        Box(Modifier.size(240.dp).offset(x = 100.dp, y = 180.dp).clip(CircleShape).background(Cyan500.copy(alpha = 0.14f)).blur(70.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {

            Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer {
                scaleX = logoScale; scaleY = logoScale; alpha = logoAlpha
            }) {
                Box(
                    Modifier.size(168.dp).scale(ringScale).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Violet500.copy(alpha = glowAlpha), Color.Transparent)))
                )
                Box(
                    Modifier.size(120.dp).clip(RoundedCornerShape(32.dp))
                        .background(Brush.linearGradient(listOf(Indigo400, Violet500)))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = "CampusSync", tint = Color.White, modifier = Modifier.size(56.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible = textVisible,
                enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(400))
            ) {
                Text("CampusSync", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
            }

            Spacer(Modifier.height(6.dp))

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(500)) + fadeIn(tween(500))
            ) {
                Text(
                    "Everything campus, in one place",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(52.dp))

            AnimatedVisibility(visible = dotsVisible && !showOfflineWarning, enter = fadeIn(tween(400))) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(dot1, dot2, dot3).forEach { a ->
                        Box(Modifier.size(7.dp).clip(CircleShape).alpha(a).background(Violet400))
                    }
                }
            }

            AnimatedVisibility(visible = showOfflineWarning, enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(300))) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Rose500.copy(alpha = 0.85f)).padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Offline", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("No internet • Using cached data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Text(
            "v1.0", color = Color.White.copy(alpha = 0.22f), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        )
    }
}