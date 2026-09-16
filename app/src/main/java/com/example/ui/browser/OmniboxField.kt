package com.example.ui.browser

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Barra inteligente de direcciones y búsqueda (Omnibox).
 * Muestra el estado del protocolo seguro, modo protegido y atajos de borrado/búsqueda.
 */
@Composable
fun OmniboxField(
    value: String,
    isFocused: Boolean,
    isSecure: Boolean,
    isProtected: Boolean = false,
    isHome: Boolean,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emeraldColor = Color(0xFF00897B)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .testTag("omnibox_input"),
        placeholder = {
            Text(
                text = if (isHome) "Buscar o escribir URL..." else "Escribir búsqueda o dirección...",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            if (isProtected) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Pestaña Protegida (Aislamiento de cookies)",
                    tint = emeraldColor,
                    modifier = Modifier.size(16.dp)
                )
            } else if (isSecure && !isHome) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Conexión segura HTTPS",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Web",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        trailingIcon = {
            if (value.isNotEmpty() && isFocused) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar texto", modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onSearch(value) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}
