package com.bobbyesp.spowlo.ui.pages.downloader

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobbyesp.spowlo.App
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.common.AsyncImageImpl
import com.bobbyesp.spowlo.ui.common.LocalWindowWidthState
import com.bobbyesp.spowlo.ui.components.ConsoleOutputComponent
import com.bobbyesp.spowlo.ui.components.MetadataResultRow
import com.bobbyesp.spowlo.ui.components.NavigationBarSpacer
import com.bobbyesp.spowlo.utils.CONFIGURE
import com.bobbyesp.spowlo.utils.DEBUG
import com.bobbyesp.spowlo.utils.GeneralTextUtils
import com.bobbyesp.spowlo.utils.PreferencesUtil
import com.bobbyesp.spowlo.utils.PreferencesUtil.getBoolean
import com.bobbyesp.spowlo.utils.SpotifyLinkPreview
import com.bobbyesp.spowlo.utils.ToastUtil
import com.bobbyesp.spowlo.utils.UrlValidator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@Composable
@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
)
fun DownloaderPage(
    navigateToSettings: () -> Unit = {},
    navigateToDownloaderSheet: () -> Unit = {},
    onOpenSpotifyItem: (String, String) -> Unit = { _, _ -> },
    navigateToSearch: (String) -> Unit = {},
    navigateToTasks: () -> Unit = {},
    navigateToLibrary: () -> Unit = {},
    sheetState: SheetState,
    downloaderViewModel: DownloaderViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val viewState by downloaderViewModel.viewStateFlow.collectAsStateWithLifecycle()
    val errorState by Downloader.errorState.collectAsStateWithLifecycle()
    val recentDownloads by downloaderViewModel.recentDownloads.collectAsStateWithLifecycle()
    val fetchingCount by Downloader.fetchingCount.collectAsStateWithLifecycle()
    val linkPreview by downloaderViewModel.linkPreview.collectAsStateWithLifecycle()
    val runningTasks = Downloader.mutableTaskList.values
        .filter { it.state is Downloader.DownloadTask.State.Running }
        .takeLast(3)
    val runningLinks = runningTasks.mapNotNull { UrlValidator.parseSpotifyLink(it.url) }
        .map { "${it.type}/${it.id}" }
        .toSet()

    val storagePermission = rememberPermissionState(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) { granted ->
        if (granted) downloaderViewModel.startDownloadSong()
        else ToastUtil.makeToast(R.string.permission_denied)
    }

    val startDownload: (String) -> Unit = { url ->
        if (downloaderViewModel.validateLinkOrShowError(url)) {
            when {
                Build.VERSION.SDK_INT > 29 || storagePermission.status == PermissionStatus.Granted -> {
                    if (CONFIGURE.getBoolean() || UrlValidator.classify(url) != UrlValidator.Type.SpotifyTrack) {
                        navigateToDownloaderSheet()
                    } else {
                        downloaderViewModel.startDownloadSong()
                    }
                }

                else -> storagePermission.launchPermissionRequest()
            }
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    var clipboardLink by remember { mutableStateOf<UrlValidator.SpotifyLink?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        clipboardLink = UrlValidator.parseSpotifyLink(clipboardManager.getText()?.text.orEmpty())
    }

    val submit: () -> Unit = {
        val url = viewState.url.trim()
        when {
            url.isBlank() -> ToastUtil.makeToast(R.string.url_empty)
            UrlValidator.parseSpotifyLink(url) == null -> navigateToSearch(url)
            else -> startDownload(url)
        }
        keyboardController?.hide()
    }

    val isCompactWidth = LocalWindowWidthState.current == WindowWidthSizeClass.Compact

    BackHandler(sheetState.isVisible) {
        downloaderViewModel.hideDialog(
            scope = scope,
            isDialog = !isCompactWidth,
            sheetState = sheetState,
        )
    }

    val pasteLink = clipboardLink?.takeIf {
        val linkUrl = "https://open.spotify.com/${it.type}/${it.id}"
        viewState.url.trim() != linkUrl && UrlValidator.parseSpotifyLink(viewState.url) == null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val taglinePrefix = stringResource(R.string.home_tagline)
                    val linkColor = MaterialTheme.colorScheme.primary
                    val tagline = buildAnnotatedString {
                        append(taglinePrefix)
                        append(" ")
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = SPOTDL_URL,
                                styles = TextLinkStyles(SpanStyle(color = linkColor)),
                                linkInteractionListener = { uriHandler.openUri(SPOTDL_URL) },
                            )
                        ) {
                            append("spotDL")
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = tagline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.show_more_actions),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = navigateToLibrary) {
                        Icon(
                            imageVector = Icons.Filled.LibraryMusic,
                            contentDescription = stringResource(R.string.your_library),
                        )
                    }
                    IconButton(onClick = navigateToTasks) {
                        Box {
                            Icon(
                                imageVector = Icons.Outlined.DownloadForOffline,
                                contentDescription = stringResource(R.string.downloads),
                            )
                            if (runningTasks.isNotEmpty() || fetchingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .size(7.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(Modifier.height(8.dp))

                    DownloaderInput(
                        value = viewState.url,
                        onValueChange = { downloaderViewModel.updateUrl(it) },
                        onSubmit = submit,
                        onOptions = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            navigateToDownloaderSheet()
                        },
                    )

                    if (pasteLink != null) {
                        PasteSuggestion(
                            onClick = {
                                downloaderViewModel.updateUrl(
                                    "https://open.spotify.com/${pasteLink.type}/${pasteLink.id}"
                                )
                                clipboardLink = null
                            },
                        )
                    }

                    val loadedPreview =
                        (linkPreview as? DownloaderViewModel.LinkPreviewState.Loaded)?.preview
                    val previewKey = UrlValidator.parseSpotifyLink(viewState.url)
                        ?.let { "${it.type}/${it.id}" }
                    AnimatedVisibility(
                        visible = loadedPreview != null && previewKey !in runningLinks
                    ) {
                        loadedPreview?.let { preview ->
                            LinkPreviewCard(
                                preview = preview,
                                onClick = {
                                    val link = UrlValidator.parseSpotifyLink(viewState.url)
                                    if (link != null) onOpenSpotifyItem(link.type, link.id)
                                },
                            )
                        }
                    }

                    AnimatedVisibility(visible = errorState.isErrorOccurred()) {
                        ErrorMessage(
                            modifier = Modifier.padding(top = 12.dp),
                            url = viewState.url,
                            errorState = errorState,
                        )
                    }

                    if (DEBUG.getBoolean()) {
                        val output = runningTasks.lastOrNull()?.currentLine.orEmpty()
                        AnimatedVisibility(visible = output.isNotEmpty()) {
                            ConsoleOutputComponent(
                                consoleOutput = output,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }

                    if (fetchingCount > 0) {
                        FetchingRow()
                    }

                    if (runningTasks.isNotEmpty()) {
                        HomeSectionHeader(title = stringResource(R.string.downloading))
                        runningTasks.forEach { task ->
                            DownloadingRow(
                                task = task,
                                onOpen = {
                                    val link = UrlValidator.parseSpotifyLink(task.url)
                                    if (link != null) onOpenSpotifyItem(link.type, link.id)
                                    else navigateToTasks()
                                },
                                onCancel = task::onCancel,
                            )
                        }
                    }

                    if (recentDownloads.isNotEmpty()) {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_recent),
                            action = stringResource(R.string.see_all),
                            onAction = navigateToTasks,
                        )
                        recentDownloads.forEach { song ->
                            RecentRow(
                                song = song,
                                onOpen = {
                                    val link = UrlValidator.parseSpotifyLink(song.songUrl)
                                    if (link != null) onOpenSpotifyItem(link.type, link.id)
                                    else navigateToTasks()
                                },
                            )
                        }
                    } else if (runningTasks.isEmpty() && fetchingCount == 0) {
                        Text(
                            text = stringResource(R.string.home_empty_hint),
                            modifier = Modifier.padding(top = 20.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }

            AnimatedVisibility(visible = !WindowInsets.isImeVisible) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CommunityButton(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.telegram_icon,
                            label = stringResource(R.string.telegram),
                            onClick = { uriHandler.openUri(TELEGRAM_URL) },
                        )
                        CommunityButton(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.github_mark,
                            label = stringResource(R.string.github),
                            onClick = { uriHandler.openUri(GITHUB_URL) },
                        )
                    }
                    NavigationBarSpacer()
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DownloaderInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOptions: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLink = UrlValidator.parseSpotifyLink(value) != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isLink) Icons.Outlined.Link else Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.Enter && event.key != Key.NumPadEnter) {
                            return@onPreviewKeyEvent false
                        }
                        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                            submit(onSubmit, keyboardController, focusManager)
                        }
                        true
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = if (isLink) ImeAction.Done else ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit(onSubmit, keyboardController, focusManager) },
                    onSearch = { submit(onSubmit, keyboardController, focusManager) },
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.home_input_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (value.isNotBlank()) {
                InputAction(
                    isLink = isLink,
                    onSubmit = { submit(onSubmit, keyboardController, focusManager) },
                    onOptions = onOptions,
                )
            }
        }
    }
}

