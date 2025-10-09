package com.bobbyesp.spowlo.features.spotify_api.data.remote

import android.util.Log
import com.adamratzman.spotify.*
import com.adamratzman.spotify.models.*
import com.bobbyesp.library.SpotDL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.adamratzman.spotify.SpotifyMarket // <-- CORREÇÃO: Import adicionado

@Module
@InstallIn(SingletonComponent::class)
object SpotifyApiRequests {

    private var api: SpotifyClientApi? = null

    @Provides
    @Singleton
    suspend fun provideSpotifyApi(): SpotifyClientApi {
        if (api != null) {
            try {
                api!!.browse.getAvailableGenreSeeds()
                return api!!
            } catch (e: SpotifyException.AuthenticationException) {
                Log.w("SpotifyApiRequests", "Token expirado ou inválido. Reconstruindo API.")
            } catch (e: Exception) {
                Log.w("SpotifyApiRequests", "Verificação da API falhou, reconstruindo. Motivo: ${e.message}")
            }
        }

        buildApiUsingAnonymousToken()
        return api!!
    }

    private suspend fun buildApiUsingAnonymousToken() {
        try {
            Log.d("SpotifyApiRequests", "Tentando obter token anônimo do SpotDL...")
            val anonymousToken = SpotDL.getInstance().getAnonymousToken()
            val token = Token(anonymousToken, "Bearer", 3600)

            Log.d("SpotifyApiRequests", "Construindo API cliente com o token obtido.")

            api = spotifyClientApi(
                clientId = null,
                clientSecret = null,
                redirectUri = null,
                token = token
            ) {
                automaticRefresh = false
                // CORREÇÃO: A propriedade 'userAgent' foi removida pois não existe em SpotifyApiOptions,
                // causando o erro "Unresolved reference". A biblioteca usará seu valor padrão.
            }.build()

            Log.d("SpotifyApiRequests", "API construída com sucesso com token anônimo.")
        } catch (e: Exception) {
            Log.e("SpotifyApiRequests", "FATAL: Falha ao construir API usando token anônimo.", e)
            throw e
        }
    }

    private suspend fun userSearch(userQuery: String): SpotifyPublicUser? {
        return provideSpotifyApi().users.getProfile(userQuery)
    }

    @Provides
    @Singleton
    suspend fun provideUserSearch(query: String): SpotifyPublicUser? {
        return userSearch("bobbyesp")
    }

    suspend fun searchAllTypes(searchQuery: String): SpotifySearchResult {
        return kotlin.runCatching {
            provideSpotifyApi().search.searchAllTypes(
                searchQuery,
                limit = 50,
                offset = 0,
                market = SpotifyMarket.US
            )
        }.getOrElse {
            Log.d("SpotifyApiRequests", "Erro ao buscar todos os tipos: ${it.message}")
            SpotifySearchResult()
        }
    }

    @Provides
    @Singleton
    suspend fun provideSearchAllTypes(query: String): SpotifySearchResult {
        return searchAllTypes(query)
    }

    private suspend fun searchTracks(searchQuery: String): List<Track> {
        return kotlin.runCatching {
            provideSpotifyApi().search.searchTrack(searchQuery, limit = 50).items
        }.getOrElse {
            Log.d("SpotifyApiRequests", "Erro ao buscar faixas: ${it.message}")
            listOf()
        }
    }

    @Provides
    @Singleton
    suspend fun provideSearchTracks(query: String): List<Track> {
        return searchTracks(query)
    }

    suspend fun getPlaylistById(id: String): Playlist? {
        return kotlin.runCatching {
            provideSpotifyApi().playlists.getPlaylist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Erro ao obter playlist por ID: ${it.message}")
        }.getOrNull()
    }

    suspend fun getTrackById(id: String): Track? {
        return kotlin.runCatching {
            provideSpotifyApi().tracks.getTrack(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Erro ao obter faixa por ID: ${it.message}")
        }.getOrNull()
    }

    private suspend fun getArtistById(id: String): Artist? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtist(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Erro ao obter artista por ID: ${it.message}")
        }.getOrNull()
    }

    suspend fun getAlbumById(id: String): Album? {
        return kotlin.runCatching {
            provideSpotifyApi().albums.getAlbum(id)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Erro ao obter álbum por ID: ${it.message}")
        }.getOrNull()
    }

    private suspend fun getArtistTopTracks(artistId: String): List<Track>? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtistTopTracks(artistId, SpotifyMarket.US)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Erro ao obter faixas mais populares do artista: ${it.message}")
        }.getOrNull()
    }

    private suspend fun getArtistAlbums(artistId: String): PagingObject<SimpleAlbum>? {
        return kotlin.runCatching {
            provideSpotifyApi().artists.getArtistAlbums(artist = artistId, market = SpotifyMarket.US, limit = 20)
        }.onFailure {
            Log.d("SpotifyApiRequests", "Erro ao obter álbuns do artista: ${it.message}")
        }.getOrNull()
    }
}