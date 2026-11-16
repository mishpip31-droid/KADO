package com.kado.app.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kado.app.data.CardRecord
import kotlin.math.abs

/** Real interactive fan: select to pull out; tap selected again to open. Swipe to navigate. */
@Composable
fun FanDeck(
    cards: List<CardRecord>,
    selectedId: String?,
    incomingId: String?,
    onSelect: (String) -> Unit,
    onOpen: (CardRecord) -> Unit,
    onSwipe: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = cards.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    // Five cards at once leave more room for every store's title.
    val visible = if (cards.size <= 5) cards else {
        val start = (center - 2).coerceIn(0, cards.size - 5)
        cards.subList(start, start + 5)
    }
    val selected = visible.firstOrNull { it.id == selectedId }
    var dragAmount by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(332.dp).pointerInput(cards.size, selectedId) {
            detectHorizontalDragGestures(
                onDragStart = { dragAmount = 0f },
                onHorizontalDrag = { change, delta -> dragAmount += delta; change.consume() },
                onDragEnd = {
                    if (abs(dragAmount) > 40f) onSwipe(if (dragAmount < 0f) 1 else -1)
                    dragAmount = 0f
                },
                onDragCancel = { dragAmount = 0f }
            )
        }
    ) {
        val dark = MaterialTheme.colorScheme.background != Color(0xFFFAF9FF)
        val holder = RoundedCornerShape(topStart = 100.dp, topEnd = 100.dp, bottomStart = 54.dp, bottomEnd = 54.dp)
        val front = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 54.dp, bottomEnd = 54.dp)
        Box(
            Modifier.fillMaxWidth().height(270.dp).align(Alignment.BottomCenter)
                .padding(horizontal = 13.dp)
                .shadow(elevation = 8.dp, shape = holder, clip = false)
                .clip(holder)
                .background(
                    Brush.verticalGradient(if (dark) listOf(Color(0xFF3B334E), Color(0xFF282234))
                    else listOf(Color(0xFFDAD1EC), Color(0xFFF0EAF9), Color(0xFFE2D9F0)))
                )
                .border(2.dp, Color.White.copy(alpha = 0.36f), holder)
        )
        val width = if (maxWidth < 350.dp) 142.dp else 154.dp
        val fanHeight = 226.dp
        val spread = if (visible.size <= 3) 25f else 18f
        val angle = if (visible.size <= 3) 18f else 14f
        visible.forEachIndexed { index, card ->
            val chosen = card.id == selectedId
            val distance = index - (visible.size - 1) / 2f
            val rotation by animateFloatAsState(
                targetValue = if (chosen) 0f else distance * angle,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
                label = "Поворот карты в веере"
            )
            val centerShift by animateFloatAsState(
                targetValue = if (chosen) -distance * spread else 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
                label = "Центрирование выдвинутой карты"
            )
            androidx.compose.runtime.key(card.id) {
                val insertion = remember(card.id) { Animatable(0f) }
                LaunchedEffect(incomingId, card.id) {
                    if (incomingId == card.id) {
                        insertion.snapTo(160f)
                        insertion.animateTo(0f, spring(dampingRatio = 0.70f, stiffness = 270f))
                    }
                }
                val slide by animateFloatAsState(
                    targetValue = if (chosen) -62f else 13f,
                    animationSpec = spring(dampingRatio = 0.73f, stiffness = 400f),
                    label = "Выдвижение карты"
                )
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .offset(x = (distance * spread).dp, y = (-12).dp)
                        .size(width = width, height = fanHeight)
                        .zIndex(if (chosen) 10f else index.toFloat())
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1.04f)
                            rotationZ = rotation
                            translationX = centerShift.dp.toPx()
                            translationY = (slide + insertion.value).dp.toPx()
                            scaleX = if (chosen) 1.04f else 1f
                            scaleY = if (chosen) 1.04f else 1f
                        }
                        .shadow(if (chosen) 18.dp else 7.dp, RoundedCornerShape(21.dp))
                        .clickable {
                            if (chosen) onOpen(card) else onSelect(card.id)
                        }
                ) {
                    CardFace(card, Modifier.matchParentSize())
                }
            }
        }
        // Opaque front wall covers the lower card area; the upper title bands
        // remain above it, like cards inserted into a real wallet.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(138.dp)
                .padding(horizontal = 14.dp)
                .shadow(10.dp, front, clip = false)
                .clip(front)
                .background(
                    Brush.verticalGradient(
                        if (dark) listOf(
                            Color(0xFF5B4E74), Color(0xFF433954), Color(0xFF2F293C)
                        ) else listOf(
                            Color(0xFFF7F3FC), Color(0xFFE9E1F4), Color(0xFFD4C9E6)
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.38f), front)
        )
        if (selected != null) Text(
            "Нажмите ещё раз, чтобы открыть ↗",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 10.sp, fontWeight = FontWeight.Medium
        )
    }
}
