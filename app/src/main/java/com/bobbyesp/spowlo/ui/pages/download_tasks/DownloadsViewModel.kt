package com.bobbyesp.spowlo.ui.pages.download_tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobbyesp.spowlo.App.Companion.applicationScope
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.utils.DatabaseUtil
import com.bobbyesp.spowlo.utils.FilesUtil.getFileSize
import com.bobbyesp.spowlo.utils.HistoryMetadataMatcher
import com.bobbyesp.spowlo.utils.HistoryMetadataMatcher.withTrackMetadata
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class LibrarySort(val persistedValue: Int) {
    NEWEST(0),
    OLDEST(1),
    TITLE(2),
    ARTIST(3),
    DURATION(4),
    SIZE(5);

    companion object {
        fun fromPersisted(value: Int): LibrarySort =
            entries.firstOrNull { it.persistedValue == value } ?: NEWEST
    }
}

data class LibraryState(
    val songs: List<DownloadedSongInfo> = emptyList(),
    val allSongs: List<DownloadedSongInfo> = emptyList(),
    val sections: List<LibrarySection> = emptyList(),
    val query: String = "",
    /** Empty string filters for entries without a known artist, `null` shows every artist. */
    val author: String? = null,
    val sort: LibrarySort = LibrarySort.NEWEST,
) {
    val totalCount: Int get() = allSongs.size
    val isFiltered: Boolean get() = query.isNotBlank() || author != null
    /** Date sorts are grouped under one header per day, other sorts stay a flat list. */
    val groupedByDay: Boolean get() = sort == LibrarySort.NEWEST || sort == LibrarySort.OLDEST
}

data class LibrarySection(val dayStart: Long, val songs: List<DownloadedSongInfo>)

data class ArtistEntry(val name: String, val count: Int)

private val DIACRITICS = Regex("\\p{M}+")

class DownloadsViewModel : ViewModel() {

    private val mediaInfoFlow = DatabaseUtil.getMediaInfo()

    private val queryFlow = MutableStateFlow("")
    private val authorFlow = MutableStateFlow<String?>(null)
    private val sortFlow = MutableStateFlow(
        LibrarySort.fromPersisted(PreferencesUtil.getLibrarySort())
    )

