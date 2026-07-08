package com.karuhundeveloper.notification.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karuhundeveloper.notification.ui.theme.*

/**
 * Coloured badge showing the HTTP method (POST / GET / PUT / PATCH / DELETE).
 * Shared between [MainScreen] (rule cards) and [AddRuleScreen] (method dropdown).
 */
@Composable
fun MethodBadge(method: String) {
    val (bg, fg) = when (method.uppercase()) {
        "POST"   -> Color(0xFF1A3A2A) to SuccessGreen
        "GET"    -> Color(0xFF1A2A3A) to CyanAccent
        "PUT"    -> Color(0xFF2A2A1A) to Color(0xFFFFD166)
        "PATCH"  -> Color(0xFF2A1A2A) to Color(0xFFCC88FF)
        "DELETE" -> Color(0xFF3A1A1A) to ErrorRed
        else     -> NavyElevated      to TextSecondary
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            method.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            letterSpacing = 0.5.sp
        )
    }
}
