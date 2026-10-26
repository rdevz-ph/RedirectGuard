package com.rdevzph.redirectguard.model

data class RedirectRecord(
    val id: String,
    val sourcePackage: String,
    val targetPackage: String,
    val sourceAppName: String,
    val targetAppName: String,
    val timestamp: Long,
    val actionTaken: RedirectAction,
    val triggerReason: String,
    val deepLinkUri: String? = null
)
