package com.kado.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kado.app.data.CardRecord
import com.kado.app.ui.theme.Muted
import com.kado.app.ui.widgets.CardFace
import com.kado.app.ui.widgets.FanDeck

@Composable
fun WalletScreen(
    cards: List<CardRecord>,
    allMode: Boolean,
    focusId: String?,
    onOpen: (CardRecord) -> Unit,
    onAdd: () -> Unit,
    onFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val filtered = cards.filter {
        (!onlyFavorites || it.favorite) && (query.isBlank() ||
            it.title.contains(query.trim(), ignoreCase = true) ||
            it.value.contains(query.trim(), ignoreCase = true))
    }
    LaunchedEffect(focusId, cards.map { it.id }) {
        if (focusId != null && cards.any { it.id == focusId }) selectedId = focusId
        else if (selectedId == null || cards.none { it.id == selectedId }) selectedId = cards.firstOrNull()?.id
    }
    val activeId = selectedId?.takeIf { id -> filtered.any { it.id == id } } ?: filtered.firstOrNull()?.id

    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Text("◕", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp) }
            Spacer(Modifier.width(10.dp))
            Text("KADO", fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("✦", color = MaterialTheme.colorScheme.primary, fontSize = 23.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(if (allMode) "Все карты" else "Мои карты", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Поиск по картам") },
            shape = RoundedCornerShape(22.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = !onlyFavorites, onClick = { onlyFavorites = false }, label = { Text("Все") })
            FilterChip(selected = onlyFavorites, onClick = { onlyFavorites = true }, label = { Text("Избранное") })
            Spacer(Modifier.weight(1f))
            Text("${filtered.size} карт", modifier = Modifier.align(Alignment.CenterVertically), color = Muted, fontSize = 12.sp)
        }
        if (allMode) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { card ->
                    CardListRow(card = card, onOpen = { onOpen(card) }, onFavorite = { onFavorite(card.id) })
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (filtered.isEmpty()) {
                    EmptyWallet(
                        hasCards = cards.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(314.dp),
                        onAdd = onAdd
                    )
                } else {
                    FanDeck(
                        cards = filtered,
                        selectedId = activeId,
                        incomingId = focusId,
                        onSelect = { selectedId = it },
                        onOpen = onOpen,
                        onSwipe = { delta ->
                            val index = filtered.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
                            selectedId = filtered[(index + delta).coerceIn(0, filtered.lastIndex)].id
                        }
                    )
                }
                Spacer(Modifier.height(13.dp))
                Text("Избранное", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(10.dp))
                val favorites = cards.filter { it.favorite }
                if (favorites.isEmpty()) Text("Отмечайте часто используемые карты звёздочкой", color = Muted, fontSize = 13.sp)
                else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    favorites.forEach { card ->
                        Column(Modifier.width(78.dp).clickable { onOpen(card) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            CardFace(card, Modifier.size(74.dp, 70.dp), compact = true)
                            Spacer(Modifier.height(4.dp))
                            Text(card.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("Недавние", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(10.dp))
                val recent = cards.filter { it.lastOpenedAt > 0 }.sortedByDescending { it.lastOpenedAt }.take(3)
                if (recent.isEmpty()) Text("Недавно открытые карты появятся здесь", color = Muted, fontSize = 13.sp)
                recent.forEach { CardListRow(card = it, onOpen = { onOpen(it) }, onFavorite = { onFavorite(it.id) }) }
                Spacer(Modifier.height(12.dp))
            }
        }
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(52.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить карту", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyWallet(hasCards: Boolean, modifier: Modifier, onAdd: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(74.dp))
        Spacer(Modifier.height(15.dp))
        Text(if (hasCards) "Карты не найдены" else "Ваш веер пока пуст", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(5.dp))
        Text(if (hasCards) "Попробуйте другой запрос" else "Добавьте первую скидочную карту", color = Muted)
        if (!hasCards) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAdd) { Text("Добавить первую карту") }
        }
    }
}

@Composable
fun CardListRow(card: CardRecord, onOpen: () -> Unit, onFavorite: () -> Unit) {
    ElevatedCard(
        onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardFace(card, Modifier.size(56.dp, 50.dp), compact = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.kind.label, fontSize = 12.sp, color = Muted)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (card.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (card.favorite) "Убрать из избранного" else "В избранное",
                    tint = if (card.favorite) Color(0xFFFFB02E) else Muted
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}
