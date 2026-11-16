package com.kado.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private class KadoDb(context: Context) : SQLiteOpenHelper(context, "kado_cards.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE cards (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                value TEXT NOT NULL,
                kind TEXT NOT NULL,
                color_argb INTEGER NOT NULL,
                symbol TEXT NOT NULL,
                favorite INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                last_opened_at INTEGER NOT NULL DEFAULT 0
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_cards_title ON cards(title)")
        db.execSQL("CREATE INDEX idx_cards_recent ON cards(last_opened_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations must preserve card records. Never drop the cards table.
    }
}

class CardRepository(context: Context) {
    private val helper = KadoDb(context.applicationContext)
    private val mutex = Mutex()
    private val _cards = MutableStateFlow<List<CardRecord>>(emptyList())
    val cards: StateFlow<List<CardRecord>> = _cards

    suspend fun load(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock { refresh() }
    }

    suspend fun put(card: CardRecord): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            helper.writableDatabase.insertWithOnConflict(
                "cards", null, card.asValues(), SQLiteDatabase.CONFLICT_REPLACE
            )
            refresh()
        }
    }

    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            helper.writableDatabase.delete("cards", "id = ?", arrayOf(id))
            refresh()
        }
    }

    suspend fun toggleFavorite(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val card = readCards().firstOrNull { it.id == id } ?: return@withLock
            val values = ContentValues().apply { put("favorite", if (card.favorite) 0 else 1) }
            helper.writableDatabase.update("cards", values, "id = ?", arrayOf(id))
            refresh()
        }
    }

    suspend fun markOpened(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val values = ContentValues().apply { put("last_opened_at", System.currentTimeMillis()) }
            helper.writableDatabase.update("cards", values, "id = ?", arrayOf(id))
            refresh()
        }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val list = JSONArray()
            readCards().forEach { c ->
                list.put(JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("value", c.value)
                    put("kind", c.kind.name)
                    put("colorArgb", c.colorArgb)
                    put("symbol", c.symbol)
                    put("favorite", c.favorite)
                    put("createdAt", c.createdAt)
                    put("lastOpenedAt", c.lastOpenedAt)
                })
            }
            JSONObject().put("format", "KADO").put("version", 1).put("cards", list).toString(2)
        }
    }

    /** Merge-only import: never deletes or overwrites existing user cards. */
    suspend fun importJson(text: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(text)
        require(root.optString("format") == "KADO") { "Это не резервная копия KADO" }
        require(root.optInt("version") == 1) { "Неподдерживаемая версия копии" }
        val items = root.getJSONArray("cards")
        require(items.length() <= 20_000) { "Слишком большая резервная копия" }
        val parsed = (0 until items.length()).map { index ->
            val obj = items.getJSONObject(index)
            val title = obj.getString("title").trim().take(120)
            val value = obj.getString("value").take(4096)
            require(title.isNotBlank() && value.isNotBlank()) { "Некорректные данные карты" }
            CardRecord(
                id = obj.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                title = title,
                value = value,
                kind = BarcodeKind.parse(obj.optString("kind")),
                colorArgb = obj.optInt("colorArgb", CARD_COLORS.first()),
                symbol = obj.optString("symbol", "✦").take(8).ifBlank { "✦" },
                favorite = obj.optBoolean("favorite"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                lastOpenedAt = obj.optLong("lastOpenedAt", 0L),
            )
        }
        mutex.withLock {
            val existing = readCards().toMutableList()
            var added = 0
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                parsed.forEach { card ->
                    if (existing.none { it.id == card.id || (
                            it.title.equals(card.title, ignoreCase = true) &&
                                it.value == card.value && it.kind == card.kind
                            )
                        }) {
                        db.insertOrThrow("cards", null, card.asValues())
                        existing.add(card)
                        added++
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            refresh()
            added
        }
    }

    private fun refresh() {
        _cards.value = readCards()
    }

    private fun readCards(): List<CardRecord> = buildList {
        helper.readableDatabase.query(
            "cards", null, null, null, null, null,
            "favorite DESC, last_opened_at DESC, created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) add(cursor.toCard())
        }
    }
}

private fun CardRecord.asValues() = ContentValues().apply {
    put("id", id)
    put("title", title)
    put("value", value)
    put("kind", kind.name)
    put("color_argb", colorArgb)
    put("symbol", symbol)
    put("favorite", if (favorite) 1 else 0)
    put("created_at", createdAt)
    put("last_opened_at", lastOpenedAt)
}

private fun Cursor.toCard() = CardRecord(
    id = getString(getColumnIndexOrThrow("id")),
    title = getString(getColumnIndexOrThrow("title")),
    value = getString(getColumnIndexOrThrow("value")),
    kind = BarcodeKind.parse(getString(getColumnIndexOrThrow("kind"))),
    colorArgb = getInt(getColumnIndexOrThrow("color_argb")),
    symbol = getString(getColumnIndexOrThrow("symbol")),
    favorite = getInt(getColumnIndexOrThrow("favorite")) == 1,
    createdAt = getLong(getColumnIndexOrThrow("created_at")),
    lastOpenedAt = getLong(getColumnIndexOrThrow("last_opened_at")),
)
