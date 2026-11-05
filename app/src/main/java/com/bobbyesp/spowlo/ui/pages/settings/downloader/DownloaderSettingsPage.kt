package com.bobbyesp.spowlo.ui.pages.settings.downloader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ShuffleOn
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.intState
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.settings.SettingsChevron
import com.bobbyesp.spowlo.ui.components.settings.SettingsDivider
import com.bobbyesp.spowlo.ui.components.settings.SettingsGroup
import com.bobbyesp.spowlo.ui.components.settings.SettingsItemNew
import com.bobbyesp.spowlo.ui.components.settings.SettingsSectionHeader
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitch
import com.bobbyesp.spowlo.ui.icons.YouTubeMusic
import com.bobbyesp.spowlo.utils.CELLULAR_DOWNLOAD
import com.bobbyesp.spowlo.utils.DONT_FILTER_RESULTS
import com.bobbyesp.spowlo.utils.DOWNLOAD_LYRICS
import com.bobbyesp.spowlo.utils.GENERATE_LRC
import com.bobbyesp.spowlo.utils.INCOGNITO_MODE
import com.bobbyesp.spowlo.utils.ONLY_VERIFIED_RESULTS
import com.bobbyesp.spowlo.utils.ORIGINAL_AUDIO
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.SKIP_ALBUM_ART
import com.bobbyesp.spowlo.utils.SKIP_EXPLICIT
import com.bobbyesp.spowlo.utils.SPLIT_BY_MAIN_ARTIST
import com.bobbyesp.spowlo.utils.SPLIT_BY_PLAYLIST
import com.bobbyesp.spowlo.utils.SPONSORBLOCK
import com.bobbyesp.spowlo.utils.THREADS
import com.bobbyesp.spowlo.utils.USE_YT_METADATA
import com.bobbyesp.spowlo.utils.PreferencesUtil.updateInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderSettingsPage(
    onBackPressed: () -> Unit,
) {
    val threadsState = THREADS.intState

    var audioFormat by remember { mutableStateOf(PreferencesUtil.getAudioFormatDesc()) }
    var audioQuality by remember { mutableStateOf(PreferencesUtil.getAudioQualityDesc()) }
    var preserveOriginalAudio by remember { mutableStateOf(PreferencesUtil.getValue(ORIGINAL_AUDIO)) }
    var useSponsorBlock by remember { mutableStateOf(PreferencesUtil.getValue(SPONSORBLOCK)) }

    var splitByPlaylist by remember { mutableStateOf(PreferencesUtil.getValue(SPLIT_BY_PLAYLIST)) }
    var splitByMainArtist by remember { mutableStateOf(PreferencesUtil.getValue(SPLIT_BY_MAIN_ARTIST)) }
    var playlistTagging by remember { mutableStateOf(PreferencesUtil.getPlaylistTagging()) }

    var downloadLyrics by remember { mutableStateOf(PreferencesUtil.getValue(DOWNLOAD_LYRICS)) }
    var generateLRC by remember { mutableStateOf(PreferencesUtil.getValue(GENERATE_LRC)) }
    var skipAlbumArt by remember { mutableStateOf(PreferencesUtil.getValue(SKIP_ALBUM_ART)) }
    var useYTMetadata by remember { mutableStateOf(PreferencesUtil.getValue(USE_YT_METADATA)) }

    var onlyVerifiedResults by remember { mutableStateOf(PreferencesUtil.getValue(ONLY_VERIFIED_RESULTS)) }
    var skipExplicit by remember { mutableStateOf(PreferencesUtil.getValue(SKIP_EXPLICIT)) }
    var dontFilter by remember { mutableStateOf(PreferencesUtil.getValue(DONT_FILTER_RESULTS)) }

    var cellularDownload by remember { mutableStateOf(PreferencesUtil.getValue(CELLULAR_DOWNLOAD)) }
    var proxy by remember { mutableStateOf(PreferencesUtil.getProxy()) }

    var incognitoMode by remember { mutableStateOf(PreferencesUtil.getValue(INCOGNITO_MODE)) }

    var showAudioFormatDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showAudioProviderDialog by remember { mutableStateOf(false) }
    var showAudioLyricDialog by remember { mutableStateOf(false) }
    var showOutputFormatDialog by remember { mutableStateOf(false) }
    var showPlaylistTaggingDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.downloader),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { BackButton { onBackPressed() } },
                scrollBehavior = scrollBehavior
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                )
            ) {
                item {
                    SettingsSectionHeader(stringResource(id = R.string.audio))
                }
                item {
                    SettingsGroup {
                        SettingsItemNew(
                            title = { Text(stringResource(id = R.string.audio_provider), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.audio_provider_desc)) },
                            icon = Icons.Outlined.ShuffleOn,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            onClick = { showAudioProviderDialog = true },
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = { Text(stringResource(id = R.string.audio_format), fontWeight = FontWeight.Bold) },
                            description = { Text(audioFormat) },
                            icon = Icons.Outlined.AudioFile,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            onClick = { showAudioFormatDialog = true },
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = { Text(stringResource(id = R.string.audio_quality), fontWeight = FontWeight.Bold) },
                            description = {
                                Text(
                                    if (preserveOriginalAudio) {
                                        stringResource(id = R.string.audio_quality_unavailable)
                                    } else {
                                        audioQuality
                                    }
                                )
                            },
                            icon = Icons.Outlined.HighQuality,
                            trailing = { SettingsChevron() },
                            enabled = !preserveOriginalAudio,
                            addTonalElevation = false,
                            onClick = { showAudioQualityDialog = true },
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                preserveOriginalAudio = it
                                PreferencesUtil.updateValue(ORIGINAL_AUDIO, it)
                            },
                            checked = preserveOriginalAudio,
                            title = { Text(stringResource(id = R.string.preserve_original_audio), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.preserve_original_audio_desc)) },
                            icon = Icons.Outlined.Audiotrack,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                useSponsorBlock = it
                                PreferencesUtil.updateValue(SPONSORBLOCK, it)
                            },
                            checked = useSponsorBlock,
                            title = { Text(stringResource(id = R.string.sponsorblock), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.sponsorblock_desc)) },
                            icon = Icons.Outlined.MoneyOff,
                            addTonalElevation = false,
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.section_organisation))
                }
                item {
                    SettingsGroup {
                        SettingsItemNew(
                            title = { Text(stringResource(id = R.string.output_format), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.output_format_desc)) },
                            icon = Icons.Outlined.Output,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            onClick = { showOutputFormatDialog = true },
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                splitByPlaylist = it
                                PreferencesUtil.updateValue(SPLIT_BY_PLAYLIST, it)
                            },
                            checked = splitByPlaylist,
                            title = { Text(stringResource(id = R.string.split_playlist), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.split_playlist_desc)) },
                            icon = Icons.Outlined.Album,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                splitByMainArtist = it
                                PreferencesUtil.updateValue(SPLIT_BY_MAIN_ARTIST, it)
                            },
                            checked = splitByMainArtist,
                            title = { Text(stringResource(id = R.string.split_by_main_artist), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.split_by_main_artist_desc)) },
                            icon = Icons.Outlined.Audiotrack,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = { Text(stringResource(id = R.string.playlist_tagging), fontWeight = FontWeight.Bold) },
                            description = {
                                Text(
                                    when (playlistTagging) {
                                        0 -> stringResource(id = R.string.playlist_tagging_off)
                                        1 -> stringResource(id = R.string.playlist_tagging_album)
                                        else -> stringResource(id = R.string.playlist_tagging_album_cover)
                                    }
                                )
                            },
                            icon = Icons.Outlined.Lyrics,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            onClick = { showPlaylistTaggingDialog = true },
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.section_metadata))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = {
                                downloadLyrics = it
                                PreferencesUtil.updateValue(DOWNLOAD_LYRICS, it)
                            },
                            checked = downloadLyrics,
                            title = { Text(stringResource(id = R.string.download_lyrics), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.download_lyrics_desc)) },
                            icon = Icons.Outlined.Download,
                            addTonalElevation = false,
                        )
                        if (downloadLyrics) {
                            SettingsDivider()
                            SettingsItemNew(
                                title = { Text(stringResource(id = R.string.lyric_providers), fontWeight = FontWeight.Bold) },
                                description = { Text(stringResource(id = R.string.lyric_providers_desc)) },
                                icon = Icons.Outlined.Lyrics,
                                trailing = { SettingsChevron() },
                                addTonalElevation = false,
                                onClick = { showAudioLyricDialog = true },
                            )
                        }
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                generateLRC = it
                                PreferencesUtil.updateValue(GENERATE_LRC, it)
                            },
                            checked = generateLRC,
                            title = { Text(stringResource(id = R.string.generate_lrc), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.generate_lrc_desc)) },
                            icon = Icons.AutoMirrored.Outlined.Article,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                skipAlbumArt = it
                                PreferencesUtil.updateValue(SKIP_ALBUM_ART, it)
                            },
                            checked = skipAlbumArt,
                            title = { Text(stringResource(id = R.string.skip_album_art), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.skip_album_art_desc)) },
                            icon = Icons.Outlined.Album,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                useYTMetadata = it
                                PreferencesUtil.updateValue(USE_YT_METADATA, it)
                            },
                            checked = useYTMetadata,
                            title = { Text(stringResource(id = R.string.use_yt_metadata), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.use_yt_metadata_desc)) },
                            icon = YouTubeMusic,
                            addTonalElevation = false,
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.section_matching))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = {
                                onlyVerifiedResults = it
                                PreferencesUtil.updateValue(ONLY_VERIFIED_RESULTS, it)
                            },
                            checked = onlyVerifiedResults,
                            title = { Text(stringResource(id = R.string.only_verified_results), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.only_verified_results_desc)) },
                            icon = Icons.Outlined.Verified,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                skipExplicit = it
                                PreferencesUtil.updateValue(SKIP_EXPLICIT, it)
                            },
                            checked = skipExplicit,
                            title = { Text(stringResource(id = R.string.skip_explict), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.skip_explict_desc)) },
                            icon = Icons.Outlined.SkipNext,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            onCheckedChange = {
                                dontFilter = it
                                PreferencesUtil.updateValue(DONT_FILTER_RESULTS, it)
                            },
                            checked = dontFilter,
                            title = { Text(stringResource(id = R.string.dont_filter_results), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.dont_filter_results_desc)) },
                            icon = Icons.Outlined.Filter,
                            addTonalElevation = false,
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.network))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = {
                                cellularDownload = it
                                PreferencesUtil.updateValue(CELLULAR_DOWNLOAD, it)
                            },
                            checked = cellularDownload,
                            title = { Text(stringResource(id = R.string.cellular_download), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.cellular_download_desc)) },
                            icon = Icons.Outlined.CellTower,
                            addTonalElevation = false,
                        )
                        SettingsDivider()
                        SettingsItemNew(
                            title = { Text(stringResource(id = R.string.proxy), fontWeight = FontWeight.Bold) },
                            description = { Text(proxy.ifBlank { stringResource(id = R.string.proxy_not_set) }) },
                            icon = Icons.Outlined.Public,
                            trailing = { SettingsChevron() },
                            addTonalElevation = false,
                            onClick = { showProxyDialog = true },
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.section_performance))
                }
                item {
                    SettingsGroup {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                )
                                Text(
                                    text = stringResource(id = R.string.threads),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .weight(1f)
                                )
                                Text(
                                    text = threadsState.intValue.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.threads_number_desc),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = threadsState.intValue.toFloat(),
                                onValueChange = {
                                    threadsState.intValue = it.toInt()
                                    THREADS.updateInt(it.toInt())
                                },
                                valueRange = 1f..10f,
                                steps = 9,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.section_privacy))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = {
                                incognitoMode = it
                                PreferencesUtil.updateValue(INCOGNITO_MODE, it)
                            },
                            checked = incognitoMode,
                            title = { Text(stringResource(id = R.string.incognito_mode), fontWeight = FontWeight.Bold) },
                            description = { Text(stringResource(id = R.string.incognito_mode_desc)) },
                            icon = Icons.Outlined.VisibilityOff,
                            addTonalElevation = false,
                        )
                    }
                }
            }
        }
    )

    if (showAudioFormatDialog) {
        AudioFormatDialog(onDismissRequest = { showAudioFormatDialog = false }) {
            audioFormat = PreferencesUtil.getAudioFormatDesc()
        }
    }
    if (showAudioQualityDialog) {
        AudioQualityDialog(onDismissRequest = { showAudioQualityDialog = false }) {
            audioQuality = PreferencesUtil.getAudioQualityDesc()
        }
    }
    if (showAudioProviderDialog) {
        AudioProviderDialog(onDismissRequest = { showAudioProviderDialog = false })
    }
    if (showAudioLyricDialog) {
        LyricProviderDialog(onDismissRequest = { showAudioLyricDialog = false })
    }
    if (showOutputFormatDialog) {
        OutputFormatDialog(onDismissRequest = { showOutputFormatDialog = false })
    }
    if (showPlaylistTaggingDialog) {
        PlaylistTaggingDialog(
            onDismissRequest = { showPlaylistTaggingDialog = false },
            onConfirm = { playlistTagging = PreferencesUtil.getPlaylistTagging() },
        )
    }
    if (showProxyDialog) {
        ProxyDialog(
            onDismissRequest = { showProxyDialog = false },
            onConfirm = { proxy = PreferencesUtil.getProxy() },
        )
    }
}
