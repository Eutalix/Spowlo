package com.bobbyesp.spowlo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bobbyesp.spowlo.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QueryTextBox(
    modifier: Modifier = Modifier,
    query: String,
    onValueChange: (String) -> Unit,
    onSearchCallback: () -> Unit,
    requestFocusOnStart: Boolean = true
) {
    val focusRequester = remember { FocusRequester() }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (!requestFocusOnStart) return@LaunchedEffect
        // Compose focuses the back button first with a hardware keyboard, so take focus once laid out.
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        8.dp
    )
    OutlinedTextField(
        value = query,
        onValueChange = onValueChange,
        placeholder = {
            if (query.isEmpty()) {
                Text(text = stringResource(id = R.string.searcher_page_query_text_box_label))
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                // Enter would otherwise move focus to the next control (the back button).
                if (event.key != Key.Enter && event.key != Key.NumPadEnter) {
                    return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                    softwareKeyboardController?.hide()
                    onSearchCallback()
                }
                true
            },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                softwareKeyboardController?.hide()
                onSearchCallback()
            }
        ),
        leadingIcon = {
            Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search icon")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear icon")
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@Composable
fun AdjacentLabel(modifier: Modifier = Modifier, text: String) {
    Text(
        text = text,
        modifier = modifier
            .padding(bottom = 12.dp, start = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}