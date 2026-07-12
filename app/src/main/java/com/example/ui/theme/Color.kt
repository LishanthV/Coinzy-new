package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bento Grid Design Color Palette
val BentoBg = Color(0xFFFDF8F6)        // Warm cream/peach background
val BentoText = Color(0xFF1F1B1B)      // Soft near-black text
val BentoBorder = Color(0xFFCAC4D0)    // Muted grey-purple border

// Bento Card Colors
val BentoLavender = Color(0xFFEADDFF)  // Light violet card
val BentoDeepViolet = Color(0xFF21005D) // Deep violet text for lavender card

val BentoBlue = Color(0xFFD3E3FD)      // Light blue card
val BentoDeepNavy = Color(0xFF001D35)   // Deep navy text for blue card

val BentoPink = Color(0xFFFFD8E4)      // Light pink card
val BentoDeepRose = Color(0xFF31111D)   // Deep rose text for pink card

val BentoGrayPink = Color(0xFFF3EDEF)   // Navigation & secondary item container
val BentoAccent = Color(0xFF6750A4)     // Primary purple accent
val BentoOnAccent = Color(0xFFFFFFFF)

// Material Theme Mappings for Light / Bento Default Theme
val LightPrimary = BentoAccent
val LightSecondary = Color(0xFF49454F)
val LightTertiary = BentoLavender
val LightBackground = BentoBg
val LightSurface = Color.White
val LightOnBackground = BentoText
val LightOnSurface = BentoText

// Material Theme Mappings for Dark Theme (styled gracefully with Bento colors)
val DarkPrimary = Color(0xFFD0BCFF)
val DarkSecondary = Color(0xFFCCC2DC)
val DarkTertiary = BentoPink
val DarkBackground = Color(0xFF141218)
val DarkSurface = Color(0xFF1D1B20)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkOnSurface = Color(0xFFE6E1E5)
