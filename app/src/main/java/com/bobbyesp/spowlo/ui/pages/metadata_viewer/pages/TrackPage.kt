package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamratzman.spotify.models.ReleaseDate
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.components.ActionDialog
import com.bobbyesp.spowlo.ui.components.FORMAT_CHOICES
import com.bobbyesp.spowlo.ui.components.FORMAT_MP3
import com.bobbyesp.spowlo.ui.components.QUALITY_CHOICES
import com.bobbyesp.spowlo.ui.components.QUALITY_ORIGINAL
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.DownloaderUtil
import com.bobbyesp.spowlo.utils.FilesUtil
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.ORIGINAL_AUDIO
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.RedownloadUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.toFileSizeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun TrackPage(
    data: Track,
    modifier: Modifier,
    onBack: () -> Unit,
    trackDownloadCallback: (String, String, DownloaderUtil.DownloadPreferences?) -> Unit,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    downloadedSong: DownloadedSongInfo? = null,
) {
    var trackData by rememberSaveable(stateSaver = TrackSaver) {
        mutableStateOf(data)
    }

    LaunchedEffect(data.id) {
        if (trackData.id != data.id) trackData = data
    }

    val artists = trackData.artists.joinToString(", ") { it.name }
    val artworkUrl = trackData.album.images.firstOrNull()?.url.orEmpty()
    val seed = rememberArtworkSeed(artworkUrl)
    val spotifyUrl = trackData.externalUrls.spotify.orEmpty()
    val releaseDate = formatReleaseDate(trackData.album.releaseDate)
    val isrc = trackData.externalIds.firstOrNull {
        it.key.equals("isrc", ignoreCase = true)
    }?.id.orEmpty()
    val durationMs = trackData.durationMs.toLong()

    val initialFormat = PreferencesUtil.getAudioFormat().takeIf { it in FORMAT_CHOICES } ?: FORMAT_MP3
    val initialQuality = if (PreferencesUtil.getValue(ORIGINAL_AUDIO)) {
        QUALITY_ORIGINAL
    } else {
        PreferencesUtil.getAudioQuality().takeIf { it in QUALITY_CHOICES } ?: QUALITY_ORIGINAL
    }
    var formatIndex by rememberSaveable { mutableStateOf(initialFormat) }
    var qualityIndex by rememberSaveable { mutableStateOf(initialQuality) }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var showRedownloadDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val providers = remember {
        PreferencesUtil.getAudioProvider().ifEmpty { listOf("YouTube Music") }
    }
    var sourceIndex by rememberSaveable { mutableStateOf(0) }
    val source = providers.getOrElse(sourceIndex) { providers.firstOrNull().orEmpty() }

    val taskKey = remember(spotifyUrl) { Downloader.makeKey(spotifyUrl, spotifyUrl.reversed()) }
    val taskState = Downloader.mutableTaskList[taskKey]?.state
    val downloading = taskState is Downloader.DownloadTask.State.Running
    val progress = (taskState as? Downloader.DownloadTask.State.Running)?.progress
    val failed = taskState is Downloader.DownloadTask.State.Error

    val isOnline = rememberIsOnline()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val download = {
        trackDownloadCallback(
            spotifyUrl,
            "$artists - ${trackData.name}",
            DownloaderUtil.DownloadPreferences().copy(
                audioFormat = formatIndex,
                audioQuality = qualityIndex,
                preserveOriginalAudio = false,
                audioProviders = source,
            ),
        )
    }

    MetadataScreen(
        topBarTitle = trackData.name,
        onBack = onBack,
        modifier = modifier,
        seed = seed,
        backdrop = { ArtworkBackdrop(artworkUrl) },
    ) { paddingValues, listState ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item {
                MetadataHero(
                    title = trackData.name,
                    artwork = {
                        if (artworkUrl.isNotEmpty()) CollectionArtwork(artworkUrl, listState)
                    },
                ) {
                    MetadataArtistLine(
                        artists = trackData.artists,
                        onArtistClick = onOpenArtist,
                    )
                    MetadataMetaLine(
                        GeneralTextUtils.formatTrackDuration(trackData.durationMs.toLong()),
                        if (trackData.explicit) stringResource(R.string.explicit) else "",
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (downloadedSong != null) {
                        SavedActions(
                            song = downloadedSong,
                            track = trackData,
                            onPlay = {
                                FilesUtil.openFile(downloadedSong.songPath) {
                                    ToastUtil.makeToastSuspend(
                                        context.getString(R.string.file_unavailable)
                                    )
                                }
                            },
                            onShare = {
                                FilesUtil.createIntentForSharingFile(downloadedSong.songPath)
                                    ?.let { intent ->
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                context.getString(R.string.share_file),
                                            )
                                        )
                                    }
                            },
                            onShowInFolder = {
                                if (!openContainingFolder(downloadedSong.songPath)) {
                                    ToastUtil.makeToastSuspend(
                                        context.getString(R.string.folder_unavailable)
                                    )
                                }
                            },
                            onDelete = { showDeleteDialog = true },
                            onRedownload = { showRedownloadDialog = true },
                            onCopyLink = { GeneralTextUtils.copyToClipboardAndNotify(spotifyUrl) },
                            onOpenSpotify = { ChromeCustomTabsUtil.openUrl(spotifyUrl) },
                        )
                    } else {
                        DownloadActions(
                            downloading = downloading,
                            progress = progress,
                            failed = failed,
                            online = isOnline,
                            urlAvailable = spotifyUrl.isNotBlank(),
                            formatIndex = formatIndex,
                            qualityIndex = qualityIndex,
                            durationMs = durationMs,
                            source = source,
                            onDownload = download,
                            onCancel = { Downloader.mutableTaskList[taskKey]?.onCancel() },
                            onOpenOptions = { showOptions = true },
                            onChangeSource = { showOptions = true },
                            onCopyLink = { GeneralTextUtils.copyToClipboardAndNotify(spotifyUrl) },
                            onOpenSpotify = { ChromeCustomTabsUtil.openUrl(spotifyUrl) },
                        )
                    }
                }
            }

            item {
                MetadataSectionTitle(stringResource(R.string.track_details))
            }

            item {
                MetadataDetailCard {
                    MetadataDetailRow(
                        label = stringResource(R.string.album),
                        value = trackData.album.name,
                        onClick = { onOpenAlbum(trackData.album.id) },
                    )
                    if (isrc.isNotBlank()) {
                        DetailDivider()
                        MetadataDetailRow(
                            label = stringResource(R.string.song_isrc),
                            value = isrc,
                        )
                    }
                    val releaseRow = releaseSummary(trackData, releaseDate)
                    if (releaseRow.isNotBlank()) {
                        DetailDivider()
                        MetadataDetailRow(
                            label = stringResource(R.string.release_date),
                            value = releaseRow,
                        )
                    }
                }
            }
        }
    }

    ArtworkPaletteTheme(seed = seed) {
        if (showOptions) {
            DownloadOptionsSheet(
                formatIndex = formatIndex,
                qualityIndex = qualityIndex,
                durationMs = durationMs,
                providers = providers,
                sourceIndex = sourceIndex,
                onFormatChange = { formatIndex = it },
                onQualityChange = { qualityIndex = it },
                onSourceChange = { sourceIndex = it },
                onDownload = {
                    showOptions = false
                    download()
                },
                onDismiss = { showOptions = false },
            )
        }

        if (showRedownloadDialog) {
            ActionDialog(
                onDismissRequest = { showRedownloadDialog = false },
                icon = Icons.Outlined.Download,
                title = pluralStringResource(R.plurals.redownload_confirm_title, 1, 1),
                supportingText = pluralStringResource(R.plurals.redownload_confirm_message, 1),
                confirmText = stringResource(R.string.redownload),
                onConfirm = {
                    showRedownloadDialog = false
                    scope.launch(Dispatchers.IO) {
                        RedownloadUtil.redownload(spotifyUrl, "$artists - ${trackData.name}")
                    }
                },
            )
        }

        if (showDeleteDialog && downloadedSong != null) {
            val sizeBytes = remember(downloadedSong.songPath) {
                runCatching { File(downloadedSong.songPath).length() }.getOrDefault(0L)
            }
            val folder = folderLabel(downloadedSong.songPath)
            ActionDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = Icons.Outlined.DeleteOutline,
                title = stringResource(R.string.delete_song_title, downloadedSong.songName),
                supportingText = if (sizeBytes > 0L) {
                    stringResource(
                        R.string.delete_song_message,
                        folder,
                        sizeBytes.toFileSizeText()
                    )
                } else {
                    stringResource(R.string.delete_song_message_no_size, folder)
                },
                confirmText = stringResource(R.string.delete),
                onConfirm = {
                    showDeleteDialog = false
                    scope.launch(Dispatchers.IO) {
                        DatabaseUtil.deleteInfoListByIdList(listOf(downloadedSong.id), true)
                    }
                },
            )
        }
    }
}

private fun releaseSummary(track: Track, releaseDate: String): String {
    val total = track.album.totalTracks ?: 0
    val trackNumber = if (track.trackNumber > 0) {
        if (total > 0) "Track ${track.trackNumber} of $total" else "Track ${track.trackNumber}"
    } else {
        ""
    }
    return listOf(releaseDate, trackNumber).filter { it.isNotBlank() }.joinToString("  ·  ")
}

private fun formatReleaseDate(date: ReleaseDate?): String {
    if (date == null || date.year <= 0) return ""
    val month = date.month?.takeIf { it in 1..12 } ?: return date.year.toString()
    val day = date.day?.takeIf { it in 1..31 }
    val localDate = runCatching { LocalDate.of(date.year, month, day ?: 1) }.getOrNull()
        ?: return date.year.toString()
    val formatter = if (day == null) {
        DateTimeFormatter.ofPattern("LLLL yyyy")
    } else {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
    return localDate.format(formatter.withLocale(Locale.getDefault()))
}

@ExperimentalSerializationApi
object TrackSaver : Saver<Track, String> {
    override fun restore(value: String): Track {
        return Json.decodeFromString(value)
    }

    override fun SaverScope.save(value: Track): String {
        return Json.encodeToString(value)
    }
}
