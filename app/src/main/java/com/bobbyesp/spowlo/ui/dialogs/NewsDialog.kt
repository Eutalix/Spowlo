package com.bobbyesp.spowlo.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.bobbyesp.spowlo.utils.NewsUtil
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun NewsDialog(
    news: NewsUtil.NewsRelease,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            // For critical news, force user to interact with buttons
            if (!news.is_critical) {
                NewsUtil.markNewsAsRead(news.id)
                onDismissRequest()
            }
        },
        title = { Text(news.title) },
        icon = {
            Icon(
                imageVector = if (news.is_critical) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = if (news.is_critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    NewsUtil.markNewsAsRead(news.id)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            // Optional action button (e.g., Telegram link)
            if (!news.action_url.isNullOrBlank() && !news.action_text.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.action_url))
                        context.startActivity(intent)
                    }
                ) {
                    Text(news.action_text)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                MarkdownText(
                    markdown = news.body,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    style = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    )
}