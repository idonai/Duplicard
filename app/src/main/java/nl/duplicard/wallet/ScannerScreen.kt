package nl.duplicard.wallet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import nl.duplicard.wallet.core.CardRules
import nl.duplicard.wallet.core.BarcodeType
import nl.duplicard.wallet.core.ScanConsensus
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable fun ScannerScreen(onScanned: (String, BarcodeType) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current; val lifecycleOwner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!allowed) {
        Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Scan your card", style = MaterialTheme.typography.headlineMedium)
            Text("Allow camera access to scan a barcode or QR code. You can also go back and enter it manually.")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app permissions") }
        }
        return
    }
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var verification by remember { mutableIntStateOf(0) }
    val latestOnScanned by rememberUpdatedState(onScanned)
    DisposableEffect(lifecycleOwner, previewView) {
        val active = AtomicBoolean(true); val delivered = AtomicBoolean(false); val consensus=ScanConsensus(3)
        val executor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE
        ).build())
        analysis.setAnalyzer(executor) { proxy ->
            val media = proxy.image
            if (!active.get() || delivered.get() || media == null) proxy.close()
            else {
                try {
                    scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener(executor) { codes ->
                            val result = codes.firstNotNullOfOrNull { code ->
                                val value=code.rawValue ?: return@firstNotNullOfOrNull null
                                val type=when(code.format) {
                                    Barcode.FORMAT_EAN_13 -> BarcodeType.EAN_13
                                    Barcode.FORMAT_EAN_8 -> BarcodeType.EAN_8
                                    Barcode.FORMAT_UPC_A -> BarcodeType.UPC_A
                                    Barcode.FORMAT_UPC_E -> BarcodeType.UPC_E
                                    Barcode.FORMAT_CODE_128 -> BarcodeType.CODE_128
                                    Barcode.FORMAT_CODE_39 -> BarcodeType.CODE_39
                                    Barcode.FORMAT_ITF -> BarcodeType.ITF
                                    Barcode.FORMAT_QR_CODE -> BarcodeType.QR_CODE
                                    else -> null
                                } ?: return@firstNotNullOfOrNull null
                                val clean=CardRules.normalizeValue(value,type)
                                if(CardRules.isValid(clean,type)) clean to type else null
                            }
                            if(!active.get()) return@addOnSuccessListener
                            if(result==null) { consensus.miss();verification=0 }
                            else {
                                verification=consensus.observe(result.second,result.first)
                                if(verification>=3&&delivered.compareAndSet(false,true)) latestOnScanned(result.first,result.second)
                            }
                        }
                        .addOnFailureListener(executor) { if (active.get()) error = "Could not read the camera image. Go back and reopen the scanner." }
                        .addOnCompleteListener(executor) { proxy.close() }
                } catch (_: Exception) { proxy.close(); if (active.get()) error = "Scanner unavailable. Please enter the number manually." }
            }
        }
        providerFuture.addListener({
            if (active.get()) {
                try {
                    provider = providerFuture.get()
                    camera = provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (_: Exception) { error = "The back camera could not start. Check permissions or enter the number manually." }
            }
        }, executor)
        onDispose {
            active.set(false); analysis.clearAnalyzer(); camera?.cameraControl?.enableTorch(false)
            provider?.unbind(preview, analysis); scanner.close(); camera = null
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().weight(1f))
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(error ?: if(verification>0) "Hold still — verifying $verification/3" else "Point at one barcode or QR code. Keep the complete code in view and hold still.")
            if (camera?.cameraInfo?.hasFlashUnit() == true) OutlinedButton(onClick = {
                torch = !torch; camera?.cameraControl?.enableTorch(torch)
            }) { Text(if (torch) "Turn torch off" else "Turn torch on") }
        }
    }
}
