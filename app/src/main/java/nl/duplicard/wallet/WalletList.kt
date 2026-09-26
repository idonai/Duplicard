package nl.duplicard.wallet

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.delay
import nl.duplicard.wallet.core.Card as LoyaltyCard
import nl.duplicard.wallet.core.CardRules

private fun walletCardColor(value:String)=Color(android.graphics.Color.parseColor(value))
private fun walletContentColor(background:Color)=if(background.luminance()>0.45f) Color.Black else Color.White

@Composable fun WalletList(cards: List<LoyaltyCard>, enabled: Boolean, onOpen: (String) -> Unit,
    onReorder: (List<String>) -> Unit, onMove: (String, Int) -> Unit, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    var preview by remember { mutableStateOf(cards) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    val currentCards by rememberUpdatedState(cards)
    val currentEnabled by rememberUpdatedState(enabled)
    val commit by rememberUpdatedState(onReorder)
    fun moveAtPointer() {
        val id = draggingId ?: return
        val target = state.layoutInfo.visibleItemsInfo.firstOrNull { pointerY >= it.offset && pointerY < it.offset + it.size }?.key as? String ?: return
        val from = preview.indexOfFirst { it.id == id }; val to = preview.indexOfFirst { it.id == target }
        if (from >= 0 && to >= 0 && from != to) preview = CardRules.move(preview, from, to)
    }
    LaunchedEffect(draggingId) {
        while (draggingId != null) {
            val info = state.layoutInfo
            val delta = when {
                pointerY < info.viewportStartOffset + 100f -> -14f
                pointerY > info.viewportEndOffset - 100f -> 14f
                else -> 0f
            }
            if (delta != 0f) { state.scrollBy(delta); moveAtPointer() }
            delay(20)
        }
    }
    LazyColumn(state = state, modifier = modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                if (currentEnabled) {
                    pointerY = offset.y
                    draggingId = state.layoutInfo.visibleItemsInfo.firstOrNull { offset.y >= it.offset && offset.y < it.offset + it.size }?.key as? String
                }
            },
            onDrag = { change, amount -> if (draggingId != null) { change.consume(); pointerY += amount.y; moveAtPointer() } },
            onDragEnd = { if (draggingId != null) commit(preview.map { it.id }); draggingId = null },
            onDragCancel = { draggingId = null; preview = currentCards }
        )
    }, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(preview, key = { it.id }) { card ->
            val index = cards.indexOfFirst { it.id == card.id }
            val background=walletCardColor(card.color);val foreground=walletContentColor(background)
            Card(onClick = { if (draggingId == null) onOpen(card.id) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics {
                    customActions = buildList {
                        if (index > 0) add(CustomAccessibilityAction("Move up") { onMove(card.id, -1); true })
                        if (index < cards.lastIndex) add(CustomAccessibilityAction("Move down") { onMove(card.id, 1); true })
                    }
                }, colors = CardDefaults.cardColors(containerColor = background,contentColor=foreground)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = MaterialTheme.shapes.medium, color = foreground.copy(alpha=0.12f)) {
                        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            Text(card.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = foreground, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(card.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${card.format.displayName} · ${card.number}", style = MaterialTheme.typography.bodyMedium, color = foreground.copy(alpha=0.72f),maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = foreground)
                }
            }
        }
    }
    LaunchedEffect(enabled, cards) { if (enabled && draggingId == null) preview = cards }
}
