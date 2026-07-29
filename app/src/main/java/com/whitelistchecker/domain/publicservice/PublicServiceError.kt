package com.whitelistchecker.domain.publicservice

class PublicServiceException(
    val code: String,
    message: String,
) : Exception(message)
