package com.whitelistchecker.data.telegram

import com.whitelistchecker.domain.model.TelegramRecipient
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import java.util.concurrent.TimeUnit

class PendingTelegramReportRepository(
    private val dao: PendingTelegramReportDao,
) {

    suspend fun getAll(): List<PendingTelegramReportEntity> = dao.getAll()

    suspend fun count(): Int = dao.count()

    suspend fun save(
        text: String,
        event: WhitelistStateChangeEvent,
        error: String?,
        attemptedSend: Boolean = false,
        recipient: TelegramRecipient? = null,
    ) {
        saveMessage(
            text = text,
            id = buildReportId(event, recipient),
            eventType = event.type.name,
            oldState = event.oldState.name,
            newState = event.newState.name,
            error = error,
            attemptedSend = attemptedSend,
            recipient = recipient,
        )
    }

    suspend fun saveMessage(
        text: String,
        id: String,
        eventType: String,
        error: String?,
        attemptedSend: Boolean = false,
        oldState: String = WhitelistState.UNKNOWN.name,
        newState: String = WhitelistState.UNKNOWN.name,
        recipient: TelegramRecipient? = null,
    ) {
        val nowMillis = System.currentTimeMillis()
        dao.insert(
            PendingTelegramReportEntity(
                id = id,
                text = text,
                eventType = eventType,
                oldState = oldState,
                newState = newState,
                recipientId = recipient?.id.orEmpty(),
                chatId = recipient?.chatId.orEmpty(),
                recipientName = recipient?.displayName.orEmpty(),
                createdAtMillis = nowMillis,
                attemptCount = if (attemptedSend) 1 else 0,
                lastAttemptAtMillis = if (attemptedSend) nowMillis else null,
                lastError = error,
            ),
        )
        deleteOldReports(nowMillis)
        enforceQueueLimit()
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun markAttempt(
        entity: PendingTelegramReportEntity,
        error: String?,
        nowMillis: Long,
    ) {
        dao.updateAttempt(
            id = entity.id,
            attemptCount = entity.attemptCount + 1,
            lastAttemptAtMillis = nowMillis,
            lastError = error,
        )
    }

    suspend fun deleteOldReports(nowMillis: Long) {
        val thresholdMillis = nowMillis - TimeUnit.DAYS.toMillis(MAX_REPORT_AGE_DAYS.toLong())
        dao.deleteOlderThan(thresholdMillis)
    }

    suspend fun enforceQueueLimit() {
        val reports = dao.getAll()
        if (reports.size <= MAX_PENDING_REPORTS) return
        val excessCount = reports.size - MAX_PENDING_REPORTS
        reports.take(excessCount).forEach { report ->
            dao.deleteById(report.id)
        }
    }

    suspend fun clear() {
        dao.clear()
    }

    companion object {
        const val MAX_PENDING_REPORTS = 20
        const val MAX_REPORT_AGE_DAYS = 7
        const val MAX_ATTEMPT_COUNT = 10

        fun buildReportId(event: WhitelistStateChangeEvent): String {
            return "${event.type.name}_${event.changedAtMillis}"
        }

        fun buildReportId(event: WhitelistStateChangeEvent, recipient: TelegramRecipient?): String {
            val base = buildReportId(event)
            return if (recipient == null) base else "${base}_${recipient.id}"
        }

        fun buildReportId(baseId: String, recipient: TelegramRecipient): String {
            return "${baseId}_${recipient.id}"
        }
    }
}
