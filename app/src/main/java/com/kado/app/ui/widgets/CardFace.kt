package com.kado.app.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kado.app.data.CardRecord

@Composable
fun CardFace(card: CardRecord, modifier: Modifier = Modifier, compact: Boolean = false) {
    val base = Color(card.colorArgb)
    val shape = RoundedCornerShape(if (compact) 18.dp else 24.dp)
    Box(
        modifier = modifier.clip(shape).background(
            Brush.linearGradient(
                listOf(base.copy(alpha = 0.92f), base, base.copy(alpha = 0.76f)),
                start = Offset(0f, 0f), end = Offset(900f, 950f)
            )
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color.White.copy(alpha = 0.10f), radius = w * 0.85f, center = Offset(w * 0.88f, h * 1.20f))
            drawCircle(Color.White.copy(alpha = 0.13f), radius = w * 0.65f, center = Offset(w * 0.87f, h * 1.20f), style = Stroke(width = w * 0.22f))
            drawCircle(Color.White.copy(alpha = 0.07f), radius = w * 0.42f, center = Offset(w * 1.03f, h * 0.02f))
        }
        Column(
            Modifier.fillMaxSize().padding(if (compact) 13.dp else 19.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(if (compact) 28.dp else 36.dp)
                        .background(Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(card.symbol, color = Color.White, fontSize = if (compact) 17.sp else 21.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    card.title, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 15.sp else 20.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = if (compact) 17.sp else 23.sp
                )
            }
            Column {
                Text(
                    "KADO", color = Color.White.copy(alpha = 0.72f),
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp
                )
                if (!compact) Text(
                    "Карта лояльности", color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp
                )
            }
        }
    }
}
