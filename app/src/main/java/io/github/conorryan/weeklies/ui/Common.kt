package io.github.conorryan.weeklies.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.ui.theme.EyebrowStyle
import io.github.conorryan.weeklies.ui.theme.LocalIsDarkTheme

/**
 * Full-screen page for non-tab routes: back arrow, title, optional trailing
 * actions, a scrollable body, and an optional bar pinned below it.
 */
@Composable
fun ModalScreen(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            actions()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
        if (bottomBar != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { bottomBar() }
        }
    }
}

/** Big title at the top of a tab screen, with an optional eyebrow above and action at the end. */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow != null) Eyebrow(eyebrow)
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        action?.invoke()
    }
}

/** Small uppercase label for sections and titles. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(text.uppercase(), style = EyebrowStyle, color = color, modifier = modifier)
}

/** Rounded, outlined white card that groups rows; rows are separated with [CardDivider]. */
@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(content = content)
    }
}

@Composable
fun CardDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
}

private val TileColorsLight = listOf(Color(0xFFF3DED3), Color(0xFFDCEBDF), Color(0xFFE4E3F1), Color(0xFFF2E8C9))
private val TileColorsDark = listOf(Color(0xFF4A3329), Color(0xFF244A33), Color(0xFF34305A), Color(0xFF4A4126))

/** Rounded square with the recipe's first letter, tinted by a stable hash of its name. */
@Composable
fun RecipeTile(name: String, modifier: Modifier = Modifier, size: Int = 44) {
    val colors = if (LocalIsDarkTheme.current) TileColorsDark else TileColorsLight
    val tint = colors[(name.hashCode() and Int.MAX_VALUE) % colors.size]
    Box(
        modifier = modifier
            .size(size.dp)
            .background(tint, RoundedCornerShape((size * 0.3f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Rounded search box with a leading magnifier and a clear button once text is entered. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Alert dialog with a title, an optional body, and a single "Close" button. */
@Composable
fun InfoDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = text,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

/**
 * Dialog with confirm and dismiss buttons. Pass [dismissLabel] = null to
 * show only the confirm button.
 */
@Composable
fun ConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissLabel: String? = stringResource(R.string.cancel),
    text: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = text,
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        },
        dismissButton = {
            if (dismissLabel != null) {
                TextButton(onClick = onDismiss) { Text(dismissLabel) }
            }
        },
    )
}

/**
 * The weekday's name in the app's language. [Weekday.label] is English-only
 * (it mirrors the React Native app's keys), so display names come from the
 * platform's own localized weekday names.
 */
@Composable
fun weekdayName(day: Weekday, short: Boolean = false): String {
    val locale = LocalConfiguration.current.locales[0] ?: java.util.Locale.getDefault()
    return java.time.DayOfWeek.of(day.ordinal + 1)
        .getDisplayName(if (short) java.time.format.TextStyle.SHORT else java.time.format.TextStyle.FULL, locale)
        .replaceFirstChar { it.titlecase(locale) }
}
