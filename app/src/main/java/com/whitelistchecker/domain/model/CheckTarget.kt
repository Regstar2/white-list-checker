package com.whitelistchecker.domain.model

data class CheckTarget(
    val name: String,
    val url: String,
    val group: TargetGroup,
)
