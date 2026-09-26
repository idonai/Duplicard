package nl.duplicard.wallet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.duplicard.wallet.core.Card
import nl.duplicard.wallet.core.CardRules
import nl.duplicard.wallet.core.BarcodeType
import kotlin.math.floor
import kotlin.math.min
import java.util.Locale

private fun storedColor(value:String)=Color(android.graphics.Color.parseColor(value))

@Composable fun BarcodePanel(value: String, format: BarcodeType, modifier: Modifier = Modifier) {
    val matrix = remember(value,format) { BarcodeEncoding.encode(value,format) }
    Surface(modifier, color = Color.White, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 24.dp)) {
            Canvas(Modifier.fillMaxWidth().then(if(format==BarcodeType.QR_CODE) Modifier.aspectRatio(1f) else Modifier.height(145.dp))
                .semantics { contentDescription = "${format.displayName}, $value" }) {
                val unit = floor(if(matrix.height==1) size.width/matrix.width else min(size.width/matrix.width,size.height/matrix.height)).coerceAtLeast(1f)
                val left = floor((size.width - matrix.width * unit) / 2f)
                val top = if(matrix.height==1) 0f else floor((size.height-matrix.height*unit)/2f)
                for(y in 0 until matrix.height) for (x in 0 until matrix.width) if (matrix[x,y])
                    drawRect(Color.Black,Offset(left+x*unit,top+y*unit),Size(unit,if(matrix.height==1) size.height else unit))
            }
            Spacer(Modifier.height(16.dp))
            Text(value, Modifier.fillMaxWidth(), color = Color.Black,
                style = if(format==BarcodeType.QR_CODE) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, maxLines=3)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditorScreen(vm: WalletViewModel, modifier: Modifier = Modifier) {
    val resolved=vm.activeFormat(); val clean=CardRules.normalizeValue(vm.number,resolved); val valid=CardRules.isValid(clean,resolved)
    var formatMenu by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf(false) }
    val palette=listOf("#DDE8D5","#C7E3F4","#D8D0F0","#F3CDD6","#F4D6B1","#F3E7A6","#BFE3D5","#D9D9D9")
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("A little less in your pocket.", style = MaterialTheme.typography.titleLarge)
        Text("Scan your loyalty card or enter its details below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = vm.name, onValueChange = vm::updateName, label = { Text("Card name") },
            placeholder = { Text("e.g. My supermarket") }, singleLine = true, enabled = !vm.busy,
            modifier = Modifier.fillMaxWidth(), supportingText = { Text("${vm.name.length}/80") })
        ExposedDropdownMenuBox(expanded=formatMenu,onExpandedChange={ if(!vm.busy) formatMenu=it }) {
            OutlinedTextField(value=if(vm.formatChoice=="AUTO") "Automatic (${resolved.displayName})" else resolved.displayName,
                onValueChange={},readOnly=true,label={ Text("Barcode format") },trailingIcon={ ExposedDropdownMenuDefaults.TrailingIcon(formatMenu) },
                modifier=Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable,enabled=!vm.busy).fillMaxWidth(),enabled=!vm.busy)
            ExposedDropdownMenu(expanded=formatMenu,onDismissRequest={ formatMenu=false }) {
                DropdownMenuItem(text={ Text("Automatic") },onClick={ vm.updateFormat("AUTO");formatMenu=false })
                BarcodeType.values().forEach { type -> DropdownMenuItem(text={ Text(type.displayName) },onClick={ vm.updateFormat(type.name);formatMenu=false }) }
            }
        }
        OutlinedTextField(value = vm.number, onValueChange = vm::updateNumber, label = { Text(if(resolved==BarcodeType.QR_CODE) "QR content" else "Card number or code") },
            singleLine = resolved!=BarcodeType.QR_CODE, maxLines=if(resolved==BarcodeType.QR_CODE) 4 else 1, enabled = !vm.busy, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = if(vm.formatChoice!="AUTO"&&resolved in listOf(BarcodeType.EAN_13,BarcodeType.EAN_8,BarcodeType.UPC_A,BarcodeType.UPC_E,BarcodeType.ITF)) KeyboardType.Number else KeyboardType.Text),
            isError = clean.isNotEmpty() && !valid,
            supportingText = { Text(if(clean.isNotEmpty()&&!valid) CardRules.validationMessage(resolved) else if(vm.formatChoice=="AUTO") "Detected as ${resolved.displayName}" else CardRules.validationMessage(resolved)) })
        OutlinedButton(onClick = vm::scan, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) { Text("Scan with camera") }
        Text("Card color",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            palette.forEach { hex ->
                val selected=vm.color.equals(hex,true)
                Box(Modifier.size(42.dp).background(storedColor(hex),CircleShape)
                    .border(if(selected) 3.dp else 1.dp,if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,CircleShape)
                    .clickable(enabled=!vm.busy){ vm.updateColor(hex) })
            }
            OutlinedButton(onClick={ customColor=true },enabled=!vm.busy,modifier=Modifier.height(42.dp)) { Text("Custom") }
        }
        if (valid) BarcodePanel(clean,resolved,Modifier.fillMaxWidth())
        Button(onClick = vm::saveCard, enabled = !vm.busy && vm.name.isNotBlank() && valid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save card") }
    }
    if(customColor) CustomColorDialog(vm.color,{ customColor=false },{ vm.updateColor(it);customColor=false })
}

