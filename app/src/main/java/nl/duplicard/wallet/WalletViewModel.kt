package nl.duplicard.wallet

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.duplicard.wallet.core.Card
import nl.duplicard.wallet.core.CardRules
import nl.duplicard.wallet.core.BarcodeType
import java.util.UUID

class WalletViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = CardRepository(application)
    var cards by mutableStateOf<List<Card>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var loadError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var pendingImport by mutableStateOf<List<Card>?>(null); private set
    var route by mutableStateOf(saved.get<String>("route") ?: "wallet"); private set
    var selectedId by mutableStateOf(saved.get<String>("selectedId")); private set
    var editingId by mutableStateOf(saved.get<String>("editingId")); private set
    var name by mutableStateOf(saved.get<String>("name") ?: ""); private set
    var number by mutableStateOf(saved.get<String>("number") ?: ""); private set
    var formatChoice by mutableStateOf(saved.get<String>("formatChoice") ?: "AUTO"); private set
    var color by mutableStateOf(saved.get<String>("color") ?: Card.DEFAULT_COLOR); private set

    init { reload() }
    fun reload() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                cards = withContext(Dispatchers.IO) { repository.load() }
                loaded = true; loadError = null
            } catch (_: Exception) {
                loaded = false
                loadError = "Your saved cards could not be read. They have not been overwritten. Retry, or keep the app installed while troubleshooting."
            } finally { busy = false; runPendingDocumentAction() }
        }
    }
    private fun navigate(value: String) { route = value; saved["route"] = value }
    fun updateName(value: String) { name = value.take(80); saved["name"] = name }
    fun updateNumber(value: String) { number = value.take(512); saved["number"] = number }
    fun updateFormat(value: String) { formatChoice = value; saved["formatChoice"] = value }
    fun updateColor(value: String) { color = value; saved["color"] = value }
    fun activeFormat(): BarcodeType = if (formatChoice == "AUTO") CardRules.detect(number) else BarcodeType.valueOf(formatChoice)
    fun clearMessage() { message = null }
    fun notify(text: String) { message = text }
    fun openCard(id: String) { selectedId = id; saved["selectedId"] = id; navigate("card") }
    fun edit(card: Card? = null) {
        editingId = card?.id; saved["editingId"] = editingId
        updateName(card?.name ?: ""); updateNumber(card?.number ?: "")
        updateFormat(card?.format?.name ?: "AUTO"); updateColor(card?.color ?: Card.DEFAULT_COLOR); navigate("editor")
    }
    fun scan() { navigate("scanner") }
    fun scanned(value: String, format: BarcodeType) { updateNumber(value); updateFormat(format.name); navigate("editor") }
    fun back() { if (!busy) navigate(if (route == "scanner") "editor" else "wallet") }
    fun saveCard() {
        if (busy || !loaded) return
        val card = try { Card(editingId ?: UUID.randomUUID().toString(), name, number, activeFormat(), color) }
        catch (e: IllegalArgumentException) { notify(e.message ?: "Invalid card."); return }
        if (cards.any { it.id != card.id && it.number == card.number && it.format == card.format }) {
            notify("This barcode is already in your wallet."); return
        }
        val next = if (editingId == null) cards + card else cards.map { if (it.id == card.id) card else it }
        if (next.size > CardRules.MAX_CARDS) { notify("Your wallet is full (2,000 cards)."); return }
        persist(next, "Card saved.") { navigate("wallet") }
    }
    fun delete(card: Card) { persist(cards.filterNot { it.id == card.id }, "Card deleted.") { navigate("wallet") } }
    fun move(id: String, offset: Int) {
        val from = cards.indexOfFirst { it.id == id }; val to = from + offset
        if (from >= 0 && to in cards.indices) persist(CardRules.move(cards, from, to))
    }
    fun reorder(ids: List<String>) {
        if (ids.size != cards.size || ids.toSet() != cards.map { it.id }.toSet()) return
        val byId = cards.associateBy { it.id }; val next = ids.map { byId.getValue(it) }
        if (next != cards) persist(next)
    }
    private fun persist(next: List<Card>, success: String? = null, after: () -> Unit = {}) {
        if (busy || !loaded) return
        busy = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.save(next) }
                cards = next; success?.let { notify(it) }; after()
            } catch (_: Exception) { notify("Could not save changes. Your previous cards are still stored. Please try again.") }
            finally { busy = false; runPendingDocumentAction() }
        }
    }
    fun readImport(uri: Uri) {
        saved["documentUri"] = uri.toString(); saved["documentAction"] = "import"
        runPendingDocumentAction()
    }
    // Restored activity results may arrive before asynchronous wallet loading finishes.
    private fun runPendingDocumentAction() {
        if (busy || !loaded) return
        val value = saved.get<String>("documentUri") ?: return
        val action = saved.get<String>("documentAction") ?: return
        saved.remove<String>("documentUri"); saved.remove<String>("documentAction")
        if (action == "import") performImportRead(Uri.parse(value)) else performExport(Uri.parse(value))
    }
    private fun performImportRead(uri: Uri) {
        busy = true
        viewModelScope.launch {
            try {
                pendingImport = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        BackupCodec.decode(BackupCodec.readBounded(it))
                    } ?: error("File could not be opened.")
                }
            } catch (e: Exception) { notify(e.message ?: "Could not read this backup.") }
            finally { busy = false; runPendingDocumentAction() }
        }
    }
    fun cancelImport() { pendingImport = null }
    fun confirmImport() {
        val incoming = pendingImport ?: return
        val merged = try { CardRules.merge(cards, incoming) }
        catch (e: IllegalArgumentException) { notify(e.message ?: "Import failed."); return }
        val added = merged.size - cards.size
        persist(merged, "Imported $added cards; skipped ${incoming.size - added} duplicates.") { pendingImport = null }
    }
    fun export(uri: Uri) {
        saved["documentUri"] = uri.toString(); saved["documentAction"] = "export"
        runPendingDocumentAction()
    }
    private fun performExport(uri: Uri) {
        busy = true; val snapshot = cards.toList()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bytes = BackupCodec.encode(snapshot)
                    val output = getApplication<Application>().contentResolver.openOutputStream(uri, "wt") ?: error("File could not be opened.")
                    output.use { it.write(bytes); it.flush() }
                }
                notify("Exported ${snapshot.size} cards.")
            } catch (_: Exception) { notify("Export failed. The selected file may be incomplete; please export again.") }
            finally { busy = false; runPendingDocumentAction() }
        }
    }
}
