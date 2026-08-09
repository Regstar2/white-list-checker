package com.whitelistchecker.ui.publicservice

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.whitelistchecker.R

@Composable
internal fun LinkCodeCopyButton(code: String) {
    LinkCodeCopyButton(code = code, compact = false)
}

@Composable
internal fun LinkCodeCopyButton(
    code: String,
    compact: Boolean,
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }

    val onCopy = {
        clipboardManager.setText(AnnotatedString(code))
        copied = true
    }
    val text = if (copied) {
        stringResource(R.string.public_service_link_code_copied)
    } else {
        stringResource(R.string.public_service_copy_link_code)
    }

    if (compact) {
        TextButton(onClick = onCopy) {
            Text(text)
        }
        return
    }

    OutlinedButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(code))
            copied = true
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text)
    }
}
