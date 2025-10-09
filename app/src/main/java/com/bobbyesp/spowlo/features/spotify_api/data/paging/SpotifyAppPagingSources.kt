package com.bobbyesp.spowlo.features.spotify_api.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adamratzman.spotify.models.*
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests

/**
 * Paging sources that lazily obtain a SpotifyClientApi via SpotifyApiRequests.provideSpotifyApi().
 * This allows anonymous token usage by default and swapping credentials later.
 */

class TrackPagingSource(
    private val query: String,
    private val filters: List<SearchFilter> = emptyList()
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0
        return try {
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.search.searchTrack(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters
            )
            val tracks = response.items
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("TrackPagingSource", "Failed to load tracks: ${ex.message}")
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
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.search.searchAlbum(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters
            )
            val albums = response.items
            LoadResult.Page(
                data = albums,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (albums.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("SimpleAlbumPagingSource", "Failed to load albums: ${ex.message}")
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
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.albums.getAlbumTracks(
                album = albumId,
                limit = params.loadSize,
                offset = offset,
                market = null
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("AlbumTracksPagingSource", "Failed to load album tracks: ${ex.message}")
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
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.search.searchArtist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("ArtistsPagingSource", "Failed to load artists: ${ex.message}")
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
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.search.searchPlaylist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("SimplePlaylistPagingSource", "Failed to load playlists: ${ex.message}")
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
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.playlists.getPlaylistTracks(
                playlist = playlistId,
                limit = params.loadSize,
                offset = offset,
                market = null
            )
            val items = response.items
            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("PlaylistTracksPagingSource", "Failed to load playlist tracks: ${ex.message}")
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PlaylistTrack>): Int? = state.anchorPosition
}

class PlaylistTracksAsTracksPagingSource(
    private val playlistId: String
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0
        return try {
            val api = SpotifyApiRequests.provideSpotifyApi()
            val response = api.playlists.getPlaylistTracks(
                playlist = playlistId,
                limit = params.loadSize,
                offset = offset,
                market = null
            )
            val tracks = response.items.mapNotNull { it.track?.asTrack }
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (ex: Exception) {
            Log.w("PlaylistTracksAsTracks", "Failed to load playlist tracks as tracks: ${ex.message}")
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? = state.anchorPosition
}

/**
 * CustomPagingSource keeps a simple behavior: returns the provided PagingObject items.
 * Note: does not auto-fetch next pages via pagingObject.next() — keep or improve later.
 */
class CustomPagingSource<T : Any>(
    private val pagingObject: PagingObject<T>
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            val items = pagingObject.items
            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = null
            )
        } catch (ex: Exception) {
            Log.w("CustomPagingSource", "Failed to load custom paging object: ${ex.message}")
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = state.anchorPosition
}