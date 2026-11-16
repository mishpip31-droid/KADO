package com.kado.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.kado.app.barcode.BarcodeTools
import com.kado.app.data.BarcodeKind
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onDetected: (String, BarcodeKind) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
    } }
    DisposableEffect(lifecycleOwner) {
        val active = AtomicBoolean(true)
        val resultSent = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient()  // Bundled ML Kit model: works offline.
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            if (!active.get()) return@addListener
            try {
                provider = future.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val mediaImage = proxy.image
                    if (!active.get() || resultSent.get() || mediaImage == null) {
                        proxy.close()
                    } else {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { results ->
                                if (active.get() && !resultSent.get()) {
                                    val found = results.firstNotNullOfOrNull { b ->
                                        val kind = BarcodeTools.kindFromMlKit(b.format)
                                        val code = b.rawValue
                                        if (kind != null && !code.isNullOrBlank()) Pair(code, kind) else null
                                    }
                                    if (found != null && resultSent.compareAndSet(false, true)) {
                                        onDetected(found.first, found.second)
                                    }
                                }
                            }
                            .addOnCompleteListener { proxy.close() }
                    }
                }
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
                // Camera unavailable; the user can return and enter a code manually.
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active.set(false)
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack, modifier = Modifier.padding(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 295.dp, height = 210.dp)
                        .border(3.dp, Color.White, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(52.dp))
                }
            }
            Column(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.58f)).padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Наведите камеру на код карты", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Штрихкоды и QR-коды распознаются автоматически", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
