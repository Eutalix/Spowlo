package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.PlaylistTrack
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimpleTrack
import com.adamratzman.spotify.models.SpotifyPublicUser
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Track
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebApi
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.SpotifyWebAuth

/** Facade used by the metadata screens, backed by [SpotifyWebApi]. */
object SpotifyApiRequests {

    private const val TAG = "SpotifyApiRequests"

    suspend fun warmUp() {
        SpotifyWebApi.warmUp()
    }

    /** Drops the cached anonymous session. */
    suspend fun resetApi() {
        Log.d(TAG, "Resetting Spotify web session")
        SpotifyWebAuth.invalidate()
    }

    suspend fun provideUserSearch(query: String): SpotifyPublicUser? = null

    suspend fun provideSearchAllTypes(query: String): SpotifySearchResult {
        return runCatching {
            SpotifySearchResult(
                tracks = SpotifyWebApi.searchTracks(query, 50, 0),
                albums = SpotifyWebApi.searchAlbums(query, 50, 0),
                artists = SpotifyWebApi.searchArtists(query, 50, 0),
                playlists = SpotifyWebApi.searchPlaylists(query, 50, 0),
            )
        }.onFailure {
            Log.e(TAG, "searchAllTypes($query) failed: ${it.message}", it)
        }.getOrDefault(SpotifySearchResult())
    }

    suspend fun provideSearchTracks(query: String): List<Track> {
        return runCatching {
            SpotifyWebApi.searchTracks(query, 50, 0).items
        }.onFailure {
            Log.e(TAG, "searchTracks($query) failed: ${it.message}", it)
        }.getOrDefault(emptyList())
    }

    suspend fun provideGetPlaylistById(id: String): Playlist? {
        return runCatching {
            SpotifyWebApi.getPlaylist(id)
        }.onSuccess {
            Log.d(TAG, "Playlist found: ${it.name}")
        }.onFailure {
            Log.e(TAG, "getPlaylist($id) failed: ${it.message}", it)
        }.getOrNull()
    }

    suspend fun provideGetPlaylistTracksPage(
        id: String,
        limit: Int = SpotifyWebApi.PLAYLIST_PAGE_SIZE,
        offset: Int,
    ): PagingObject<PlaylistTrack> {
        return runCatching {
            SpotifyWebApi.getPlaylistTracks(id, limit, offset)
        }.onFailure {
            Log.e(TAG, "getPlaylistTracks($id, $offset) failed: ${it.message}", it)
        }.getOrDefault(PagingObject("", emptyList(), limit, null, offset, null, 0))
    }

    suspend fun provideGetTrackById(id: String): Track? {
        return runCatching {
            SpotifyWebApi.getTrack(id)
        }.onFailure {
            Log.e(TAG, "getTrack($id) failed: ${it.message}", it)
        }.getOrNull()
    }

    suspend fun provideGetArtistById(id: String): Artist? {
        return runCatching {
            SpotifyWebApi.getArtist(id)
        }.onFailure {
            Log.e(TAG, "getArtist($id) failed: ${it.message}", it)
        }.getOrNull()
    }

    suspend fun providesGetAlbumById(id: String): Album? {
        return runCatching {
            SpotifyWebApi.getAlbum(id)
        }.onFailure {
            Log.e(TAG, "getAlbum($id) failed: ${it.message}", it)
        }.getOrNull()
    }

    suspend fun providesGetAlbumTracksPage(
        id: String,
        limit: Int = SpotifyWebApi.ALBUM_PAGE_SIZE,
        offset: Int,
    ): PagingObject<SimpleTrack> {
        return runCatching {
            SpotifyWebApi.getAlbumTracks(id, limit, offset)
        }.onFailure {
            Log.e(TAG, "getAlbumTracks($id, $offset) failed: ${it.message}", it)
        }.getOrDefault(PagingObject("", emptyList(), limit, null, offset, null, 0))
    }

    /** Play count and monthly listeners from the detail queries, which the official models drop. */
    fun getTrackPlayCount(id: String): Long? = SpotifyWebApi.playCount(id)

    fun getArtistMonthlyListeners(id: String): Long? = SpotifyWebApi.monthlyListeners(id)

    /** Audio features are not exposed by the web player backend. */
    suspend fun providesGetAudioFeatures(id: String): AudioFeatures? = null

    suspend fun providesGetArtistTopTracks(id: String): List<Track>? {
        return runCatching {
            SpotifyWebApi.getArtistTopTracks(id)
        }.onFailure {
            Log.e(TAG, "getArtistTopTracks($id) failed: ${it.message}", it)
        }.getOrDefault(emptyList())
    }

    suspend fun providesGetArtistAlbums(id: String): PagingObject<SimpleAlbum>? {
        return runCatching {
            SpotifyWebApi.getArtistAlbums(id)
        }.onFailure {
            Log.e(TAG, "getArtistAlbums($id) failed: ${it.message}", it)
        }.getOrNull()
    }
}