    val fileSizes: StateFlow<Map<Int, Long>> = mediaInfoFlow
        .map { songs ->
            withContext(Dispatchers.IO) {
                songs.associate { info ->
                    info.id to runCatching { info.songPath.getFileSize() }.getOrDefault(0L)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val library: StateFlow<LibraryState> = combine(
        mediaInfoFlow, queryFlow, authorFlow, sortFlow, fileSizes
    ) { songs, query, author, sort, sizes ->
        buildLibraryState(songs, query, author, sort, sizes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    /** Every artist in the library with its song count, unknown artists last. */
    val artists: StateFlow<List<ArtistEntry>> = mediaInfoFlow
        .map { songs ->
            songs.groupingBy { it.songAuthor.trim() }.eachCount()
                .map { (name, count) -> ArtistEntry(name, count) }
                .sortedWith(
                    compareBy<ArtistEntry> { it.name.isEmpty() }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class MetadataProgress(
        val total: Int = 0,
        val completed: Int = 0,
        val inProgressId: Int? = null,
    ) {
        val isRunning: Boolean get() = total > 0 && completed < total
    }

    private val _metadataProgress = MutableStateFlow(MetadataProgress())
    val metadataProgress = _metadataProgress.asStateFlow()

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun setAuthor(author: String?) {
        authorFlow.value = author
    }

    fun setSort(sort: LibrarySort) {
        sortFlow.value = sort
        PreferencesUtil.setLibrarySort(sort.persistedValue)
    }

    fun clearFilters() {
        queryFlow.value = ""
        authorFlow.value = null
    }

    fun updateMetadata(ids: List<Int>) {
        if (ids.isEmpty() || _metadataProgress.value.isRunning) return
        viewModelScope.launch(Dispatchers.IO) {
            var updated = 0
            try {
                _metadataProgress.value = MetadataProgress(total = ids.size)
                ids.forEach { id ->
                    _metadataProgress.update { it.copy(inProgressId = id) }
                    val song = DatabaseUtil.getInfoByIdOrNull(id)
                    val track = song?.let { HistoryMetadataMatcher.findTrack(it) }
                    if (song != null && track != null) {
                        Downloader.writeTrackTags(track, song.songPath, song.songName)
                        DatabaseUtil.updateSongInfo(song.withTrackMetadata(track))
                        updated++
                    }
                    _metadataProgress.update {
                        it.copy(completed = it.completed + 1, inProgressId = null)
                    }
                }
            } finally {
                _metadataProgress.value = MetadataProgress()
            }
            ToastUtil.makeToastSuspend(context, metadataResultMessage(updated, ids.size))
        }
    }

    private fun metadataResultMessage(updated: Int, total: Int): String = when {
        updated == 0 -> context.getString(R.string.no_spotify_matches)
        updated < total -> context.getString(R.string.metadata_updated_partial, updated, total)
        else -> context.resources.getQuantityString(R.plurals.metadata_updated, updated, updated)
    }

    fun redownload(ids: List<Int>) {
        if (ids.isEmpty()) return
        applicationScope.launch(Dispatchers.IO) {
            ToastUtil.makeToastSuspend(
                context,
                context.resources.getQuantityString(
                    R.plurals.redownload_started, ids.size, ids.size
                )
            )
            var completed = 0
            ids.forEach { id ->
                val song = DatabaseUtil.getInfoByIdOrNull(id) ?: return@forEach
                val track = HistoryMetadataMatcher.findTrack(song) ?: return@forEach
                val url = track.externalUrls.spotify ?: return@forEach
                if (song.songUrl.isBlank() || song.thumbnailUrl.isBlank()) {
                    DatabaseUtil.updateSongInfo(song.withTrackMetadata(track))
                }
                if (Downloader.redownloadTrack(url, song.songName, song.songPath)) completed++
            }
            if (completed < ids.size) {
                ToastUtil.makeToastSuspend(
                    context,
                    if (completed == 0) {
                        context.getString(R.string.no_spotify_matches)
                    } else {
                        context.getString(R.string.redownload_partial, completed, ids.size)
                    }
                )
            }
        }
    }

    fun deleteItems(ids: Collection<Int>, deleteFile: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseUtil.deleteInfoListByIdList(ids.toList(), deleteFile)
        }
    }

    private fun buildLibraryState(
        songs: List<DownloadedSongInfo>,
        query: String,
        author: String?,
        sort: LibrarySort,
        sizes: Map<Int, Long>,
    ): LibraryState {
        val tokens = normalizeSearchText(query).split(' ').filter { it.isNotBlank() }
        val visible = songs.filter { song ->
            (author == null || song.songAuthor.trim() == author) &&
                (tokens.isEmpty() || song.matchesAll(tokens))
        }
        val sorted = visible.sortedWith(comparatorFor(sort, sizes))
        return LibraryState(
            songs = sorted,
            allSongs = songs,
            sections = if (sort == LibrarySort.NEWEST || sort == LibrarySort.OLDEST) {
                groupByDay(sorted)
            } else {
                listOf(LibrarySection(dayStart = 0L, songs = sorted))
            },
            query = query,
            author = author,
            sort = sort,
        )
    }

    private fun groupByDay(songs: List<DownloadedSongInfo>): List<LibrarySection> =
        songs.groupBy { dayStartOf(it.downloadedAt) }
            .map { (dayStart, daySongs) -> LibrarySection(dayStart, daySongs) }

    private fun comparatorFor(
        sort: LibrarySort,
        sizes: Map<Int, Long>,
    ): Comparator<DownloadedSongInfo> = when (sort) {
        // Entries without a download time stay at the bottom of either date order.
        LibrarySort.NEWEST -> compareBy<DownloadedSongInfo> { it.downloadedAt <= 0L }
            .thenByDescending { it.downloadedAt }
            .thenByDescending { it.id }

        LibrarySort.OLDEST -> compareBy<DownloadedSongInfo> { it.downloadedAt <= 0L }
            .thenBy { it.downloadedAt }
            .thenBy { it.id }

        LibrarySort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { song: DownloadedSongInfo ->
            song.songName
        }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.songAuthor }

        LibrarySort.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { song: DownloadedSongInfo ->
            song.songAuthor.trim()
        }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.songName }

        LibrarySort.DURATION -> compareByDescending<DownloadedSongInfo> { it.songDuration }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.songName }

        LibrarySort.SIZE -> compareByDescending<DownloadedSongInfo> { sizes[it.id] ?: 0L }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.songName }
    }
}

private fun DownloadedSongInfo.matchesAll(tokens: List<String>): Boolean {
    val haystack = normalizeSearchText("$songName $songAuthor")
    return tokens.all { haystack.contains(it) }
}

/** Local midnight of the day the song was downloaded, `0` when the timestamp is unknown. */
private fun dayStartOf(timestamp: Long): Long =
    if (timestamp <= 0L) 0L
    else Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun normalizeSearchText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
