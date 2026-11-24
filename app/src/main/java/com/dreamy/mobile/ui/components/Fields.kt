package com.dreamy.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dreamy.mobile.ui.theme.Coral
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.ui.theme.NavyMuted
import com.dreamy.mobile.ui.theme.White
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cyan,
    unfocusedBorderColor = Light,
    errorBorderColor = Coral,
    focusedContainerColor = White,
    unfocusedContainerColor = White,
    disabledContainerColor = White,
    focusedTextColor = Navy,
    unfocusedTextColor = Navy,
    disabledTextColor = Navy,
    disabledBorderColor = Light,
    disabledTrailingIconColor = Navy,
    unfocusedPlaceholderColor = NavyMuted.copy(alpha = 0.6f),
    focusedPlaceholderColor = NavyMuted.copy(alpha = 0.6f),
)

/** `.label` above a `.field`, with an optional validation message below. */
@Composable
fun DreamyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    minLines: Int = 1,
    enabled: Boolean = true,
    password: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier.fillMaxWidth()) {
        label?.let { Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp)) }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = placeholder?.let { { Text(it) } },
            isError = error != null,
            singleLine = minLines == 1,
            minLines = minLines,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors(),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
    }
}

/** Read-only field that opens a picker when tapped. */
@Composable
private fun PickerField(
    value: String,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                placeholder = { Text(placeholder) },
                isError = error != null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors(),
            )
            // A disabled text field swallows no clicks, so an overlay handles the tap.
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(onClickLabel = label, onClick = onClick),
            )
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    display: (LocalDate) -> String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    PickerField(value?.let(display).orEmpty(), label, "jj/mm/aaaa", error, modifier) { open = true }
    if (!open) return
    // DatePicker works in UTC milliseconds.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                open = false
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Annuler") } },
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    PickerField(value?.toString().orEmpty(), label, "hh:mm", error, modifier) { open = true }
    if (!open) return
    val state = rememberTimePickerState(value?.hour ?: 10, value?.minute ?: 0, is24Hour = true)
    Dialog(onDismissRequest = { open = false }) {
        DreamyCard {
            TimePicker(state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { open = false }) { Text("Annuler") }
                TextButton(onClick = {
                    onValueChange(LocalTime.of(state.hour, state.minute))
                    open = false
                }) { Text("OK") }
            }
        }
    }
}

/** `<select>` equivalent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String?,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        label?.let { Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp)) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                isError = error != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = {
                        onSelect(value)
                        expanded = false
                    })
                }
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
    }
}

/** Oui / Non select used by the feedback form. */
@Composable
fun YesNoField(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    DropdownField(label, listOf(true to "Oui", false to "Non"), value, onValueChange)
}
