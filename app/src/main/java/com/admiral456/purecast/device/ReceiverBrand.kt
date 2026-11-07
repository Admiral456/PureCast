package com.admiral456.purecast.device

enum class ReceiverBrand(val displayName: String, val platformName: String) {
    SAMSUNG("Samsung", "Tizen / DLNA / TV browser"),
    LG("LG", "webOS / DLNA / TV browser"),
    SONY("Sony", "Android TV / Google TV / DLNA"),
    TCL("TCL", "Google TV / Android TV / DLNA"),
    PHILIPS("Philips", "Google TV / Android TV / DLNA"),
    ANDROID_TV("Android TV", "Android TV / Google TV / DLNA"),
    OTHER("Jiná televize", "DLNA / UPnP / TV browser")
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
