package com.bobbyesp.spowlo.ui.pages.metadata_viewer.pages

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.text.MarqueeText
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.dynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BACKDROP_FADE_PX = 640f
private const val ARTWORK_CORNER_DP = 24
private const val ARTWORK_MAX_DP = 272

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataScreen(
    topBarTitle: String,
    onBack: () -> Unit,
    backdrop: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    seed: Color? = null,
    content: @Composable (PaddingValues, LazyListState) -> Unit,
) {
    if (LocalMetadataArtworkColors.current) {
        ArtworkThemedScreen(modifier = modifier, seed = seed) {
            MetadataChrome(
                topBarTitle = topBarTitle,
                onBack = onBack,
                backdrop = backdrop,
                content = content,
            )
        }
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            MetadataChrome(
                topBarTitle = topBarTitle,
                onBack = onBack,
                backdrop = backdrop,
                content = content,
            )
        }
    }
}

@Composable
private fun ArtworkThemedScreen(
    modifier: Modifier,
    seed: Color?,
    content: @Composable () -> Unit,
) {
    val isLight = !LocalDarkTheme.current.isDarkTheme()
    val fallbackSeed = LocalTonalPalettes.current.keyColor
    val palettes = remember(seed, fallbackSeed) {
        artworkPalettes(seed ?: fallbackSeed)
    }

    CompositionLocalProvider(LocalTonalPalettes provides palettes) {
        val scheme = dynamicColorScheme(isLight)
        MaterialTheme(colorScheme = scheme) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = scheme.surface,
                contentColor = scheme.onSurface,
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataChrome(
    topBarTitle: String,
    onBack: () -> Unit,
    backdrop: @Composable BoxScope.() -> Unit,
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
            AsyncImageImpl(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.3f)
                    .blur(72.dp),
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
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
            .padding(top = 8.dp, bottom = 4.dp),
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
                .padding(start = 16.dp, end = 16.dp, top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            SelectionContainer {
                MarqueeText(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            details()
        }
    }
}

@Composable
fun CollectionArtwork(url: String) {
    val size = (LocalConfiguration.current.screenHeightDp / 3).coerceAtMost(ARTWORK_MAX_DP).dp
    val shape = RoundedCornerShape(ARTWORK_CORNER_DP.dp)
    AsyncImageImpl(
        modifier = Modifier
            .size(size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
            .clip(shape),
        model = url,
        contentDescription = stringResource(id = R.string.track_artwork),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun MetadataTags(
    tags: List<String>,
    trailing: @Composable () -> Unit = {},
) {
    val visible = tags.filter { it.isNotBlank() }
    if (visible.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visible.forEach { MetadataTag(it) }
        trailing()
    }
}

@Composable
fun MetadataTag(text: String, icon: ImageVector? = null) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
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
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onArtistClick(artist.id) }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .alpha(alpha = 0.8f),
            )
        }
    }
}

@Composable
fun MetadataLine(text: String, emphasized: Boolean = false) {
    SelectionContainer {
        Text(
            text = text,
            style = if (emphasized) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodySmall
            },
            modifier = Modifier.alpha(alpha = 0.8f),
        )
    }
}

@Composable
fun MetadataSectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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

@Composable
fun MetadataStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
