package com.bobbyesp.library.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifySong(
    val name: String = "",
    val artists: List<String> = listOf(),
    val artist: String = "",
    val album_name: String = "",
    val album_artist: String = "",
    val genres: List<String>? = emptyList(),
    val disc_number: Int? = 0,
    val disc_count: Int? = 0,
    // Changed duration to Double? to handle potential nulls and type mismatches more gracefully.
    // kotlinx.serialization is smart enough to coerce Int to Double.
    val duration: Double? = 0.0,
    val year: Int? = 0,
    val date: String = "",
    val track_number: Int? = 0,
    val tracks_count: Int? = 0,
    val song_id: String = "",
    val explicit: Boolean = false,
    val publisher: String? = null,
    val url: String = "",
    val isrc: String? = null,
    val cover_url: String = "",
    val copyright_text: String? = null,
    val download_url: String? = null,
    
    // Adding new fields from the spotdl v4 JSON with default null values
    val popularity: Int? = null,
    val list_name: String? = null,
    val list_url: String? = null,
    val list_position: Int? = null,
    val list_length: Int? = null,
    val artist_id: String? = null,
    val album_type: String? = null,
    
    // This field from the old version can be removed or kept.
    // With ignoreUnknownKeys=true, it's safe to keep it.
    val song_list: SpotifyPlaylist? = null,
    
    val lyrics: String? = null,
    val album_id: String? = null
)

@Serializable
data class SpotifyPlaylist(
    val name: String = "",
    val url: String = "",
    val urls: List<String> = listOf(),
    // This field might be obsolete. Let's make it nullable for safety.
    val spotifySongs: List<SpotifySong>? = null,
    val genres: List<String>? = listOf(),
    val albums: List<String>? = listOf(),
    // This field might be obsolete. Let's make it nullable for safety.
    val artist: SpotifyArtist? = null,
    val description: String = "",
    val author_url: String = "",
    val author_name: String = "",
    val cover_url: String = "",
)

@Serializable
data class SpotifyArtist(
    val external_urls: Map<String, String> = emptyMap(),
    val href: String = "",
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val uri: String = ""
)