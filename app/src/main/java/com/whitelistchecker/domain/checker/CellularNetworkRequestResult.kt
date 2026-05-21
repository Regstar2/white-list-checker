package com.whitelistchecker.domain.checker

import android.net.Network

data class CellularNetworkRequestResult(
    val network: Network? = null,
    val permissionDenied: Boolean = false,
)
