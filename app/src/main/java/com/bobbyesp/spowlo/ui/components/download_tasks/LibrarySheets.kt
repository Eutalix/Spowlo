package com.bobbyesp.spowlo.ui.components.download_tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.components.BottomSheet
import com.bobbyesp.spowlo.ui.pages.download_tasks.ArtistEntry
import com.bobbyesp.spowlo.ui.pages.download_tasks.LibrarySort

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun SheetHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun sortLabel(sort: LibrarySort): String = stringResource(
    when (sort) {
        LibrarySort.NEWEST -> R.string.sort_newest
        LibrarySort.OLDEST -> R.string.sort_oldest
        LibrarySort.TITLE -> R.string.sort_title
        LibrarySort.ARTIST -> R.string.sort_artist
        LibrarySort.DURATION -> R.string.sort_duration
        LibrarySort.SIZE -> R.string.sort_size
    }
)

@Composable
fun artistLabel(name: String): String =
    if (name.isBlank()) stringResource(R.string.unknown_artist) else name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    current: LibrarySort,
    onSelect: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(onDismiss = onDismiss, dragHandle = { SheetDragHandle() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(text = stringResource(R.string.sort_by))
            LibrarySort.entries.forEach { sort ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = sort == current,
                            role = Role.RadioButton,
                            onClick = { onSelect(sort) }
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = sort == current, onClick = null)
                    Spacer(Modifier.width(16.dp))
                    Text(text = sortLabel(sort), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSheet(
    artists: List<ArtistEntry>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(artists, query) {
        if (query.isBlank()) artists
        else artists.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    BottomSheet(onDismiss = onDismiss, dragHandle = { SheetDragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(text = stringResource(R.string.filter_artist))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                placeholder = { Text(text = stringResource(R.string.search_artists)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                item(key = "all") {
                    ArtistRow(
                        label = stringResource(R.string.all_artists),
                        count = artists.sumOf { it.count },
                        selected = selected == null,
                        onClick = { onSelect(null) }
                    )
                }
                items(filtered, key = { it.name }) { artist ->
                    ArtistRow(
                        label = artistLabel(artist.name),
                        count = artist.count,
                        selected = selected == artist.name,
                        onClick = { onSelect(artist.name) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ArtistRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
