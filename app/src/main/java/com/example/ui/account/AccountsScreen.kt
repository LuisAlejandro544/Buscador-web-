package com.example.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.UserAccountEntity
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla dedicada de Cuentas y Sincronización Web.
 * 
 * Permite al usuario vincular su cuenta de Google u otra cuenta personalizada
 * para sincronizar su perfil e iniciar sesión con un solo toque en sitios web
 * que soporten autenticación federada (Google One-Tap / FedCM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeAccount by viewModel.activeAccount.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<UserAccountEntity?>(null) }
    var isLoadingGoogleSignIn by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cuentas y Acceso Web",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("accounts_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Tarjeta de Cuenta Activa / Perfil Principal ---
            if (activeAccount != null) {
                ActiveAccountCard(
                    account = activeAccount!!,
                    onToggleAutoSignIn = { enabled ->
                        viewModel.toggleAutoSignInWeb(activeAccount!!.id, enabled)
                    }
                )
            } else {
                NoAccountCard(
                    onLinkClick = { showAddAccountDialog = true }
                )
            }

            // --- Botones de Acción para Vincular ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vincular nueva cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Conecta tu cuenta para autocompletar en páginas web que utilicen 'Continuar con Google' o login federado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón de Google con Credential Manager
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoadingGoogleSignIn = true
                                val result = viewModel.credentialManager.signInWithGoogle(activityContext = context)
                                isLoadingGoogleSignIn = false
                                if (result.isSuccess && !result.email.isNullOrBlank()) {
                                    viewModel.linkAccount(
                                        email = result.email,
                                        displayName = result.displayName ?: result.email,
                                        photoUrl = result.photoUrl,
                                        idToken = result.idToken,
                                        provider = "GOOGLE"
                                    )
                                    Toast.makeText(context, "Cuenta vinculada: ${result.email}", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Si el usuario canceló conscientemente el selector nativo, no forzamos el diálogo manual
                                    if (result.errorMessage != "Operación cancelada por el usuario") {
                                        Toast.makeText(
                                            context,
                                            result.errorMessage ?: "Introduce los datos de tu cuenta manualmente",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showAddAccountDialog = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("link_google_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isLoadingGoogleSignIn
                    ) {
                        if (isLoadingGoogleSignIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectando con Google...")
                        } else {
                            Icon(Icons.Default.VpnKey, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vincular con Google (Credential Manager)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón para agregar cuenta personalizada / manual
                    OutlinedButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_custom_account_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir cuenta manualmente / personalizada")
                    }
                }
            }

            // --- Lista de Todas las Cuentas Vinculadas ---
            if (allAccounts.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cuentas registradas (${allAccounts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        allAccounts.forEachIndexed { index, account ->
                            AccountListItem(
                                account = account,
                                isActive = account.isActive,
                                onSelectActive = { viewModel.switchActiveAccount(account.id) },
                                onDelete = { accountToDelete = account }
                            )
                            if (index < allAccounts.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            // --- Tarjeta de Privacidad y Aislamiento ---
            PrivacyNoticeCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Diálogo para agregar cuenta manual/Google ---
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { email, name, provider ->
                showAddAccountDialog = false
                viewModel.linkAccount(
                    email = email,
                    displayName = name,
                    provider = provider,
                    autoSignInWeb = true
                )
                Toast.makeText(context, "Cuenta $email registrada con éxito", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Diálogo de confirmación para eliminar cuenta ---
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Desvincular cuenta") },
            text = {
                Text("¿Estás seguro de que deseas desvincular la cuenta ${accountToDelete!!.email}? Ya no se utilizará para autocompletar inicio de sesión en páginas web.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToDelete?.let { viewModel.removeAccount(it.id) }
                        accountToDelete = null
                        Toast.makeText(context, "Cuenta desvinculada", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Desvincular", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta destacada para la cuenta activa en el navegador.
 */
@Composable
private fun ActiveAccountCard(
    account: UserAccountEntity,
    onToggleAutoSignIn: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar o inicial del usuario
                if (!account.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = account.photoUrl,
                        contentDescription = "Avatar de ${account.displayName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.displayName.firstOrNull()?.uppercase() ?: "G",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Activa",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Proveedor: ${account.provider}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // Switch de inicio de sesión automático en páginas web
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inicio de sesión automático en webs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Conecta con Google One-Tap o sugiere esta cuenta en formularios de inicio de sesión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = account.autoSignInWeb,
                    onCheckedChange = onToggleAutoSignIn,
                    modifier = Modifier.testTag("auto_signin_switch")
                )
            }
        }
    }
}

/**
 * Estado vacío cuando no hay ninguna cuenta vinculada.
 */
@Composable
private fun NoAccountCard(onLinkClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sin cuenta vinculada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vincula tu cuenta de Google o correo para poder iniciar sesión directamente en los sitios web con un solo toque.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Fila de cuenta individual en la lista de cuentas vinculadas.
 */
@Composable
private fun AccountListItem(
    account: UserAccountEntity,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        RadioButton(
            selected = isActive,
            onClick = onSelectActive,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Desvincular cuenta",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Tarjeta informativa sobre la privacidad de cuentas en pestañas protegidas e incógnito.
 */
@Composable
private fun PrivacyNoticeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF00897B), // Verde esmeralda de Pestañas Protegidas
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Aislamiento de Seguridad y Privacidad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• En Pestañas Protegidas, las cuentas se mantienen aisladas y no se autocompletan para asegurar que la sesión no contamine tu contenedor.\n• En Modo Incógnito, se suspende cualquier inicio de sesión automático.\n• Las credenciales se conservan de forma local y cifrada en tu teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Diálogo para introducir una cuenta de forma manual o configurada.
 */
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String, name: String, provider: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("GOOGLE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular cuenta al navegador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Introduce los datos de tu cuenta para guardarla en el navegador y usarla en tus accesos web:",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre para mostrar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        val displayName = if (name.isNotBlank()) name else email.substringBefore("@")
                        onConfirm(email.trim(), displayName.trim(), provider)
                    }
                },
                enabled = email.isNotBlank()
            ) {
                Text("Vincular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
