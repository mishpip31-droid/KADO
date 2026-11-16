package com.kado.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kado.app.ui.theme.Muted

@Composable
fun SettingsScreen(cardCount: Int, onExport: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Настройки", fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Ваши карты: $cardCount", color = Muted)
        Spacer(Modifier.height(26.dp))
        Text("Резервное копирование", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        SettingsAction(Icons.Default.FileUpload, "Экспортировать карты", "Сохранить все карты в JSON-файл", onExport)
        SettingsAction(Icons.Default.FileDownload, "Импортировать карты", "Восстановить карты без удаления существующих", onImport)
        Spacer(Modifier.height(17.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(7.dp))
            Text(
                "Карты хранятся только на устройстве. Регистрация и интернет не требуются. " +
                    "Файл резервной копии не зашифрован — храните его в безопасном месте.",
                color = Muted, fontSize = 13.sp, lineHeight = 20.sp
            )
        }
        Spacer(Modifier.height(27.dp))
        Text("О приложении", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("KADO 0.1.0 · Первая версия", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("Без рекламы, облака и отслеживания. Динамические QR-коды и NFC-карты не клонируются.", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp), shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(7.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
    }
}
