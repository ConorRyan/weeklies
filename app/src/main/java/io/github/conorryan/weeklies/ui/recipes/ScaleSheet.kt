package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.quantity.formatScaled
import io.github.conorryan.weeklies.data.quantity.parseScaleFactor
import io.github.conorryan.weeklies.data.quantity.scaleIngredientLine
import io.github.conorryan.weeklies.ui.Eyebrow

/** A chosen scale: the factor applied to one-portion amounts and its label ("÷4", "×1.5"). */
data class ScaleChoice(val factor: Double, val label: String)

/**
 * Sheet for scaling a recipe's amounts: quick ÷6…×6 presets or a custom
 * multiply/divide value (decimals allowed, comma or point), with a live
 * preview. [onShow] only changes what the detail screen displays; [onSave]
 * rewrites the stored one-portion amounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleSheet(
    ingredients: List<String>,
    onShow: (ScaleChoice) -> Unit,
    onSave: (ScaleChoice) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var presetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var divide by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf("") }

    val preset = presetIndex?.let { SCALE_PRESETS[it] }
    val customFactor = parseScaleFactor(custom, divide)
    val choice: ScaleChoice? = when {
        preset != null -> ScaleChoice(preset.factor, preset.label)
        customFactor != null -> ScaleChoice(
            customFactor,
            scaleLabel(custom.trim().replace(',', '.').toDouble(), divide),
        )
        else -> null
    }
    val customInvalid = preset == null && custom.isNotBlank() && customFactor == null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.scale_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.scale_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onReset) { Text(stringResource(R.string.scale_reset)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(stringResource(R.string.scale_quick))
                for (row in SCALE_PRESETS.indices.chunked(3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in row) {
                            val selected = presetIndex == i
                            val item = SCALE_PRESETS[i]
                            if (selected) {
                                Button(
                                    onClick = { presetIndex = i },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                ) { Text(item.label, style = MaterialTheme.typography.titleLarge) }
                            } else {
                                OutlinedButton(
                                    onClick = { presetIndex = i },
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                ) {
                                    Text(item.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(stringResource(R.string.scale_custom))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow(Modifier.width(120.dp)) {
                        SegmentedButton(
                            selected = presetIndex == null && !divide,
                            onClick = { divide = false; presetIndex = null },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            icon = {},
                        ) { Text("×", style = MaterialTheme.typography.titleLarge) }
                        SegmentedButton(
                            selected = presetIndex == null && divide,
                            onClick = { divide = true; presetIndex = null },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            icon = {},
                        ) { Text("÷", style = MaterialTheme.typography.titleLarge) }
                    }
                    OutlinedTextField(
                        value = custom,
                        onValueChange = {
                            custom = it
                            presetIndex = null
                        },
                        placeholder = { Text(stringResource(R.string.scale_custom_hint)) },
                        label = { Text(stringResource(if (divide) R.string.scale_divide_by else R.string.scale_multiply_by)) },
                        isError = customInvalid,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    when {
                        customInvalid -> stringResource(R.string.scale_invalid)
                        choice != null -> stringResource(R.string.scale_becomes, formatScaled(choice.factor))
                        else -> stringResource(R.string.scale_decimals_ok)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (customInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (choice != null && ingredients.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                        ingredients.take(3).forEachIndexed { index, line ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    scaleIngredientLine(line, choice.factor),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { choice?.let(onShow) },
                    enabled = choice != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(
                        if (choice != null) {
                            stringResource(R.string.scale_show, choice.label)
                        } else {
                            stringResource(R.string.scale_enter_value)
                        },
                    )
                }
                OutlinedButton(
                    onClick = { choice?.let(onSave) },
                    enabled = choice != null,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.scale_save), modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    stringResource(R.string.scale_save_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Ingredient amount in bold accent, the rest normal; lines without a leading amount span the row. */
@Composable
fun IngredientLine(line: String, modifier: Modifier = Modifier) {
    val split = io.github.conorryan.weeklies.data.quantity.splitLeadingQuantity(line)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (split != null) {
            Text(
                split.first,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(84.dp),
            )
            Text(split.second, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        } else {
            Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