@Composable private fun CustomColorDialog(initial:String,onDismiss:()->Unit,onApply:(String)->Unit) {
    val parsed=remember(initial) { android.graphics.Color.parseColor(initial) }
    var red by remember(initial) { mutableFloatStateOf(android.graphics.Color.red(parsed).toFloat()) }
    var green by remember(initial) { mutableFloatStateOf(android.graphics.Color.green(parsed).toFloat()) }
    var blue by remember(initial) { mutableFloatStateOf(android.graphics.Color.blue(parsed).toFloat()) }
    val hex=String.format(Locale.ROOT,"#%02X%02X%02X",red.toInt(),green.toInt(),blue.toInt())
    AlertDialog(onDismissRequest=onDismiss,title={ Text("Custom card color") },text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Surface(color=storedColor(hex),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().height(64.dp)) {}
            Text("Red ${red.toInt()}");Slider(red,{red=it},valueRange=0f..255f,steps=254)
            Text("Green ${green.toInt()}");Slider(green,{green=it},valueRange=0f..255f,steps=254)
            Text("Blue ${blue.toInt()}");Slider(blue,{blue=it},valueRange=0f..255f,steps=254)
            Text(hex,fontFamily=FontFamily.Monospace)
        }
    },confirmButton={ TextButton(onClick={onApply(hex)}){Text("Apply")} },dismissButton={ TextButton(onClick=onDismiss){Text("Cancel")} })
}
private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
@Composable fun CardScreen(card: Card, onEdit: () -> Unit, onDelete: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val window = LocalContext.current.activity()?.window
    DisposableEffect(window) {
        val oldBrightness = window?.attributes?.screenBrightness
        val wasKeptOn = (window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        window?.let { it.attributes = it.attributes.apply { screenBrightness = 1f }; it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        onDispose {
            window?.let {
                it.attributes = it.attributes.apply { screenBrightness = oldBrightness ?: -1f }
                if (!wasKeptOn) it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    val accent=storedColor(card.color)
    Column(modifier.background(accent.copy(alpha=0.24f)).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("READY TO SCAN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(card.name, style = MaterialTheme.typography.headlineLarge)
        BarcodePanel(card.number,card.format,Modifier.fillMaxWidth())
        Text("Show this barcode at checkout. Brightness is temporarily increased while this card is open.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onEdit, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Edit card") }
            TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1f)) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
}
