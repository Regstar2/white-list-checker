package com.whitelistchecker.domain.model

data class PublicServiceStatus(
    val lastUploadAtMillis: Long? = null,
    val lastUploadError: String? = null,
    val pendingReportCount: Int = 0,
    val lastLinkCode: String? = null,
    val lastLinkCodeExpiresAtMillis: Long? = null,
    val lastLinkError: String? = null,
    val linkedChatsCount: Int = 0,
    val lastServiceSyncAtMillis: Long? = null,
    val lastServiceSyncError: String? = null,
    val lastRemoteCommandAtMillis: Long? = null,
    val lastRemoteCommandResult: String? = null,
)
