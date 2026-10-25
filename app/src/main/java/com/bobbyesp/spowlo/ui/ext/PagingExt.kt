package com.bobbyesp.spowlo.ui.ext

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

fun <T : Any> LazyListScope.loadStateContent(
    items: LazyPagingItems<T>,
    itemCount: Int = 7,
    loadingContent: @Composable () -> Unit
) {
    items.apply {
        when {
            loadState.refresh is LoadState.Loading -> {
                items(itemCount) {
                    // Render a loading indicator while refreshing
                    loadingContent()
                }
            }

            loadState.append is LoadState.Loading -> {
                items(itemCount) {
                    // Render a loading indicator at the end while loading more items
                    loadingContent()
                }
            }

            loadState.refresh is LoadState.Error -> {
                item {
                    ErrorText((loadState.refresh as LoadState.Error).error)
                }
            }

            loadState.append is LoadState.Error -> {
                item {
                    ErrorText((loadState.append as LoadState.Error).error)
                }
            }
        }
    }
}

/** Always shown, so load errors are not swallowed and rendered as an empty list. */
@Composable
private fun ErrorText(error: Throwable) {
    Text(
        text = error.message ?: error::class.java.simpleName,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
