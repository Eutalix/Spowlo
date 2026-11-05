package com.bobbyesp.spowlo.ui.pages.settings.appearance

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.ui.common.LocalBottomBarEnabled
import com.bobbyesp.spowlo.ui.common.LocalDarkTheme
import com.bobbyesp.spowlo.ui.common.LocalDynamicColorSwitch
import com.bobbyesp.spowlo.ui.common.LocalMetadataArtworkColors
import com.bobbyesp.spowlo.ui.common.LocalPaletteStyleIndex
import com.bobbyesp.spowlo.ui.common.LocalSeedColor
import com.bobbyesp.spowlo.ui.common.Route
import com.bobbyesp.spowlo.ui.components.BackButton
import com.bobbyesp.spowlo.ui.components.LargeTopAppBar
import com.bobbyesp.spowlo.ui.components.settings.SettingsChevron
import com.bobbyesp.spowlo.ui.components.settings.SettingsDivider
import com.bobbyesp.spowlo.ui.components.settings.SettingsGroup
import com.bobbyesp.spowlo.ui.components.settings.SettingsItemNew
import com.bobbyesp.spowlo.ui.components.settings.SettingsSectionHeader
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitch
import com.bobbyesp.spowlo.ui.components.settings.SettingsSwitchWithDivider
import com.bobbyesp.spowlo.utils.DarkThemePreference.Companion.OFF
import com.bobbyesp.spowlo.utils.DarkThemePreference.Companion.ON
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.palettesMap
import com.google.android.material.color.DynamicColors
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.dynamicColorScheme
import io.material.hct.Hct

private val colorList: List<Color> =
    ((4..10) + (1..3)).map { it * 35.0 }.map { Color(Hct.from(it, 40.0, 40.0).toInt()) }

private val hueStyleIndices = listOf(0, 1, 2, 3, 4, 6)
private const val MONOCHROME_STYLE_INDEX = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePage(
    navController: NavHostController
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = darkTheme.isDarkTheme()
    val dynamicColorEnabled = LocalDynamicColorSwitch.current
    var showColorPicker by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = {
                Text(
                    text = stringResource(id = R.string.appearance),
                    fontWeight = FontWeight.Bold
                )
            }, navigationIcon = {
                BackButton { navController.popBackStack() }
            }, scrollBehavior = scrollBehavior
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
            ) {
                item {
                    SettingsSectionHeader(stringResource(id = R.string.theme))
                }
                item {
                    SettingsGroup {
                        SettingsSwitchWithDivider(
                            onCheckedChange = {
                                PreferencesUtil.modifyDarkThemePreference(
                                    if (isDarkTheme) OFF else ON
                                )
                            },
                            checked = isDarkTheme,
                            title = {
                                Text(
                                    text = stringResource(id = R.string.dark_theme),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = darkTheme.getDarkThemeDesc()) },
                            icon = Icons.Outlined.DarkMode,
                            onClick = { navController.navigate(Route.APP_THEME) },
                            addTonalElevation = false,
                        )
                        if (DynamicColors.isDynamicColorAvailable()) {
                            SettingsDivider()
                            SettingsSwitch(
                                onCheckedChange = { PreferencesUtil.switchDynamicColor(it) },
                                checked = dynamicColorEnabled,
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.dynamic_color),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                description = { Text(text = stringResource(id = R.string.dynamic_color_desc)) },
                                icon = Icons.Outlined.Wallpaper,
                                addTonalElevation = false,
                            )
                        }
                        SettingsDivider()
                        SettingsItemNew(
                            onClick = { showColorPicker = true },
                            enabled = !dynamicColorEnabled,
                            title = {
                                Text(
                                    text = stringResource(id = R.string.accent_color),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = {
                                Text(
                                    text = stringResource(
                                        if (dynamicColorEnabled) {
                                            R.string.accent_color_unavailable
                                        } else {
                                            R.string.accent_color_desc
                                        }
                                    )
                                )
                            },
                            icon = Icons.Outlined.ColorLens,
                            trailing = { ThemeSwatch() },
                            addTonalElevation = false,
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.metadata_pages))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = { PreferencesUtil.switchMetadataArtworkColors(it) },
                            checked = LocalMetadataArtworkColors.current,
                            title = {
                                Text(
                                    text = stringResource(id = R.string.metadata_artwork_colors),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = {
                                Text(text = stringResource(id = R.string.metadata_artwork_colors_desc))
                            },
                            icon = Icons.Outlined.Image,
                            addTonalElevation = false,
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.navigation))
                }
                item {
                    SettingsGroup {
                        SettingsSwitch(
                            onCheckedChange = { PreferencesUtil.switchBottomBar(it) },
                            checked = LocalBottomBarEnabled.current,
                            enabled = false,
                            title = {
                                Text(
                                    text = stringResource(id = R.string.bottom_bar),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            description = { Text(text = stringResource(id = R.string.bottom_bar_desc)) },
                            icon = Icons.Outlined.ViewAgenda,
                            addTonalElevation = false,
                        )
                    }
                }

                item {
                    SettingsSectionHeader(stringResource(id = R.string.additional_settings))
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    item {
                        SettingsGroup {
                            SettingsItemNew(
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.language),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                icon = Icons.Outlined.Language,
                                description = {
                                    Text(text = com.bobbyesp.spowlo.utils.getLanguageDesc())
                                },
                                trailing = { SettingsChevron() },
                                addTonalElevation = false,
                                onClick = { navController.navigate(Route.LANGUAGES) },
                            )
                        }
                    }
                }
            }
        })

    if (showColorPicker) {
        ThemeColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onSelect = { colorArgb, styleIndex ->
                PreferencesUtil.switchDynamicColor(enabled = false)
                PreferencesUtil.modifyThemeSeedColor(colorArgb, styleIndex)
            },
        )
    }
}

