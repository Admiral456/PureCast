package com.admiral456.purecast.dlna

data class DlnaDevice(
    val id: String,
    val friendlyName: String,
    val manufacturer: String?,
    val modelName: String?,
    val location: String,
    val avTransportControlUrl: String,
    val serviceType: String,
    val connectionManagerControlUrl: String? = null,
    val connectionManagerServiceType: String? = null
)
