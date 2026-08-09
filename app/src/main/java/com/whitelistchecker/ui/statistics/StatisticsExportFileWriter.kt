package com.whitelistchecker.ui.statistics

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.whitelistchecker.domain.statistics.StatisticsExportDocument
import java.nio.charset.StandardCharsets

object StatisticsExportFileWriter {
    fun write(
        contentResolver: ContentResolver,
        uri: Uri,
        document: StatisticsExportDocument,
    ): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(document.content.toByteArray(StandardCharsets.UTF_8))
            } ?: return false
            true
        } catch (exception: Exception) {
            Log.w(TAG, "Statistics export write failed", exception)
            false
        }
    }

    private const val TAG = "StatisticsExportWriter"
}
