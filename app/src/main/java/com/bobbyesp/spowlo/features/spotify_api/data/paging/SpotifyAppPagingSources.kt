package com.bobbyesp.spowlo.features.spotify_api.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.models.*

// MUDANÇA: A API agora é um parâmetro obrigatório e não-nulo (val em vez de var).
class TrackPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0

        // MUDANÇA: A verificação "if (spotifyApi == null)" foi removida.
        return try {
            val response = spotifyApi.search.searchTrack(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            val tracks = response.items
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        return state.anchorPosition
    }
}

class SimpleAlbumPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, SimpleAlbum>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleAlbum> {
        val offset = params.key ?: 0

        return try {
            val response = spotifyApi.search.searchAlbum(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            val albums = response.items
            LoadResult.Page(
                data = albums,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (albums.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleAlbum>): Int? {
        return state.anchorPosition
    }
}

class AlbumTracksPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val albumId: String,
) : PagingSource<Int, SimpleTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimpleTrack> {
        val offset = params.key ?: 0

        return try {
            val response = spotifyApi.albums.getAlbumTracks(
                limit = params.loadSize,
                offset = offset,
                album = albumId,
                market = null,
            )

            val tracks = response.items
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, SimpleTrack>): Int? {
        return state.anchorPosition
    }
}

class ArtistsPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, Artist>() {
    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        val offset = params.key ?: 0

        return try {
            val response = spotifyApi.search.searchArtist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            val artists = response.items
            LoadResult.Page(
                data = artists,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (artists.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}

class SimplePlaylistPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val query: String,
    private val filters: List<SearchFilter> = emptyList(),
) : PagingSource<Int, SimplePlaylist>() {
    override fun getRefreshKey(state: PagingState<Int, SimplePlaylist>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SimplePlaylist> {
        val offset = params.key ?: 0

        return try {
            val response = spotifyApi.search.searchPlaylist(
                query = query,
                limit = params.loadSize,
                offset = offset,
                market = null,
                filters = filters,
            )

            val playlists = response.items
            LoadResult.Page(
                data = playlists,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (playlists.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}

class PlaylistTracksPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val playlistId: String,
) : PagingSource<Int, PlaylistTrack>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PlaylistTrack> {
        val offset = params.key ?: 0

        return try {
            val response = spotifyApi.playlists.getPlaylistTracks(
                limit = params.loadSize,
                offset = offset,
                playlist = playlistId,
                market = null,
            )

            val tracks = response.items
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PlaylistTrack>): Int? {
        return state.anchorPosition
    }
}

class PlaylistTracksAsTracksPagingSource(
    private val spotifyApi: SpotifyClientApi,
    private val playlistId: String,
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val offset = params.key ?: 0

        return try {
            val response = spotifyApi.playlists.getPlaylistTracks(
                limit = params.loadSize,
                offset = offset,
                playlist = playlistId,
                market = null,
            )

            val tracks = response.items.mapNotNull { it.track?.asTrack }
            LoadResult.Page(
                data = tracks,
                prevKey = if (offset > 0) offset - params.loadSize else null,
                nextKey = if (tracks.isNotEmpty()) offset + params.loadSize else null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        return state.anchorPosition
    }
}

/*
 * ATENÇÃO: A lógica desta classe parece incorreta.
 * Ela sempre retorna a primeira página de 'pagingObject.items' e não implementa a paginação
 * para buscar as páginas seguintes. A implementação correta exigiria chamar o método `next()`
 * do PagingObject para carregar os próximos itens.
 */
class CustomPagingSource<T : Any>(
    private val pagingObject: PagingObject<T>,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            // Esta lógica pega sempre a mesma lista de itens da primeira página.
            val items = pagingObject.items

            LoadResult.Page(
                data = items,
                prevKey = null, // Lógica de chave anterior/próxima precisa ser revista
                nextKey = null
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}