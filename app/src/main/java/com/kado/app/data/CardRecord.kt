package com.kado.app.data

import com.google.zxing.BarcodeFormat
import java.util.UUID

enum class BarcodeKind(val label: String, val zxing: BarcodeFormat) {
    CODE_128("Code 128", BarcodeFormat.CODE_128),
    EAN_13("EAN-13", BarcodeFormat.EAN_13),
    EAN_8("EAN-8", BarcodeFormat.EAN_8),
    QR_CODE("QR-код", BarcodeFormat.QR_CODE),
    UPC_A("UPC-A", BarcodeFormat.UPC_A),
    CODE_39("Code 39", BarcodeFormat.CODE_39),
    ITF("ITF", BarcodeFormat.ITF),
    CODABAR("Codabar", BarcodeFormat.CODABAR),
    PDF_417("PDF417", BarcodeFormat.PDF_417),
    AZTEC("Aztec", BarcodeFormat.AZTEC),
    DATA_MATRIX("Data Matrix", BarcodeFormat.DATA_MATRIX);

    companion object {
        fun parse(text: String): BarcodeKind = entries.firstOrNull { it.name == text } ?: CODE_128
    }
}

data class CardRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val value: String,
    val kind: BarcodeKind = BarcodeKind.CODE_128,
    val colorArgb: Int = CARD_COLORS[0],
    val symbol: String = "✦",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = 0L,
)

val CARD_COLORS: List<Int> = listOf(
    0xFF7B4BD7.toInt(),
    0xFF2360CF.toInt(),
    0xFFDF5570.toInt(),
    0xFF138D70.toInt(),
    0xFFF39835.toInt(),
    0xFF5C62AC.toInt(),
    0xFF343D63.toInt(),
)
val CARD_SYMBOLS: List<String> = listOf("✦", "★", "♥", "✿", "◆", "●", "☘", "✚", "◈", "☕", "🛍")
