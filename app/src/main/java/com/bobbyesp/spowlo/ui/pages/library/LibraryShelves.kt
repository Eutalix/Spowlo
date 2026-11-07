package com.bobbyesp.spowlo.ui.pages.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeCard
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeCardType
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.HomeShelf
import com.bobbyesp.spowlo.features.spotify_api.data.remote.web.UserPlaylist

/**
 * The account home feed, with Spotify's own "Your playlists" section swapped for the complete
 * library list: the home section only carries the most recent handful.
 */
@Composable
internal fun rememberLibraryShelves(state: UserLibraryState.Ready): List<HomeShelf> {
    val madeForYouTitle = stringResource(R.string.made_for_you)
    val yourPlaylistsTitle = stringResource(R.string.your_playlists)
    val ownPlaylistIds = remember(state.playlists) {
        state.playlists.mapTo(HashSet()) { it.id }
    }
    val playlistCards = remember(state.playlists) {
        state.playlists.map { it.asHomeCard() }
    }

    return remember(state.shelves, state.mixes, playlistCards, ownPlaylistIds, madeForYouTitle, yourPlaylistsTitle) {
        val shelves = buildList {
            addAll(state.shelves)
            if (state.shelves.isEmpty() && state.mixes.isNotEmpty()) {
                add(
                    HomeShelf(
                        id = "made-for-you",
                        title = madeForYouTitle,
                        cards = state.mixes.map { it.asHomeCard() },
                    )
                )
            }
        }

        if (playlistCards.isEmpty()) return@remember shelves

        buildList {
            add(
                HomeShelf(
                    id = "library-playlists",
                    title = yourPlaylistsTitle,
                    cards = playlistCards,
                )
            )
            addAll(shelves.filterNot { it.isOwnPlaylistsShelf(ownPlaylistIds) })
        }
    }
}

/** A shelf is the user's own playlists only when every card is one of their playlists. */
private fun HomeShelf.isOwnPlaylistsShelf(ownPlaylistIds: Set<String>): Boolean =
    cards.isNotEmpty() &&
        cards.all { it.type == HomeCardType.PLAYLIST && it.id in ownPlaylistIds }

internal fun UserPlaylist.asHomeCard() = HomeCard(
    id = id,
    type = HomeCardType.PLAYLIST,
    title = name,
    subtitle = owner,
    imageUrl = imageUrl,
)
