package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.adamratzman.spotify.models.SimpleArtist
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.common.LocalDarkTheme
import com.bobbyesp.spowlo.ui.common.LocalMetadataArtworkColors
import com.bobbyesp.spowlo.ui.components.AnchoredMenu
import com.bobbyesp.spowlo.ui.components.AnchoredMenuItem
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.theme.metadataHeroTitle
import com.bobbyesp.spowlo.utils.ChromeCustomTabsUtil
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.dynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BACKDROP_FADE_PX = 640f
private const val ARTWORK_CORNER_DP = 20
private const val ARTWORK_MAX_DP = 212
private const val ARTWORK_WIDTH_FRACTION = 0.52f

val ArtworkShape = RoundedCornerShape(ARTWORK_CORNER_DP.dp)

/** Green is reserved for "saved" state, never for generic accents. */
val SavedGreen = Color(0xFF3F9D5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataScreen(
    topBarTitle: String,
    onBack: () -> Unit,
    backdrop: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    seed: Color? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues, LazyListState) -> Unit,
) {
    ArtworkPaletteTheme(seed = seed) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            MetadataChrome(
                topBarTitle = topBarTitle,
                onBack = onBack,
                backdrop = backdrop,
                actions = actions,
                content = content,
            )
        }
    }
}

/**
 * Applies the artwork-derived palette when artwork colours are enabled.
 * Also used by sheets and dialogs emitted outside [MetadataScreen] so they match the page.
 */
@Composable
fun ArtworkPaletteTheme(seed: Color?, content: @Composable () -> Unit) {
    if (!LocalMetadataArtworkColors.current) {
        content()
        return
    }
    val isLight = !LocalDarkTheme.current.isDarkTheme()
    val fallbackSeed = LocalTonalPalettes.current.keyColor
    val palettes = remember(seed, fallbackSeed) {
        artworkPalettes(seed ?: fallbackSeed)
    }

    CompositionLocalProvider(LocalTonalPalettes provides palettes) {
        MaterialTheme(colorScheme = dynamicColorScheme(isLight), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataChrome(
    topBarTitle: String,
    onBack: () -> Unit,
    backdrop: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (PaddingValues, LazyListState) -> Unit,
) {
    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / BACKDROP_FADE_PX).coerceIn(0f, 1f)
        }
    }
    val barColor = lerp(Color.Transparent, MaterialTheme.colorScheme.surface, scrolled)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - scrolled),
            content = backdrop,
        )
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(scrolled),
                        )
                    },
                    navigationIcon = { BackButton { onBack() } },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = barColor),
                )
            },
        ) { paddingValues ->
            content(paddingValues, listState)
        }
    }
}

@Composable
fun rememberArtworkSeed(url: String): Color? {
    val context = LocalContext.current
    val enabled = LocalMetadataArtworkColors.current
    var seed by remember(url) { mutableStateOf<Color?>(null) }

    LaunchedEffect(url, enabled) {
        if (url.isEmpty() || !enabled) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(160)
            .build()
        val bitmap = (context.imageLoader.execute(request) as? SuccessResult)
            ?.drawable?.toBitmap() ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) {
            Palette.from(bitmap).generate()
        }
        seed = palette.bestSeed()
    }

    return seed
}

private fun Palette.bestSeed(): Color? {
    val swatch = vibrantSwatch
        ?: lightVibrantSwatch
        ?: darkVibrantSwatch
        ?: mutedSwatch
        ?: lightMutedSwatch
        ?: darkMutedSwatch
        ?: dominantSwatch
    return swatch?.let { Color(it.rgb) }
}

private fun artworkPalettes(seed: Color) = seed.toTonalPalettes(
    if (seed.chromaFraction() < 0.12f) PaletteStyle.Monochrome else PaletteStyle.Spotify
)

private fun Color.chromaFraction(): Float {
    val high = maxOf(red, green, blue)
    val low = minOf(red, green, blue)
    return if (high <= 0.02f) 0f else (high - low) / high
}

@Composable
fun ArtworkBackdrop(url: String) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier = Modifier.fillMaxSize()) {
        if (url.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AsyncImageImpl(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.3f)
                        .blur(72.dp),
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    surface,
                                )
                            )
                        ),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = 0.2f),
                        0.32f to surface.copy(alpha = 0.5f),
                        0.56f to surface.copy(alpha = 0.88f),
                        0.72f to surface,
                        1f to surface,
                    )
                )
        )
    }
}

@Composable
fun ColorBackdrop(color: Color) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to color.copy(alpha = 0.5f),
                    0.4f to lerp(color, surface, 0.55f).copy(alpha = 0.75f),
                    0.66f to surface,
                    1f to surface,
                )
            )
    )
}

@Composable
fun MetadataHero(
    title: String,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    details: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
            content = artwork,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            SelectionContainer {
                Text(
                    text = title,
                    style = metadataHeroTitle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            details()
        }
    }
}

@Composable
fun rememberArtworkSize(): Dp {
    val width = LocalConfiguration.current.screenWidthDp
    return (width * ARTWORK_WIDTH_FRACTION).coerceIn(168f, ARTWORK_MAX_DP.toFloat()).dp
}

@Composable
fun CollectionArtwork(
    url: String,
    scrollState: LazyListState? = null,
    shape: Shape = ArtworkShape,
) {
    val size = rememberArtworkSize()
    val sizePx = with(LocalDensity.current) { size.toPx() }
    Box(
        modifier = Modifier
            .size(size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
            .clip(shape),
    ) {
        AsyncImageImpl(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val offset = if (scrollState != null && scrollState.firstVisibleItemIndex == 0) {
                        scrollState.firstVisibleItemScrollOffset
                    } else {
                        0
                    }
                    val progress = (offset / (sizePx * 0.8f)).coerceIn(0f, 1f)
                    translationY = -sizePx * 0.05f * progress
                    scaleX = 1f + 0.12f * progress
                    scaleY = 1f + 0.12f * progress
                },
            model = url,
            contentDescription = stringResource(id = R.string.track_artwork),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun MetadataMetaLine(vararg parts: String, modifier: Modifier = Modifier) {
    val visible = parts.filter { it.isNotBlank() }
    if (visible.isEmpty()) return
    Text(
        text = visible.joinToString("  ·  "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.2.sp,
        modifier = modifier,
    )
}

@Composable
fun MetadataArtistLine(
    artists: List<SimpleArtist>,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (artists.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        artists.forEachIndexed { index, artist ->
            Text(
                text = artist.name + if (index == artists.lastIndex) "" else ",",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onArtistClick(artist.id) }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun MetadataOwnerLine(name: String, imageUrl: String = "", modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (imageUrl.isNotEmpty()) {
            AsyncImageImpl(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape),
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MetadataOverflowMenu(spotifyUrl: String) {
    val clipboard = LocalClipboardManager.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnchoredMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AnchoredMenuItem(
                text = stringResource(R.string.open_in_spotify),
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                enabled = spotifyUrl.isNotBlank(),
                onClick = {
                    expanded = false
                    ChromeCustomTabsUtil.openUrl(spotifyUrl)
                },
            )
            AnchoredMenuItem(
                text = stringResource(R.string.copy_link),
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                enabled = spotifyUrl.isNotBlank(),
                onClick = {
                    expanded = false
                    clipboard.setText(AnnotatedString(spotifyUrl))
                },
            )
        }
    }
}

@Composable
fun MetadataSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.6.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 8.dp),
    )
}

@Composable
fun MetadataDetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(content = content)
    }
}

@Composable
fun MetadataDetailRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.6f),
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }
    }
}


