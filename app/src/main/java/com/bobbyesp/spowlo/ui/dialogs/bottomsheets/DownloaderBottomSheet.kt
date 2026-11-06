package com.bobbyesp.spowlo.ui.dialogs.bottomsheets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.BottomSheet
import com.bobbyesp.spowlo.ui.components.DownloadOptionChip
import com.bobbyesp.spowlo.ui.components.DownloadOptionLabel
import com.bobbyesp.spowlo.ui.components.DownloadPrimaryButton
import com.bobbyesp.spowlo.ui.components.DownloadSheetHeader
import com.bobbyesp.spowlo.ui.components.FORMAT_CHOICES
import com.bobbyesp.spowlo.ui.components.FORMAT_FLAC
import com.bobbyesp.spowlo.ui.components.FORMAT_MP3
import com.bobbyesp.spowlo.ui.components.QUALITY_CHOICES
import com.bobbyesp.spowlo.ui.components.QUALITY_ORIGINAL
import com.bobbyesp.spowlo.utils.AUDIO_FORMAT
import com.bobbyesp.spowlo.utils.AUDIO_QUALITY
import com.bobbyesp.spowlo.utils.ORIGINAL_AUDIO
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.UrlValidator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderBottomSheet(
    onBackPressed: () -> Unit,
    url: String,
    bottomSheetState: SheetState,
    onDownloadPressed: () -> Unit,
    navigateToTrack: (String) -> Unit,
    navigateToPlaylist: (String) -> Unit,
    navigateToAlbum: (String) -> Unit,
    navigateToArtist: (String) -> Unit,
) {
    val settings = PreferencesUtil
    var formatIndex by remember {
        mutableStateOf(settings.getAudioFormat().takeIf { it in FORMAT_CHOICES } ?: FORMAT_MP3)
    }
    var qualityIndex by remember {
        mutableStateOf(
            if (settings.getValue(ORIGINAL_AUDIO)) {
                QUALITY_ORIGINAL
            } else {
                settings.getAudioQuality().takeIf { it in QUALITY_CHOICES } ?: QUALITY_ORIGINAL
            }
        )
    }

    val onQualityChange: (Int) -> Unit = { quality ->
        qualityIndex = quality
        settings.encodeInt(AUDIO_QUALITY, quality)
        settings.updateValue(ORIGINAL_AUDIO, quality == QUALITY_ORIGINAL)
    }
    val onFormatChange: (Int) -> Unit = { format ->
        formatIndex = format
        settings.encodeInt(AUDIO_FORMAT, format)
        if (format == FORMAT_FLAC) onQualityChange(QUALITY_ORIGINAL)
    }

    val link = remember(url) { UrlValidator.parseSpotifyLink(url) }
    val linkType = link?.type
    val linkId = link?.id.orEmpty()

    BottomSheet(state = bottomSheetState, onDismiss = onBackPressed) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            DownloadSheetHeader(
                title = stringResource(R.string.settings_before_download),
                subtitle = stringResource(R.string.settings_before_download_text),
            )

            DownloadOptionLabel(stringResource(R.string.format))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FORMAT_CHOICES.forEach { format ->
                    DownloadOptionChip(
                        label = PreferencesUtil.getAudioFormatDesc(format).uppercase(Locale.ROOT),
                        selected = formatIndex == format,
                        onClick = { onFormatChange(format) },
                    )
                }
            }

            if (formatIndex != FORMAT_FLAC) {
                DownloadOptionLabel(stringResource(R.string.bitrate))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QUALITY_CHOICES.forEach { quality ->
                        DownloadOptionChip(
                            label = if (quality == QUALITY_ORIGINAL) {
                                stringResource(R.string.original)
                            } else {
                                PreferencesUtil.getAudioQualityDesc(quality)
                                    .removeSuffix(" kbps")
                            },
                            selected = qualityIndex == quality,
                            onClick = { onQualityChange(quality) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            DownloadPrimaryButton(
                text = stringResource(R.string.start_download),
                onClick = {
                    onBackPressed()
                    onDownloadPressed()
                },
            )

            when (linkType) {
                "track" -> SheetSecondaryAction(stringResource(R.string.see_track)) {
                    navigateToTrack(linkId)
                    onBackPressed()
                }

                "playlist" -> SheetSecondaryAction(stringResource(R.string.see_playlist)) {
                    navigateToPlaylist(linkId)
                    onBackPressed()
                }

                "album" -> SheetSecondaryAction(stringResource(R.string.see_album)) {
                    navigateToAlbum(linkId)
                    onBackPressed()
                }

                "artist" -> SheetSecondaryAction(stringResource(R.string.see_artist)) {
                    navigateToArtist(linkId)
                    onBackPressed()
                }
            }
        }
    }
}

@Composable
private fun SheetSecondaryAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
