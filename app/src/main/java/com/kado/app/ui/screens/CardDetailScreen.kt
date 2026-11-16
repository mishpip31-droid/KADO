package com.kado.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kado.app.barcode.BarcodeTools
import com.kado.app.data.BarcodeKind
import com.kado.app.data.CardRecord
import com.kado.app.ui.theme.Muted
import com.kado.app.ui.widgets.CardFace

@Composable
fun CardDetailScreen(
    card: CardRecord,
    onBack: () -> Unit,
    onFullscreen: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            Text(card.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onFavorite) {
                Icon(if (card.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Избранное", tint = if (card.favorite) Color(0xFFF3A923) else MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Редактировать") }
        }
        Spacer(Modifier.height(18.dp))
        CardFace(card, modifier = Modifier.fillMaxWidth().height(175.dp))
        Spacer(Modifier.height(22.dp))
        Text("Код для кассы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CodeImage(card, modifier = Modifier.fillMaxWidth().height(if (card.kind.isSquare()) 240.dp else 155.dp))
                Text(card.value, color = Color(0xFF282238), fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFullscreen, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(19.dp)) {
            Text("Показать код на весь экран", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { onCopy(card.value) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text("Скопировать номер")
        }
        Spacer(Modifier.height(18.dp))
        Text("Формат: ${card.kind.label}", color = Muted)
        Spacer(Modifier.height(15.dp))
        TextButton(onClick = { confirmDelete = true }) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text("Удалить карту", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Удалить карту?") },
        text = { Text("Карта «${card.title}» будет удалена с устройства. Отменить действие нельзя.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
    )
}

fun BarcodeKind.isSquare(): Boolean = this in setOf(BarcodeKind.QR_CODE, BarcodeKind.AZTEC, BarcodeKind.DATA_MATRIX)

@Composable
fun CodeImage(card: CardRecord, modifier: Modifier = Modifier) {
    val bitmap = remember(card.value, card.kind) {
        runCatching { BarcodeTools.toBitmap(card.value, card.kind).asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) Image(
        bitmap = bitmap,
        contentDescription = "${card.kind.label} карты ${card.title}",
        modifier = modifier,
        contentScale = ContentScale.Fit
    ) else Box(modifier, contentAlignment = Alignment.Center) {
        Text("Невозможно сформировать код. Проверьте формат.", color = Color.Red, textAlign = TextAlign.Center)
    }
}

@Composable
fun CheckoutScreen(card: CardRecord, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.Black)
            }
            Text("KADO", color = Color(0xFF7141CB), fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF33886B))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF211B31), fontSize = 27.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(card.kind.label, color = Color.DarkGray)
            Spacer(Modifier.height(26.dp))
            CodeImage(card, Modifier.fillMaxWidth().height(if (card.kind.isSquare()) 310.dp else 200.dp))
            Spacer(Modifier.height(20.dp))
            Text(card.value, color = Color(0xFF241C38), fontSize = 16.sp, textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Покажите код кассиру", color = Color.DarkGray, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("Яркость экрана временно увеличена", color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
