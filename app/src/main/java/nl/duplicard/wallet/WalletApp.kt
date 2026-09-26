package nl.duplicard.wallet

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WalletApp(vm: WalletViewModel) {
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var exportDialog by remember { mutableStateOf(false) }
    var discardDialog by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::readImport) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val card = vm.cards.firstOrNull { it.id == vm.selectedId }
    val enabled = vm.loaded && !vm.busy
    val goBack: () -> Unit = {
        val original = vm.cards.firstOrNull { it.id == vm.editingId }
        val dirty = vm.name != (original?.name ?: "") || vm.number != (original?.number ?: "") ||
            vm.formatChoice != (original?.format?.name ?: "AUTO") || vm.color != (original?.color ?: nl.duplicard.wallet.core.Card.DEFAULT_COLOR)
        if (vm.route == "editor" && dirty) discardDialog = true else vm.back()
    }
    BackHandler(enabled = vm.route != "wallet" || vm.busy) { if (!vm.busy) goBack() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text(when (vm.route) {
                "editor" -> if (vm.editingId == null) "Add card" else "Edit card"
                "scanner" -> "Scan card"
                "card" -> "Your card"
                else -> "Duplicard"
            }) }, navigationIcon = {
                if (vm.route != "wallet") TextButton(onClick = goBack, enabled = !vm.busy) { Text("Back") }
            }, actions = {
                if (vm.route == "wallet" || vm.route == "card") {
                    TextButton(onClick = { menu = true }, enabled = enabled) { Text("Options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (vm.route == "card" && card != null) {
                            val index = vm.cards.indexOf(card)
                            DropdownMenuItem(text = { Text("Move card up") }, enabled = index > 0, onClick = { menu = false; vm.move(card.id, -1) })
                            DropdownMenuItem(text = { Text("Move card down") }, enabled = index < vm.cards.lastIndex, onClick = { menu = false; vm.move(card.id, 1) })
                        } else {
                            DropdownMenuItem(text = { Text("Import backup") }, onClick = { menu = false; importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) })
                            DropdownMenuItem(text = { Text("Export backup") }, onClick = { menu = false; exportDialog = true })
                        }
                    }
                }
            })
        },
        floatingActionButton = {
            if (vm.route == "wallet" && vm.loaded) ExtendedFloatingActionButton(
                onClick = { if (enabled) vm.edit() }, text = { Text("Add new card") }, icon = { Text("+", style = MaterialTheme.typography.headlineSmall) })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                vm.loadError != null -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Could not open wallet", style = MaterialTheme.typography.headlineMedium); Text(vm.loadError!!)
                    Button(onClick = vm::reload, enabled = !vm.busy) { Text("Retry") }
                }
                !vm.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Opening your wallet…") }
                vm.route == "editor" -> EditorScreen(vm, Modifier.fillMaxSize())
                vm.route == "scanner" -> ScannerScreen(vm::scanned, Modifier.fillMaxSize())
                vm.route == "card" && card != null -> CardScreen(card, { vm.edit(card) }, { deleteDialog = true }, enabled, Modifier.fillMaxSize())
                else -> {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your everyday cards.", style = MaterialTheme.typography.headlineMedium)
                        Text(if (vm.cards.isEmpty()) "All together. Always with you." else "${vm.cards.size} saved · Hold and drag to reorder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (vm.cards.isEmpty()) Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("A lighter wallet starts here.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                            Text("No cards saved. Please add a new card", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }, enabled = enabled) { Text("Or import a Duplicard backup") }
                        }
                    } else WalletList(vm.cards, enabled, vm::openCard, vm::reorder, vm::move, Modifier.fillMaxSize())
                }
            }
        }
    }
    if (discardDialog) AlertDialog(onDismissRequest = { discardDialog = false }, title = { Text("Discard changes?") }, text = { Text("This card has unsaved changes.") },
        confirmButton = { TextButton(onClick = { discardDialog = false; vm.back() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discardDialog = false }) { Text("Keep editing") } })
    if (deleteDialog && card != null) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text("Delete ${card.name}?") },
        text = { Text("This removes the card from this phone. Export a backup first if you want to keep a copy.") },
        confirmButton = { TextButton(onClick = { deleteDialog = false; vm.delete(card) }, enabled = enabled) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } })
    if (exportDialog) AlertDialog(onDismissRequest = { exportDialog = false }, title = { Text("Export your wallet") },
        text = { Text("Save ${vm.cards.size} cards to a JSON backup. It includes card names and numbers as plain text. Keep it somewhere private.") },
        confirmButton = { TextButton(onClick = { exportDialog = false; exportLauncher.launch("Duplicard-${LocalDate.now()}.json") }, enabled = enabled) { Text("Choose file") } },
        dismissButton = { TextButton(onClick = { exportDialog = false }) { Text("Cancel") } })
    vm.pendingImport?.let { incoming ->
        val existing = vm.cards.map { it.format to it.number }.toSet()
        val count = incoming.map { it.format to it.number }.distinct().count { it !in existing }
        AlertDialog(onDismissRequest = { if (!vm.busy) vm.cancelImport() }, title = { Text("Import backup?") },
            text = { Text("Add $count cards and skip ${incoming.size - count} duplicates. Your existing cards and their order will be kept.") },
            confirmButton = { TextButton(onClick = vm::confirmImport, enabled = enabled) { Text("Import") } },
            dismissButton = { TextButton(onClick = vm::cancelImport, enabled = !vm.busy) { Text("Cancel") } })
    }
}
