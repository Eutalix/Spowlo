package com.bobbyesp.spowlo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val FORMAT_MP3 = 0
internal const val FORMAT_FLAC = 1
internal const val FORMAT_OPUS = 3
internal const val FORMAT_M4A = 4
internal const val QUALITY_ORIGINAL = 17

internal val FORMAT_CHOICES = listOf(FORMAT_MP3, FORMAT_M4A, FORMAT_OPUS, FORMAT_FLAC)
internal val QUALITY_CHOICES = listOf(QUALITY_ORIGINAL, 16, 15, 13, 11)

val DownloadOptionShape = RoundedCornerShape(14.dp)

@Composable
fun DownloadSheetHeader(title: String, subtitle: String? = null) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
    if (!subtitle.isNullOrBlank()) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun DownloadOptionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
}

@Composable
fun DownloadOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.heightIn(min = 38.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
fun DownloadPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Download,
) {
    Button(
        onClick = onClick,
        shape = DownloadOptionShape,
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
