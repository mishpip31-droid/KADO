package com.kado.app.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.kado.app.data.BarcodeKind

object BarcodeTools {
    fun kindFromMlKit(format: Int): BarcodeKind? = when (format) {
        Barcode.FORMAT_QR_CODE -> BarcodeKind.QR_CODE
        Barcode.FORMAT_CODE_128 -> BarcodeKind.CODE_128
        Barcode.FORMAT_EAN_13 -> BarcodeKind.EAN_13
        Barcode.FORMAT_EAN_8 -> BarcodeKind.EAN_8
        Barcode.FORMAT_UPC_A -> BarcodeKind.UPC_A
        Barcode.FORMAT_CODE_39 -> BarcodeKind.CODE_39
        Barcode.FORMAT_ITF -> BarcodeKind.ITF
        Barcode.FORMAT_CODABAR -> BarcodeKind.CODABAR
        Barcode.FORMAT_PDF417 -> BarcodeKind.PDF_417
        Barcode.FORMAT_AZTEC -> BarcodeKind.AZTEC
        Barcode.FORMAT_DATA_MATRIX -> BarcodeKind.DATA_MATRIX
        else -> null
    }

    fun errorFor(value: String, kind: BarcodeKind): String? {
        if (value.isBlank()) return "Введите номер карты или содержимое кода"
        if (value.length > 4096) return "Код слишком длинный"
        return try {
            // The same ZXing writer is used for validation and the checkout screen.
            MultiFormatWriter().encode(value, kind.zxing, 256, 128)
            null
        } catch (e: Exception) {
            "Не удалось сформировать ${kind.label}. Проверьте номер и формат."
        }
    }

    fun toBitmap(value: String, kind: BarcodeKind): Bitmap {
        val square = kind in setOf(BarcodeKind.QR_CODE, BarcodeKind.AZTEC, BarcodeKind.DATA_MATRIX)
        val width = if (square) 840 else 1152
        val height = if (square) 840 else 420
        val matrix = MultiFormatWriter().encode(
            value, kind.zxing, width, height, mapOf(EncodeHintType.MARGIN to 2)
        )
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                pixels[y * matrix.width + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        return bitmap
    }
}
