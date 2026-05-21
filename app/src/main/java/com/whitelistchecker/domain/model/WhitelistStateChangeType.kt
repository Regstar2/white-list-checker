package com.whitelistchecker.domain.model

enum class WhitelistStateChangeType {
    WHITELIST_TURNED_ON,
    WHITELIST_TURNED_OFF,
    MANUAL_CHECK,
    TEST_MESSAGE,
    OTHER_CONFIRMED_CHANGE,
    NO_CONFIRMED_CHANGE,
}
