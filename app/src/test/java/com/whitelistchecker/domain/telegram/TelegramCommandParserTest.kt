package com.whitelistchecker.domain.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramCommandParserTest {

    @Test
    fun parsesStatusCommand() {
        assertEquals(TelegramBotCommand.STATUS, TelegramCommandParser.parse("/status"))
    }

    @Test
    fun parsesCheckCommandWithBotSuffix() {
        assertEquals(TelegramBotCommand.CHECK, TelegramCommandParser.parse("/check@my_bot"))
    }

    @Test
    fun parsesHelpCommandWithArguments() {
        assertEquals(TelegramBotCommand.HELP, TelegramCommandParser.parse("/help please"))
    }

    @Test
    fun unknownCommandReturnsUnknown() {
        assertEquals(TelegramBotCommand.UNKNOWN, TelegramCommandParser.parse("/start"))
    }

    @Test
    fun authorizesExactAllowedChatId() {
        assertTrue(
            TelegramCommandAuthorizer.isAuthorized(
                allowedChatIds = setOf("123", "-100987"),
                chatId = "-100987",
            ),
        )
    }

    @Test
    fun rejectsForeignChatId() {
        assertFalse(
            TelegramCommandAuthorizer.isAuthorized(
                allowedChatIds = setOf("123"),
                chatId = "456",
            ),
        )
    }

    @Test
    fun rejectsBlankChatId() {
        assertFalse(
            TelegramCommandAuthorizer.isAuthorized(
                allowedChatIds = setOf("123"),
                chatId = "",
            ),
        )
    }
}
