package com.app.confiengg.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    val textFieldModifier = if (singleLine) {
        modifier.height(48.dp)
    } else {
        modifier.heightIn(min = 48.dp)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = textFieldModifier,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = remember { MutableInteractionSource() },
                placeholder = { Text(placeholder, fontSize = 14.sp) },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                contentPadding = if (singleLine) {
                    PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                } else {
                    PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                },
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = false,
                        interactionSource = remember { MutableInteractionSource() },
                        colors = colors,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            )
        }
    )
}
