package com.kado.app

import com.kado.app.data.BarcodeKind
import org.junit.Assert.assertEquals
import org.junit.Test

class BarcodeKindTest {
    @Test fun knownBarcodeTypePreserved() {
        assertEquals(BarcodeKind.EAN_13, BarcodeKind.parse("EAN_13"))
        assertEquals(BarcodeKind.QR_CODE, BarcodeKind.parse("QR_CODE"))
    }

    @Test fun unknownBackupTypeUsesDefault() {
        assertEquals(BarcodeKind.CODE_128, BarcodeKind.parse("UNKNOWN_OLD_FORMAT"))
    }
}
