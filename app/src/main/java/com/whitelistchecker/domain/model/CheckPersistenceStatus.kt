package com.whitelistchecker.domain.model

data class CheckPersistenceStatus(
    val historySaved: Boolean,
    val technicalStatisticsUpdated: Boolean,
    val whitelistTimelineUpdated: Boolean,
    val errorMessage: String? = null,
) {
    val isComplete: Boolean
        get() = historySaved && technicalStatisticsUpdated && whitelistTimelineUpdated
}
