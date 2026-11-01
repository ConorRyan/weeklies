package io.github.conorryan.weeklies.ui.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.ShoppingItem
import io.github.conorryan.weeklies.ui.CardDivider
import io.github.conorryan.weeklies.ui.CardGroup
import io.github.conorryan.weeklies.ui.Eyebrow
import io.github.conorryan.weeklies.ui.ScreenTitle
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.recipes.RecipePickerSheet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    graph: AppGraph,
    toastBus: ToastBus,
    onGoToRecipes: () -> Unit,
) {
    val vm: ShoppingViewModel = viewModel(
        factory = ShoppingViewModel.Factory(graph, toastBus),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var pickerOpen by remember { mutableStateOf(false) }
    var portionsFor by remember { mutableStateOf<Recipe?>(null) }
    var editTargetId by remember { mutableStateOf<String?>(null) }
    var editLabel by remember { mutableStateOf(TextFieldValue("")) }
    val commitEdit = {
        val target = editTargetId
        if (target != null) {
            val trimmed = editLabel.text.trim()
            if (trimmed.isNotEmpty()) vm.renameItem(target, trimmed)
            editTargetId = null
        }
    }
    val open = state.items.filterNot { it.checked }
    val done = state.items.filter { it.checked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState),
    ) {
        ScreenTitle(title = stringResource(R.string.shopping_title))
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = {
                    val id = state.todayRecipeId
                    val recipe = state.recipes.firstOrNull { it.id == id }
                    when {
                        id == null -> toastBus.info(
                            R.string.shopping_no_recipe_today,
                            message = R.string.shopping_no_recipe_today_msg,
                        )
                        recipe == null -> toastBus.info(
                            R.string.shopping_recipe_missing,
                            message = R.string.shopping_recipe_missing_msg,
                        )
                        else -> portionsFor = recipe
                    }
                },
                label = { Text(stringResource(R.string.shopping_add_todays_recipe), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.extraLarge,
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onPrimary,
                    leadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.heightIn(min = 40.dp),
            )
            AssistChip(
                onClick = { pickerOpen = true },
                label = { Text(stringResource(R.string.shopping_add_recipe), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CardGroup {
                open.forEachIndexed { index, item ->
                    if (index > 0) CardDivider()
                    OpenItemRow(
                        item = item,
                        editing = editTargetId == item.id,
                        editLabel = editLabel,
                        onEditLabel = { editLabel = it },
                        onTick = { vm.tick(item.id) },
                        onStartEdit = {
                            commitEdit()
                            editLabel = TextFieldValue(item.label, TextRange(item.label.length))
                            editTargetId = item.id
                        },
                        onCommitEdit = commitEdit,
                    )
                }
                if (open.isNotEmpty()) CardDivider()
                NewItemRow(
                    onAdd = vm::addItem,
                    onFocused = {
                        // Keep the input above the keyboard: scroll to the bottom now and again
                        // whenever the keyboard opening changes the scroll bounds.
                        snapshotFlow { scrollState.maxValue }
                            .collect { maxValue -> scrollState.animateScrollTo(maxValue) }
                    },
                )
            }

            if (done.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Eyebrow(
                            stringResource(R.string.shopping_in_basket, done.size),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = vm::removeChecked) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.shopping_clear_basket),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    CardGroup(color = MaterialTheme.colorScheme.surfaceContainerHighest, border = false) {
                        done.forEachIndexed { index, item ->
                            if (index > 0) CardDivider()
                            DoneItemRow(item, onTick = { vm.tick(item.id) })
                        }
                    }
                }
            }
        }
    }

    val portionsTarget = portionsFor
    if (portionsTarget != null) {
        PortionsDialog(
            recipe = portionsTarget,
            defaultPortions = state.defaultPortions,
            onAdd = { portions ->
                portionsFor = null
                vm.addRecipeIngredients(portionsTarget.id, portions)
            },
            onDismiss = { portionsFor = null },
        )
    }

    if (pickerOpen) {
        RecipePickerSheet(
            title = stringResource(R.string.shopping_add_recipe),
            recipes = state.recipes,
            onPick = { recipe ->
                pickerOpen = false
                portionsFor = recipe
            },
            onDismiss = { pickerOpen = false },
            emptyContent = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.shopping_add_recipes_first))
                    TextButton(onClick = { pickerOpen = false; onGoToRecipes() }) {
                        Text(stringResource(R.string.plan_go_to_recipes))
                    }
                }
            },
        )
    }
}

/** Square checkbox drawn to match the design: outlined when open, filled with a tick when done. */
@Composable
private fun TickBox(checked: Boolean) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun OpenItemRow(
    item: ShoppingItem,
    editing: Boolean,
    editLabel: TextFieldValue,
    onEditLabel: (TextFieldValue) -> Unit,
    onTick: () -> Unit,
    onStartEdit: () -> Unit,
    onCommitEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !editing, role = Role.Checkbox, onClick = onTick)
            .heightIn(min = 52.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TickBox(checked = false)
        if (editing) {
            val focusRequester = remember { FocusRequester() }
            var wasFocused by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            BasicTextField(
                value = editLabel,
                onValueChange = onEditLabel,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCommitEdit() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (wasFocused && !focusState.isFocused) onCommitEdit()
                        wasFocused = focusState.isFocused
                    },
            )
        } else {
            Text(
                item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
        IconButton(onClick = if (editing) onCommitEdit else onStartEdit) {
            Icon(
                if (editing) Icons.Filled.Check else Icons.Outlined.Edit,
                contentDescription = stringResource(if (editing) R.string.shopping_done_editing else R.string.shopping_edit_item),
                tint = if (editing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun DoneItemRow(item: ShoppingItem, onTick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onTick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TickBox(checked = true)
        Text(
            item.label,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NewItemRow(
    onAdd: (String) -> Unit,
    onFocused: suspend () -> Unit,
) {
    var newLabel by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) { if (focused) onFocused() }
    val submit = {
        val trimmed = newLabel.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(trimmed)
            newLabel = ""
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        BasicTextField(
            value = newLabel,
            onValueChange = { newLabel = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (newLabel.isEmpty()) {
                        Text(
                            stringResource(R.string.shopping_item_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
        )
        if (newLabel.isNotBlank()) {
            IconButton(onClick = { submit() }) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.shopping_add_item), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
