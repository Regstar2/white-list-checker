package com.whitelistchecker.domain.model

enum class ActiveMonitoringState {
    STOPPED,
    STARTING,
    RUNNING,
    CHECKING,
    STOPPING,
    STOPPED_BY_SYSTEM,
    ERROR,
}
