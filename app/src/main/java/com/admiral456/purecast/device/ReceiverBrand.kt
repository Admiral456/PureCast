package com.admiral456.purecast.device

enum class ReceiverBrand(val displayName: String, val platformName: String) {
    SAMSUNG("Samsung", "DLNA / Tizen browser where available; model-specific"),
    LG("LG", "DLNA / webOS browser where available; model-specific"),
    SONY("Sony", "DLNA / TV browser where available; model-specific"),
    TCL("TCL", "DLNA / TV browser where available; model-specific"),
    PHILIPS("Philips", "DLNA / TV browser where available; model-specific"),
    ANDROID_TV("Android TV", "DLNA / Android TV browser where available"),
    OTHER("Jiná televize", "DLNA / UPnP / TV browser where available")
}

object ReceiverBrandClassifier {
    fun classify(manufacturer: String?, model: String?, friendlyName: String?): ReceiverBrand {
        val identity = listOfNotNull(manufacturer, model, friendlyName)
            .joinToString(" ")
            .lowercase()

        return when {
            identity.contains("samsung") -> ReceiverBrand.SAMSUNG
            identity.contains("lg electronics") || identity.contains("webos") ||
                identity.matches(Regex(".*\\blg\\b.*")) -> ReceiverBrand.LG
            identity.contains("sony") || identity.contains("bravia") -> ReceiverBrand.SONY
            identity.contains("tcl") -> ReceiverBrand.TCL
            identity.contains("philips") -> ReceiverBrand.PHILIPS
            identity.contains("android tv") || identity.contains("google tv") -> ReceiverBrand.ANDROID_TV
            else -> ReceiverBrand.OTHER
        }
    }
}