@Composable
private fun InputAction(
    isLink: Boolean,
    onSubmit: () -> Unit,
    onOptions: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.secondaryContainer
    val content = MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onSubmit)
                    .padding(start = 12.dp, end = if (isLink) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isLink) Icons.Outlined.FileDownload else Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = content,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(if (isLink) R.string.download else R.string.searcher),
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                )
            }
            if (isLink) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(content.copy(alpha = 0.24f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(30.dp)
                        .clickable(onClick = onOptions),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.download_options),
                        modifier = Modifier.size(18.dp),
                        tint = content,
                    )
                }
            }
        }
    }
}

private fun submit(
    onSubmit: () -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
) {
    focusManager.clearFocus()
    keyboardController?.hide()
    onSubmit()
}

@Composable
private fun PasteSuggestion(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentPaste,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_paste_link),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LinkPreviewCard(
    preview: SpotifyLinkPreview,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImageImpl(
            modifier = Modifier
                .size(52.dp)
                .clip(MaterialTheme.shapes.small),
            model = preview.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            isPreview = false,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = preview.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = preview.subtitle.ifBlank { preview.type.replaceFirstChar { it.uppercase() } },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
            )
        }
        val trailing = when {
            preview.durationMs > 0 -> GeneralTextUtils.formatTrackDuration(preview.durationMs)
            preview.songCount > 0 -> pluralStringResource(
                R.plurals.song_count,
                preview.songCount,
                preview.songCount,
            )
            else -> ""
        }
        if (trailing.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FetchingRow() {
    Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.home_fetching),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DownloadingRow(
    task: Downloader.DownloadTask,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
) {
    val progress = (task.state as? Downloader.DownloadTask.State.Running)?.progress ?: 0f
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "downloadProgress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImageImpl(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            model = task.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            isPreview = false,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.taskName.ifBlank { stringResource(R.string.downloading) },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            if (task.artist.isNotBlank()) {
                Text(
                    text = task.artist,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            if (progress <= 0f) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = animated,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Outlined.Cancel,
                contentDescription = stringResource(R.string.cancel_download),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentRow(
    song: DownloadedSongInfo,
    onOpen: () -> Unit,
) {
    MetadataResultRow(
        name = song.songName,
        subtitle = song.songAuthor,
        artworkUrl = song.thumbnailUrl,
        trailing = {
            Text(
                text = GeneralTextUtils.formatRelativeTime(song.downloadedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        showDivider = true,
        onClick = onOpen,
    )
}

@Composable
private fun CommunityButton(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

private const val SPOTDL_URL = "https://github.com/spotdl/spotify-downloader"
private const val GITHUB_URL = "https://github.com/Eutalix/Spowlo"
private const val TELEGRAM_URL = "https://t.me/spowlo_chatroom"

@Composable
fun ErrorMessage(
    modifier: Modifier = Modifier,
    url: String,
    errorState: Downloader.ErrorState,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val message = errorState.message
        ?: stringResource(errorState.messageResId ?: R.string.unknown_error)
    val details = errorState.details

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
        if (!details.isNullOrEmpty()) {
            TextButton(
                onClick = {
                    clipboardManager.setText(
                        AnnotatedString(App.getVersionReport() + "\nURL: $url\n$details")
                    )
                    ToastUtil.makeToastSuspend(context.getString(R.string.error_copied))
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.error_copy))
            }
        }
    }
}
