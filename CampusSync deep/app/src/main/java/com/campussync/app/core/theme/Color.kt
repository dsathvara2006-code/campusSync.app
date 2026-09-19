package com.campussync.app.core.theme

import androidx.compose.ui.graphics.Color

/**
 * ============================================================
 *  CAMPUSSYNC — "NOVA" DESIGN SYSTEM
 *  A calm, premium, edtech-grade palette. Indigo/Violet brand,
 *  soft neutral surfaces, high-contrast semantic colors.
 * ============================================================
 */

// ---------- Brand ramp ----------
val Indigo50  = Color(0xFFEFF1FF)
val Indigo100 = Color(0xFFDEE1FF)
val Indigo200 = Color(0xFFB8BEFF)
val Indigo300 = Color(0xFF8D93F7)
val Indigo400 = Color(0xFF6C6FF2)
val Indigo500 = Color(0xFF4F46E5)   // Core brand
val Indigo600 = Color(0xFF4338CA)
val Indigo700 = Color(0xFF362FA0)
val Indigo800 = Color(0xFF272166)
val Indigo900 = Color(0xFF181343)

val Violet400 = Color(0xFFA78BFA)
val Violet500 = Color(0xFF8B5CF6)
val Cyan400   = Color(0xFF22D3EE)
val Cyan500   = Color(0xFF06B6D4)

// ---------- Semantic ----------
val Emerald500 = Color(0xFF12B76A)
val Emerald100 = Color(0xFFD8F6E4)
val Amber500   = Color(0xFFF59E0B)
val Amber100   = Color(0xFFFEF0D5)
val Rose500    = Color(0xFFF04438)
val Rose100    = Color(0xFFFDE4E1)
val Sky500     = Color(0xFF0BA5EC)
val Sky100     = Color(0xFFE0F5FF)

// ---------- Neutrals — Light ----------
val Slate25  = Color(0xFFFCFCFF)
val Slate50  = Color(0xFFF7F8FC)
val Slate100 = Color(0xFFEFF1F7)
val Slate200 = Color(0xFFE2E5F0)
val Slate300 = Color(0xFFCBD0E1)
val Slate400 = Color(0xFF9AA2BF)
val Slate500 = Color(0xFF6B7291)
val Slate600 = Color(0xFF4E5471)
val Slate700 = Color(0xFF383D57)
val Slate900 = Color(0xFF12142B)

// ---------- Neutrals — Dark ----------
val Night950 = Color(0xFF0A0B16)
val Night900 = Color(0xFF11132200)
val NightBg  = Color(0xFF0D0E1C)
val Night800 = Color(0xFF161829)
val Night700 = Color(0xFF1F2236)
val Night600 = Color(0xFF2A2D45)
val NightBorder = Color(0x1FFFFFFF)

// ============================================================
//  App-facing tokens — referenced throughout the composables.
//  Keeping these names stable so every screen resolves colors
//  from ONE place (this file) instead of scattered hex values.
// ============================================================

// Brand
val Primary       = Indigo500
val PrimaryDark   = Indigo700
val PrimaryLight  = Indigo100
val Secondary     = Violet500
val Accent        = Cyan500

// Semantic
val Success           = Emerald500
val SuccessBackground = Emerald100
val Warning           = Amber500
val WarningBackground = Amber100
val Error             = Rose500
val ErrorBackground   = Rose100
val Info              = Sky500
val InfoBackground    = Sky100

// Legacy aliases — kept so existing references across the codebase
// (StatusSuccess, BrandPrimary, TextPrimary, etc.) keep compiling.
val BrandPrimary            = Primary
val BrandPrimaryLight       = PrimaryLight
val StatusSuccess           = Success
val StatusSuccessBackground = SuccessBackground
val StatusWarning           = Warning
val StatusWarningBackground = WarningBackground
val StatusError             = Error
val StatusErrorBackground   = ErrorBackground
val StatusInfo              = Info
val StatusInfoBackground    = InfoBackground

val TextPrimary: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onBackground

val TextSecondary: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

val BorderLight: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant

val SurfaceWhite: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.surface

// Admin/chart accents
val NeonPurple = Violet500
val NeonBlue   = Sky500
val NeonGreen  = Emerald500
val NeonOrange = Amber500
val NeonRed    = Rose500
val NeonCyan   = Cyan500
val ChartTeal  = Color(0xFF14B8A6)
val ChartBlue  = Sky500
val ChartPurple = Violet500
val DarkSurfaceHover  = Night600
val GlassSurfaceLight = Color(0x14FFFFFF)