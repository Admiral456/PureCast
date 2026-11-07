package com.admiral456.purecast.device

import android.os.Build

enum class SenderBrand(val displayName: String) {
    SAMSUNG("Samsung"),
    XIAOMI("Xiaomi"),
    REDMI("Redmi"),
    OTHER_ANDROID("Android")
}

object SenderBrandClassifier {
    fun classify(manufacturer: String?, brand: String?, model: String? = null): SenderBrand {
        val identity = listOfNotNull(manufacturer, brand, model)
            .joinToString(" ")
            .lowercase()

        return when {
            identity.contains("redmi") -> SenderBrand.REDMI
            identity.contains("samsung") -> SenderBrand.SAMSUNG
            identity.contains("xiaomi") || identity.contains("poco") -> SenderBrand.XIAOMI
            else -> SenderBrand.OTHER_ANDROID
        }
    }
}

data class SenderDeviceInfo(
    val brand: SenderBrand,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int
) {
    companion object {
        fun current(): SenderDeviceInfo = SenderDeviceInfo(
            brand = SenderBrandClassifier.classify(Build.MANUFACTURER, Build.BRAND, Build.MODEL),
            model = Build.MODEL.orEmpty().ifBlank { "Neznámý model" },
            androidVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { "Neznámá verze" },
            apiLevel = Build.VERSION.SDK_INT
        )
    }
}
