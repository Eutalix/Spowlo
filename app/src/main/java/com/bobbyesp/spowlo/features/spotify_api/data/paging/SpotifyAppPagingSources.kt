package com.bobbyesp.spowlo.features.spotify_api.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adamratzman.spotify.SpotifyClientApi // THE FIX: Import the correct API type
import com.adamratzman.spotify.models.*
import com.bobbyesp.spowlo.features.spotify_api.data.remote.SpotifyApiRequests

class TrackPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX: Use the correct API type
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.search.searchTrack(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )


            if (response.isNotEmpty()) {
                val tracks = response.items

                LoadResult.Page(
                    data = tracks,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No tracks found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        return state.anchorPosition
    }
}

class SimpleAlbumPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, SimpleAlbum>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleAlbum> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.search.searchAlbum(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            if (response.isNotEmpty()) {
                val albums = response.items

                LoadResult.Page(
                    data = albums,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (albums.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No albums found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleAlbum>): Int? {
        return state.anchorPosition
    }
}

class AlbumTracksPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX
    private var albumId: String,
) : PagingSource<Int, SimpleTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleTrack> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.albums.getAlbumTracks(
                limit = params.loadSize,
                offset = offset,
                album = albumId,
                market = null,
            )

            if (response.isNotEmpty()) {
                val albums = response.items

                LoadResult.Page(
                    data = albums,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (albums.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No album tracks found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleTrack>): Int? {
        return state.anchorPosition
    }
}

class ArtistsPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, Artist>() {
    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.search.searchArtist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            if (response.isNotEmpty()) {
                val artists = response.items

                LoadResult.Page(
                    data = artists,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (artists.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No artists found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}

class SimplePlaylistPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX
    private var query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, SimplePlaylist>() {
    override fun getRefreshKey(state: PagingState<Int, SimplePlaylist>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimplePlaylist> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.search.searchPlaylist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            if (response.isNotEmpty()) {
                val playlists = response.items

                LoadResult.Page(
                    data = playlists,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (playlists.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No playlists found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

}

class PlaylistTracksPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX
    private var playlistId: String,
) : PagingSource<Int, PlaylistTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistTrack> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.playlists.getPlaylistTracks(
                limit = params.loadSize,
                offset = offset,
                playlist = playlistId,
                market = null,
            )

            if (response.isNotEmpty()) {
                val tracks = response.items

                LoadResult.Page(
                    data = tracks,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No album tracks found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PlaylistTrack>): Int? {
        return state.anchorPosition
    }
}

class PlaylistTracksAsTracksPagingSource(
    private var spotifyApi: SpotifyClientApi? = null, // THE FIX
    private var playlistId: String,
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0

        if (spotifyApi == null) {
            spotifyApi = SpotifyApiRequests.provideSpotifyApi()
        }

        return try {
            val response = spotifyApi!!.playlists.getPlaylistTracks(
                limit = params.loadSize,
                offset = offset,
                playlist = playlistId,
                market = null,
            )

            if (response.isNotEmpty()) {
                val tracks = response.items.mapNotNull { it.track?.asTrack }

                LoadResult.Page(
                    data = tracks,
                    prevKey = if (offset > 0) offset - params.loadSize else null,
                    nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
                )
            } else {
                LoadResult.Error(IllegalStateException("No album tracks found"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        return state.anchorPosition
    }
}

class CustomPagingSource<T : Any>(
    private var pagingObject: PagingObject<T>,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val offset = params.key ?: 0

        return try {
            val items = pagingObject.items

            LoadResult.Page(
                data = items,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (items.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}