@Composable
private fun ThemeSwatch() {
    val color = Color(LocalSeedColor.current)
    val style = palettesMap[LocalPaletteStyleIndex.current] ?: PaletteStyle.TonalSpot
    val palettes = remember(color, style) { color.toTonalPalettes(style) }
    PaletteStrip(
        modifier = Modifier.size(32.dp),
        palettes = palettes,
        isLight = !LocalDarkTheme.current.isDarkTheme(),
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun PaletteStrip(
    modifier: Modifier,
    palettes: TonalPalettes,
    isLight: Boolean,
    shape: Shape = RoundedCornerShape(14.dp),
    selected: Boolean = false,
) {
    CompositionLocalProvider(LocalTonalPalettes provides palettes) {
        val scheme = dynamicColorScheme(isLight)
        Box(
            modifier = modifier
                .clip(shape)
                .background(scheme.surfaceContainer)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
                    } else {
                        Modifier
                    }
                ),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                PaletteBand(scheme.primaryContainer)
                PaletteBand(scheme.primary)
                PaletteBand(scheme.secondary)
                PaletteBand(scheme.tertiary)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.PaletteBand(color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(color)
    )
}

@Composable
private fun ThemeColorPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int, Int) -> Unit,
) {
    val seedColor = Color(LocalSeedColor.current)
    val selectedStyleIndex = LocalPaletteStyleIndex.current
    val isLight = !LocalDarkTheme.current.isDarkTheme()
    var hue by remember(seedColor) { mutableStateOf(nearestHue(seedColor)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(id = R.string.accent_color),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.accent_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    colorList.forEach { candidate ->
                        HueSwatch(
                            color = candidate,
                            selected = candidate == hue,
                            onClick = { hue = candidate },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                hueStyleIndices.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { styleIndex ->
                            StylePreview(
                                color = hue,
                                style = palettesMap.getValue(styleIndex),
                                isLight = isLight,
                                selected = seedColor == hue && selectedStyleIndex == styleIndex,
                                label = paletteStyleName(styleIndex),
                                onClick = { onSelect(hue.toArgb(), styleIndex) },
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                MonochromeOption(
                    color = hue,
                    isLight = isLight,
                    selected = selectedStyleIndex == MONOCHROME_STYLE_INDEX,
                    onClick = { onSelect(hue.toArgb(), MONOCHROME_STYLE_INDEX) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.done))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.HueSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
private fun RowScope.StylePreview(
    color: Color,
    style: PaletteStyle,
    isLight: Boolean,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val palettes = remember(color, style) { color.toTonalPalettes(style) }
    Column(modifier = Modifier.weight(1f)) {
        PaletteStrip(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .clickable(onClick = onClick),
            palettes = palettes,
            isLight = isLight,
            selected = selected,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun MonochromeOption(
    color: Color,
    isLight: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palettes = remember(color) { color.toTonalPalettes(PaletteStyle.Monochrome) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaletteStrip(
            modifier = Modifier.size(width = 64.dp, height = 44.dp),
            palettes = palettes,
            isLight = isLight,
            shape = RoundedCornerShape(10.dp),
            selected = selected,
        )
        Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.palette_monochrome),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun paletteStyleName(index: Int): String = stringResource(
    when (index) {
        0 -> R.string.palette_tonal
        1 -> R.string.palette_spritz
        2 -> R.string.palette_fruit_salad
        3 -> R.string.palette_vibrant
        4 -> R.string.palette_spotify
        6 -> R.string.palette_rainbow
        else -> R.string.palette_monochrome
    }
)

private fun nearestHue(color: Color): Color {
    val hue = Hct.fromInt(color.toArgb()).hue
    return colorList.minByOrNull { candidate ->
        val delta = kotlin.math.abs(Hct.fromInt(candidate.toArgb()).hue - hue) % 360.0
        if (delta > 180.0) 360.0 - delta else delta
    } ?: colorList.first()
}


