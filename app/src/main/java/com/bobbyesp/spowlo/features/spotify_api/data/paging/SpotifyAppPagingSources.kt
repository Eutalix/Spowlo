package com.bobbyesp.spowlo.features.spotify_api.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adamratzman.spotify.models.*
import com.bobbyesp.library.SpotDL
import kotlinx.coroutines.delay

/**
 * Paging sources that provide paginated data from the Spotify API for the app's UI.
 * These sources now use the unified Spotify API instance provided by the :library module.
 */

// Helper function to safely get the API instance, with a retry mechanism.
private suspend fun getSpotifyApi(): SpotifyAppApi {
    var api = SpotDL.getInstance().spotifyApi
    var attempts = 0
    while (api == null && attempts < 50) { // Wait up to 5 seconds
        delay(100)
        api = SpotDL.getInstance().spotifyApi
        attempts++
    }
    return api ?: throw IllegalStateException("Spotify API from library is not available.")
}

class TrackPagingSource(
    private val query: String,
    private val filters: List<SearchFilter> = emptyList()
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0
        return try {
            val api = getSpotifyApi()
            val response = api.search.searchTrack(
                query = query,
                limit = params.loadSize,
                offset = offset,
                filters = filters
            )
            val tracks = response.items
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.e("TrackPagingSource", "Failed to load tracks", ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? = state.anchorPosition
}

class SimpleAlbumPagingSource(
    private val query: String,
    private val filters: List<SearchFilter> = emptyList()
) : PagingSource<Int, SimpleAlbum>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleAlbum> {
        val offset = params.key ?: 0
        return try {
            val api = getSpotifyApi()
            val response = api.search.searchAlbum(
                query = query,
                limit = params.loadSize,
                offset = offset,
                filters = filters
            )
            val albums = response.items
            LoadResult.Page(
                data = albums,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (albums.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.e("SimpleAlbumPagingSource", "Failed to load albums", ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleAlbum>): Int? = state.anchorPosition
}

class AlbumTracksPagingSource(
    private val albumId: String
) : PagingSource<Int, SimpleTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleTrack> {
        val offset = params.key ?: 0
        return try {
            val api = getSpotifyApi()
            val response = api.albums.getAlbumTracks(
                album = albumId,
                limit = params.loadSize,
                offset = offset,
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.e("AlbumTracksPagingSource", "Failed to load album tracks", ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleTrack>): Int? = state.anchorPosition
}

class ArtistsPagingSource(
    private val query: String,
    private val filters: List<SearchFilter> = emptyList()
) : PagingSource<Int, Artist>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        val offset = params.key ?: 0
        return try {
            val api = getSpotifyApi()
            val response = api.search.searchArtist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                filters = filters
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.e("ArtistsPagingSource", "Failed to load artists", ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? = state.anchorPosition
}

class SimplePlaylistPagingSource(
    private val query: String,
    private val filters: List<SearchFilter> = emptyList()
) : PagingSource<Int, SimplePlaylist>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimplePlaylist> {
        val offset = params.key ?: 0
        return try {
            val api = getSpotifyApi()
            val response = api.search.searchPlaylist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                filters = filters
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.e("SimplePlaylistPagingSource", "Failed to load playlists", ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimplePlaylist>): Int? = state.anchorPosition
}

class PlaylistTracksPagingSource(
    private val playlistId: String
) : PagingSource<Int, PlaylistTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistTrack> {
        val offset = params.key ?: 0
        return try {
            val api = getSpotifyApi()
            val response = api.playlists.getPlaylistTracks(
                playlist = playlistId,
                limit = params.loadSize,
                offset = offset
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.e("PlaylistTracksPagingSource", "Failed to load playlist tracks", ex)
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PlaylistTrack>): Int? = state.anchorPosition
}