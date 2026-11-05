package com.bobbyesp.spowlo.ui.components.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SettingsItemNew(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    addTonalElevation: Boolean = true,
    clipCorners: Boolean = false,
    highlightIcon: Boolean = false
) {
    ListItem(
        modifier = Modifier
            .apply { if (clipCorners) this.clip(MaterialTheme.shapes.medium) }
            .then(modifier),
        leadingContent = {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        trailingContent = trailing,
        supportingContent = description,
        headlineContent = title,
        tonalElevation = if (addTonalElevation) 3.dp else 0.dp,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            leadingIconColor = if (highlightIcon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsItemNew(
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    addTonalElevation: Boolean = true,
    clipCorners: Boolean = false,
    highlightIcon: Boolean = false,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null
) {
    SettingsItemNew(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onClickLabel = onClickLabel,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                enabled = enabled
            )
            .alpha(if (enabled) 1f else 0.5f),
        icon = icon,
        description = description,
        title = title,
        trailing = trailing,
        addTonalElevation = addTonalElevation,
        clipCorners = clipCorners,
        highlightIcon = highlightIcon
    )
}

@Composable
fun SettingsSwitch(
    onCheckedChange: ((Boolean) -> Unit)?,
    checked: Boolean,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    thumbContent: (@Composable () -> Unit)? = null,
    addTonalElevation: Boolean = true,
    clipCorners: Boolean = false,
    highlightIcon: Boolean = false
) {
    val toggleableModifier = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange
            )
            .apply { if (!enabled) this.alpha(0.5f) }
    } else Modifier

    SettingsItemNew(
        modifier = modifier
            .then(toggleableModifier),
        icon = icon,
        description = description,
        title = title,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                thumbContent = thumbContent
            )
        },
        addTonalElevation = addTonalElevation,
        clipCorners = clipCorners,
        highlightIcon = highlightIcon
    )
}

@Composable
fun SettingsNewSingleChoiceItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    wantsTonalElevation: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                maxLines = 1,
                fontWeight = FontWeight.Bold
            )
        },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(contentPadding),
        tonalElevation = if (wantsTonalElevation) 3.dp else 0.dp,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

//settings switch with divider between the switch and the rest of the item. On click actions are independent of the switch
@Composable
fun SettingsSwitchWithDivider(
    onCheckedChange: ((Boolean) -> Unit)?,
    checked: Boolean,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    thumbContent: (@Composable () -> Unit)? = null,
    addTonalElevation: Boolean = true,
    clipCorners: Boolean = false,
    highlightIcon: Boolean = false,
    onClick: () -> Unit = {}
) {
    val toggleableModifier = if (onCheckedChange != null) {
        Modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange
            )
            .apply { if (!enabled) this.alpha(0.5f) }
    } else Modifier

    SettingsItemNew(
        modifier = modifier
            .then(toggleableModifier),
        icon = icon,
        description = description,
        title = title,
        onClick = { onClick() },
        trailing = {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Divider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 8.dp)
                        .width(1f.dp)
                        .align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    thumbContent = thumbContent
                )
            }
        },
        addTonalElevation = addTonalElevation,
        clipCorners = clipCorners,
        highlightIcon = highlightIcon
    )
}

@Composable
fun ElevatedSettingsCard(
    shape: Shape? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = shape ?: MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                3.dp
            )
        )
    ) {
        content()
    }
}

@Composable
fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier,
    indent: Dp = 56.dp,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = indent),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
fun SettingsChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}
