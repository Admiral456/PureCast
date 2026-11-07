package com.admiral456.purecast.device

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceBrandClassifierTest {
    @Test
    fun recognizesSamsungPhones() {
        assertEquals(SenderBrand.SAMSUNG, SenderBrandClassifier.classify("samsung", "samsung", "SM-S928B"))
    }

    @Test
    fun recognizesXiaomiPhones() {
        assertEquals(SenderBrand.XIAOMI, SenderBrandClassifier.classify("Xiaomi", "Xiaomi", "Xiaomi 17T Pro"))
    }

    @Test
    fun recognizesRedmiPhones() {
        assertEquals(SenderBrand.REDMI, SenderBrandClassifier.classify("Xiaomi", "Redmi", "Redmi Note"))
    }

    @Test
    fun recognizesPocoAsXiaomiFamily() {
        assertEquals(SenderBrand.XIAOMI, SenderBrandClassifier.classify("Xiaomi", "POCO", "POCO F"))
    }

    @Test
    fun fallsBackToGenericAndroid() {
        assertEquals(SenderBrand.OTHER_ANDROID, SenderBrandClassifier.classify("Google", "Pixel", "Pixel 9"))
    }

    @Test
    fun recognizesSamsungTv() {
        assertEquals(ReceiverBrand.SAMSUNG, ReceiverBrandClassifier.classify("Samsung", "UE55", "Samsung Smart TV"))
    }

    @Test
    fun recognizesLgTv() {
        assertEquals(ReceiverBrand.LG, ReceiverBrandClassifier.classify("LG Electronics", "55UJ7507", "webOS TV"))
    }

    @Test
    fun recognizesSonyBravia() {
        assertEquals(ReceiverBrand.SONY, ReceiverBrandClassifier.classify("Sony", "BRAVIA", "BRAVIA"))
    }
}
