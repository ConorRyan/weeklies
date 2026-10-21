package io.github.conorryan.weeklies.ui.shopping

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.ui.BottomAction
import io.github.conorryan.weeklies.ui.BottomActionBar
import io.github.conorryan.weeklies.ui.TabHeader
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.theme.HeaderColor
import io.github.conorryan.weeklies.ui.theme.headerColorFor

@Composable
fun ShoppingListScreen(graph: AppGraph, toastBus: ToastBus) {
    val vm: ShoppingViewModel = viewModel(
        factory = ShoppingViewModel.Factory(graph, toastBus),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        TabHeader("Shopping List", Icons.Filled.ShoppingCart, headerColorFor(HeaderColor.SHOPPING))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(scrollState),
        ) {
            var editTargetId by remember { mutableStateOf<String?>(null) }
            var editLabel by remember { mutableStateOf("") }
            var editFieldFocused by remember { mutableStateOf(false) }
            val commitEdit = {
                val target = editTargetId
                if (target != null) {
                    val trimmed = editLabel.trim()
                    if (trimmed.isNotEmpty()) {
                        vm.renameItem(target, trimmed)
                    }
                    editTargetId = null
                }
            }
            for (item in state.items) {
                val editing = editTargetId == item.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !editing) { vm.tick(item.id) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = null,
                        modifier = Modifier.size(24.dp),
                    )
                    if (editing) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        BasicTextField(
                            value = editLabel,
                            onValueChange = { editLabel = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { commitEdit() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    val wasFocused = editFieldFocused
                                    editFieldFocused = focusState.isFocused
                                    if (wasFocused && !focusState.isFocused) commitEdit()
                                },
                        )
                    } else {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            color = if (item.checked) {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (editing) {
                                commitEdit()
                            } else {
                                editLabel = item.label
                                editFieldFocused = false
                                editTargetId = item.id
                            }
                        },
                    ) {
                        Icon(
                            if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                            contentDescription = if (editing) "Done editing" else "Edit item",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            var newLabel by remember { mutableStateOf("") }
            var fieldFocused by remember { mutableStateOf(false) }
            LaunchedEffect(fieldFocused) {
                if (fieldFocused) {
                    // Keep the input above the keyboard: scroll to the bottom now and again
                    // whenever the keyboard opening changes the scroll bounds.
                    snapshotFlow { scrollState.maxValue }
                        .collect { maxValue -> scrollState.animateScrollTo(maxValue) }
                }
            }
            val submitNewLabel = {
                val trimmed = newLabel.trim()
                if (trimmed.isNotEmpty()) {
                    vm.addItem(trimmed)
                    newLabel = ""
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitNewLabel() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .onFocusChanged { fieldFocused = it.isFocused }
                                .border(
                                    width = 1.dp,
                                    color = if (fieldFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (newLabel.isEmpty()) {
                                Text(
                                    "Item",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { submitNewLabel() },
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add item",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        BottomActionBar(
            listOf(
                BottomAction(
                    icon = Icons.Filled.AddShoppingCart,
                    label = "Add today's recipe",
                    onClick = { vm.addTodaysIngredients() },
                ),
                BottomAction(
                    icon = Icons.Filled.Delete,
                    label = "Remove checked",
                    onClick = { vm.removeChecked() },
                    enabled = state.hasChecked,
                    destructive = true,
                ),
            ),
        )
    }
}
