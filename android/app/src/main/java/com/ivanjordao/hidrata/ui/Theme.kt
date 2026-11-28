package com.ivanjordao.hidrata.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** As mesmas cores das variáveis CSS (:root) do index.html. */
object C {
    val bg1 = Color(0xFF071330)
    val bg2 = Color(0xFF0D2B5E)
    val card = Color(0x0FFFFFFF)      // rgba(255,255,255,.06)
    val card2 = Color(0x1AFFFFFF)     // rgba(255,255,255,.10)
    val stroke = Color(0x1AFFFFFF)
    val text = Color(0xFFEAF2FF)
    val muted = Color(0xFF93A8CF)
    val w1 = Color(0xFF4FC3FF)
    val w2 = Color(0xFF1D4ED8)
    val ok = Color(0xFF34D399)
    val warn = Color(0xFFFBBF24)
    val bad = Color(0xFFFB7185)
    val sheet = Color(0xFF10234D)
}

@Composable
fun HidrataTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.w1,
            onPrimary = Color.White,
            secondary = C.w2,
            background = C.bg1,
            surface = C.sheet,
            onSurface = C.text,
            onBackground = C.text,
            error = C.bad,
        ),
        content = content,
    )
}
