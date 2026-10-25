package com.bobbyesp.spowlo.features.spotify_api.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playable
import com.adamratzman.spotify.models.PlaylistTrack
import com.adamratzman.spotify.models.SearchFilter
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimplePlaylist
import com.adamratzman.spotify.models.SimpleTrack
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi

private const val TAG = "SpotifyPaging"

/**
 * Builds the next page key. An empty page means the list ended, not that the
 * request failed; reporting [LoadResult.Error] there made the UI retry forever.
 */
private fun nextKey(itemsSize: Int, offset: Int, loadSize: Int): Int? =
    if (itemsSize < loadSize) null else offset + loadSize

private fun <T : Any> page(items: List<T>, offset: Int, loadSize: Int): PagingSource.LoadResult<Int, T> =
    PagingSource.LoadResult.Page(
        data = items,
        prevKey = if (offset > 0) (offset - loadSize).coerceAtLeast(0) else null,
        nextKey = nextKey(items.size, offset, loadSize),
    )

private fun <T : Any> failure(operation: String, exception: Exception): PagingSource.LoadResult<Int, T> {
    Log.e(TAG, "$operation failed: ${exception.message}", exception)
    return PagingSource.LoadResult.Error(exception)
}

class TrackPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.searchTracks(query, params.loadSize, offset)
            page(response.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Track search", exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? = state.anchorPosition
}

class SimpleAlbumPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, SimpleAlbum>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleAlbum> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.searchAlbums(query, params.loadSize, offset)
            page(response.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Album search", exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleAlbum>): Int? = state.anchorPosition
}

class AlbumTracksPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var albumId: String,
) : PagingSource<Int, SimpleTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleTrack> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.getAlbumTracks(albumId, params.loadSize, offset)
            page(response.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Album tracks", exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleTrack>): Int? = state.anchorPosition
}

class ArtistsPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, Artist>() {

    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.searchArtists(query, params.loadSize, offset)
            page(response.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Artist search", exception)
        }
    }
}

class SimplePlaylistPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, SimplePlaylist>() {

    override fun getRefreshKey(state: PagingState<Int, SimplePlaylist>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimplePlaylist> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.searchPlaylists(query, params.loadSize, offset)
            page(response.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Playlist search", exception)
        }
    }
}

class PlaylistTracksPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var playlistId: String,
) : PagingSource<Int, PlaylistTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistTrack> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.getPlaylistTracks(playlistId, params.loadSize, offset)
            page(response.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Playlist tracks", exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PlaylistTrack>): Int? = state.anchorPosition
}

class PlaylistTracksAsTracksPagingSource(
    @Suppress("UNUSED_PARAMETER") private var spotifyApi: SpotifyAppApi? = null,
    private var playlistId: String,
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0
        return try {
            val response = SpotifyWebApi.getPlaylistTracks(playlistId, params.loadSize, offset)
            val tracks = response.items.mapNotNull { it.track?.asTrack }
            page(tracks, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Playlist tracks", exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? = state.anchorPosition
}

class CustomPagingSource<T : Any>(
    private var pagingObject: PagingObject<T>,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val offset = params.key ?: 0
        return try {
            page(pagingObject.items, offset, params.loadSize)
        } catch (exception: Exception) {
            failure("Custom paging", exception)
        }
    }
}
