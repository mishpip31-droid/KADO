package com.kado.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kado.app.barcode.BarcodeTools
import com.kado.app.data.BarcodeKind
import com.kado.app.data.CARD_COLORS
import com.kado.app.data.CARD_SYMBOLS
import com.kado.app.data.CardRecord
import com.kado.app.ui.theme.Muted
import com.kado.app.ui.widgets.CardFace

@Composable
fun AddCardScreen(onBack: () -> Unit, onScan: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            Text("Добавить карту", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(23.dp))
        Text("Выберите способ", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        ActionOption(Icons.Default.CameraAlt, "Сканировать карту", "Наведите камеру на штрихкод или QR-код", onScan)
        ActionOption(Icons.Default.Image, "Из галереи", "Распознать код с фото или скриншота", onGallery)
        ActionOption(Icons.Default.Keyboard, "Ввести вручную", "Номер карты или код любого магазина", onManual)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.QrCode, contentDescription = null,
                    modifier = Modifier.size(70.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                Text("Новая карта займёт место в веере", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Коды сохраняются только на этом устройстве. Интернет не нужен.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ActionOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Text("›", color = Muted, fontSize = 24.sp)
        }
    }
}

@Composable
fun CardEditorScreen(
    existing: CardRecord?,
    initialValue: String,
    initialKind: BarcodeKind?,
    onSave: (CardRecord) -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onGallery: () -> Unit,
) {
    var title by rememberSaveable(existing?.id, initialValue) { mutableStateOf(existing?.title ?: "") }
    var value by rememberSaveable(existing?.id, initialValue) { mutableStateOf(initialValue.ifBlank { existing?.value ?: "" }) }
    var kindName by rememberSaveable(existing?.id, initialValue) {
        mutableStateOf((initialKind ?: existing?.kind ?: BarcodeKind.CODE_128).name)
    }
    var color by rememberSaveable(existing?.id) { mutableStateOf(existing?.colorArgb ?: CARD_COLORS[0]) }
    var symbol by rememberSaveable(existing?.id) { mutableStateOf(existing?.symbol ?: "✦") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val kind = BarcodeKind.parse(kindName)
    val preview = CardRecord(title = title.ifBlank { "Название магазина" },
        value = value, kind = kind, colorArgb = color, symbol = symbol)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            Text(if (existing == null) "Новая карта" else "Редактировать карту", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(15.dp))
        CardFace(preview, Modifier.fillMaxWidth().height(157.dp))
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(120); error = null },
            label = { Text("Название магазина") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = value, onValueChange = { value = it.take(4096); error = null },
            label = { Text("Штрихкод / номер карты") }, minLines = 1, maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Формат кода", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text("${kind.label}   ▾") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                BarcodeKind.entries.forEach { choice ->
                    DropdownMenuItem(text = { Text(choice.label) }, onClick = {
                        kindName = choice.name
                        expanded = false
                        error = null
                    })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onScan) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Камера")
            }
            OutlinedButton(onClick = onGallery) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Галерея")
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Цвет карты", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CARD_COLORS.forEach { argb ->
                Box(
                    Modifier.size(39.dp)
                        .background(Color(argb), CircleShape)
                        .border(if (color == argb) 3.dp else 1.dp,
                            if (color == argb) MaterialTheme.colorScheme.onSurface else Color.White, CircleShape)
                        .clickable { color = argb },
                    contentAlignment = Alignment.Center
                ) { if (color == argb) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Значок", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CARD_SYMBOLS.forEach { sign ->
                Box(
                    Modifier.size(42.dp)
                        .background(if (symbol == sign) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp))
                        .border(1.dp, if (symbol == sign) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(14.dp))
                        .clickable { symbol = sign },
                    contentAlignment = Alignment.Center
                ) { Text(sign, fontSize = 21.sp) }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            error = when {
                title.isBlank() -> "Укажите название магазина"
                else -> BarcodeTools.errorFor(value, kind)
            }
            if (error == null) {
                onSave(CardRecord(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    title = title.trim(), value = value,
                    kind = kind, colorArgb = color, symbol = symbol,
                    favorite = existing?.favorite ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    lastOpenedAt = existing?.lastOpenedAt ?: 0L
                ))
            }
        }, modifier = Modifier.fillMaxWidth().height(53.dp), shape = RoundedCornerShape(19.dp)) {
            Text(if (existing == null) "Сохранить карту" else "Сохранить изменения", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
