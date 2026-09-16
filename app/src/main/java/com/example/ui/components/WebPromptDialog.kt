package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.browser.engine.WebPromptRequest

/**
 * Componente Compose que renderiza de manera nativa los diálogos web interceptados por GeckoView.
 * Soporta alertas, confirmaciones, solicitud de texto, autenticación HTTP y selector de documentos.
 */
@Composable
fun WebPromptDialog(
    promptRequest: WebPromptRequest?,
    onDismissRequest: () -> Unit
) {
    if (promptRequest == null) return

    when (promptRequest) {
        is WebPromptRequest.Alert -> {
            AlertDialog(
                onDismissRequest = {
                    promptRequest.onConfirm()
                },
                icon = {
                    Icon(Icons.Default.Info, contentDescription = "Alerta web", tint = MaterialTheme.colorScheme.primary)
                },
                title = {
                    Text(text = promptRequest.title, style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Text(text = promptRequest.message, style = MaterialTheme.typography.bodyMedium)
                },
                confirmButton = {
                    Button(
                        onClick = { promptRequest.onConfirm() },
                        modifier = Modifier.testTag("dialog_alert_confirm_button")
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }

        is WebPromptRequest.Confirm -> {
            AlertDialog(
                onDismissRequest = {
                    promptRequest.onDismiss()
                },
                icon = {
                    Icon(Icons.Default.QuestionMark, contentDescription = "Confirmación web", tint = MaterialTheme.colorScheme.primary)
                },
                title = {
                    Text(text = promptRequest.title, style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Text(text = promptRequest.message, style = MaterialTheme.typography.bodyMedium)
                },
                confirmButton = {
                    Button(
                        onClick = { promptRequest.onConfirm() },
                        modifier = Modifier.testTag("dialog_confirm_positive_button")
                    ) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { promptRequest.onDismiss() },
                        modifier = Modifier.testTag("dialog_confirm_negative_button")
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        is WebPromptRequest.Prompt -> {
            var inputText by remember(promptRequest) { mutableStateOf(promptRequest.defaultValue) }

            AlertDialog(
                onDismissRequest = {
                    promptRequest.onDismiss()
                },
                icon = {
                    Icon(Icons.Default.Info, contentDescription = "Pregunta web", tint = MaterialTheme.colorScheme.primary)
                },
                title = {
                    Text(text = promptRequest.title, style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Column {
                        if (promptRequest.message.isNotBlank()) {
                            Text(text = promptRequest.message, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_prompt_text_field"),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { promptRequest.onConfirm(inputText) },
                        modifier = Modifier.testTag("dialog_prompt_confirm_button")
                    ) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { promptRequest.onDismiss() },
                        modifier = Modifier.testTag("dialog_prompt_cancel_button")
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        is WebPromptRequest.HttpAuth -> {
            var username by remember(promptRequest) { mutableStateOf("") }
            var password by remember(promptRequest) { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {
                    promptRequest.onDismiss()
                },
                icon = {
                    Icon(Icons.Default.Lock, contentDescription = "Autenticación web", tint = MaterialTheme.colorScheme.primary)
                },
                title = {
                    Text(text = "Autenticación requerida", style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Column {
                        Text(
                            text = "El sitio en ${promptRequest.host} solicita credenciales de acceso.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!promptRequest.realm.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mensaje: ${promptRequest.realm}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Usuario") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_auth_user_field"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_auth_pass_field"),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { promptRequest.onConfirm(username, password) },
                        modifier = Modifier.testTag("dialog_auth_confirm_button")
                    ) {
                        Text("Iniciar sesión")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { promptRequest.onDismiss() },
                        modifier = Modifier.testTag("dialog_auth_cancel_button")
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        is WebPromptRequest.FileChooser -> {
            val singleFileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    promptRequest.onFilesSelected(listOf(uri))
                } else {
                    promptRequest.onDismiss()
                }
            }

            val multipleFilesLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris: List<Uri> ->
                if (uris.isNotEmpty()) {
                    promptRequest.onFilesSelected(uris)
                } else {
                    promptRequest.onDismiss()
                }
            }

            AlertDialog(
                onDismissRequest = {
                    promptRequest.onDismiss()
                },
                icon = {
                    Icon(Icons.Default.Description, contentDescription = "Subir archivo", tint = MaterialTheme.colorScheme.primary)
                },
                title = {
                    Text(text = promptRequest.title, style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Text(
                        text = if (promptRequest.isMultiple) {
                            "La página solicita seleccionar uno o más archivos para subir."
                        } else {
                            "La página solicita seleccionar un archivo para subir."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val types = promptRequest.mimeTypes.ifEmpty { listOf("*/*") }.toTypedArray()
                            if (promptRequest.isMultiple) {
                                multipleFilesLauncher.launch(types)
                            } else {
                                singleFileLauncher.launch(types)
                            }
                        },
                        modifier = Modifier.testTag("dialog_file_choose_button")
                    ) {
                        Text("Seleccionar archivo")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { promptRequest.onDismiss() },
                        modifier = Modifier.testTag("dialog_file_cancel_button")
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
