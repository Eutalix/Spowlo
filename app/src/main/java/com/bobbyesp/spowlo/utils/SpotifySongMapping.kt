package com.bobbyesp.spowlo.utils

import com.adamratzman.spotify.models.Album
import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.ReleaseDate
import com.adamratzman.spotify.models.SimpleTrack
import com.adamratzman.spotify.models.Track
import com.bobbyesp.library.domain.model.SpotifySong

fun Track.toSpotifySong(fullAlbum: Album? = null, fullArtist: Artist? = null): SpotifySong =
    SpotifySong(
        name = name,
        artist = artists.firstOrNull()?.name ?: UNKNOWN_ARTIST,
        artists = artists.map { it.name },
        genres = fullArtist?.genres ?: emptyList(),
        album_name = album.name,
        album_artist = album.artists.joinToString(", ") { it.name },
        cover_url = album.images?.firstOrNull()?.url ?: "",
        duration = durationMs / 1000.0,
        disc_number = discNumber,
        disc_count = 1,
        tracks_count = fullAlbum?.totalTracks ?: album.totalTracks,
        album_id = fullAlbum?.id ?: album.id,
        isrc = externalIds.firstOrNull { it.key.equals("isrc", ignoreCase = true) }?.id,
        publisher = fullAlbum?.label,
        date = album.releaseDate.toIsoDate(),
        copyright_text = fullAlbum?.copyrights?.firstOrNull()?.text,
        url = externalUrls.spotify ?: "https://open.spotify.com/track/$id",
        song_id = id,
        explicit = explicit,
        year = album.releaseDate?.year ?: 0,
        track_number = trackNumber,
    )

fun SimpleTrack.toSpotifySong(album: Album): SpotifySong = SpotifySong(
    name = name,
    artist = artists.firstOrNull()?.name ?: UNKNOWN_ARTIST,
    artists = artists.map { it.name },
    album_name = album.name,
    album_artist = album.artists.joinToString(", ") { it.name },
    cover_url = album.images.firstOrNull()?.url ?: "",
    duration = durationMs / 1000.0,
    disc_number = discNumber,
    disc_count = 1,
    tracks_count = maxOf(album.totalTracks, album.tracks.total),
    album_id = album.id,
    publisher = album.label,
    date = album.releaseDate.toIsoDate(),
    copyright_text = album.copyrights.firstOrNull()?.text,
    url = externalUrls.spotify ?: "https://open.spotify.com/track/$id",
    song_id = id.ifBlank { uri.uri.substringAfterLast(':') },
    explicit = explicit,
    year = album.releaseDate?.year ?: 0,
    track_number = trackNumber,
)

private fun ReleaseDate?.toIsoDate(): String = when {
    this == null -> ""
    month != null && day != null -> "%04d-%02d-%02d".format(year, month, day)
    else -> year.toString()
}

private const val UNKNOWN_ARTIST = "Unknown Artist"
